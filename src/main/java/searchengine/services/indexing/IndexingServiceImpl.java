package searchengine.services.indexing;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import searchengine.LemmaFinder;
import searchengine.SiteResearcher;
import searchengine.config.Account;
import searchengine.dto.ErrorResponse;
import searchengine.dto.indexing.OkResponse;
import searchengine.model.*;
import searchengine.model.enums.SiteStatus;
import searchengine.repository.IndexRepository;
import searchengine.repository.LemmaRepository;
import searchengine.repository.PageRepository;
import searchengine.repository.SiteRepository;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.ForkJoinPool;



@Service
@RequiredArgsConstructor
@Slf4j
public class IndexingServiceImpl implements IndexingService {
    private final Account account;
    private final Map<SiteData, List<PageData>> sitesIndexing = Collections.synchronizedMap(new HashMap<>());
    private String pageIndexing = "";
    @Autowired
    @Getter
    private PageRepository pageRepository;
    @Autowired
    private SiteRepository siteRepository;
    @Autowired
    private LemmaRepository lemmaRepository;
    @Autowired
    private IndexRepository indexRepository;

    @Override
    public ResponseEntity startIndexing() {
        if (!sitesIndexing.isEmpty()) {
            return ResponseEntity.ok(new ErrorResponse("Предыдущая индексация еще не завершена"));
        }
        if (!pageIndexing.isBlank()) {
            return ResponseEntity.ok(new ErrorResponse("Индексация невозможна. Запущена индексация страницы"));
        }
        indexAllSites();
        return ResponseEntity.ok(new OkResponse());
    }

    private void indexAllSites() {
        for (SiteData site : siteRepository.findAll()) {
            new Thread(() -> {
                SiteData siteData = siteRepository.findFirstByName(site.getName());
                if (siteData != null) {
                    deleteSiteData(siteData);
                    siteData.setUrl(getSiteUrl(site));
                } else {
                    siteData = new SiteData(SiteStatus.INDEXING, new Date(),
                            "", getSiteUrl(site), site.getName());
                }
                siteRepository.save(siteData);
                List<PageData> pageDataList = new ArrayList<>(List.of(new PageData(siteData, "/", 0, "")));
                sitesIndexing.put(siteData, pageDataList);
                indexSitePages(pageDataList, siteData);
                sitesIndexing.remove(siteData);
            }).start();
        }
    }

    private void indexSitePages(List<PageData> pageDataList, SiteData siteData) {
        new ForkJoinPool().invoke(new SiteResearcher(pageDataList.get(0), pageDataList, this));
        if (siteData.getStatus() == SiteStatus.INDEXING) {
            insertAllData(pageDataList, siteData);
            siteData.setStatus(SiteStatus.INDEXED);
            siteRepository.save(siteData);
        }
    }

    @Override
    public ResponseEntity stopIndexing() {
        if (sitesIndexing.isEmpty()) {
            return ResponseEntity.ok(new ErrorResponse("Индексация не запущена"));
        }
        for (SiteData siteData : sitesIndexing.keySet()) {
            siteData.setStatus(SiteStatus.FAILED);
            siteData.setLastError("Индексация остановлена пользователем");
            siteRepository.save(siteData);
        }
        return ResponseEntity.ok(new OkResponse());
    }

    @Override
    public ResponseEntity indexPage(String url) {
        if (!sitesIndexing.isEmpty()) {
            return ResponseEntity.ok(new ErrorResponse("Индексация невозможна. Выполняется индексация сайтов."));
        }
        SiteData siteData = findSiteData(url);
        if (siteData == null) {
            return ResponseEntity.ok(new ErrorResponse("Данная страница находится за пределами сайтов, указанных в конфигурационном файле"));
        }
        Document doc;
        try {
            doc = getDocument(url);
        } catch (IOException e) {
            log.error("An error occurred:", e);
            return ResponseEntity.ok(new ErrorResponse("Страница не доступна"));
        }
        if (pageIndexing.equals(doc.location())) {
            return ResponseEntity.ok(new ErrorResponse("Эта страница уже индексируется"));
        }
        pageIndexing = doc.location();
        getPageData(siteData, doc);
        pageIndexing = "";
        return ResponseEntity.ok(new OkResponse());
    }

    private void getPageData(SiteData siteData, Document doc) {
        String relativeUrl = getRelativeUrl(doc.location(), siteData.getUrl());
        PageData pageData = pageRepository.findFirstByPathAndSite(relativeUrl, siteData);
        if (pageData != null) {
            deletePageData(pageData);
        }
        pageData = new PageData(siteData, relativeUrl,
                doc.connection().response().statusCode(), doc.html());
        insertAllData(new ArrayList<>(List.of(pageData)), siteData);
    }


    public SiteData findSiteData(String url) {
        List<SiteData> siteDataList = siteRepository.findAll();
        return siteDataList
                .stream()
                .filter(s -> !getRelativeUrl(url, s.getUrl()).isBlank())
                .findFirst()
                .orElse(null);
    }

    public void deleteSiteData(SiteData siteData) {
        sitesIndexing.put(siteData, null);
        siteData.setStatus(SiteStatus.INDEXING);
        siteData.setLastError("");
        siteRepository.save(siteData);
        while (pageRepository.countBySite(siteData) != 0 && siteData.getStatus() != SiteStatus.FAILED) {
            List<PageData> pageDataList = pageRepository.findFirst500BySite(siteData);
            pageRepository.deleteAll(pageDataList);
        }
        if (siteData.getStatus() != SiteStatus.FAILED) {
            lemmaRepository.deleteAll(lemmaRepository.findAllBySite(siteData));
        }
        siteData.setStatusTime(new Date());
    }

    public void deletePageData(PageData pageData) {
        List<LemmaData> lemmaDataListToUpdate = new ArrayList<>();
        List<LemmaData> lemmaDataListToDelete = new ArrayList<>();

        pageData.getLemmaDataList().forEach(lemmaData -> {
            if (lemmaData.getFrequency() > 1) {
                lemmaData.setFrequency(lemmaData.getFrequency() - 1);
                lemmaDataListToUpdate.add(lemmaData);
            } else {
                lemmaDataListToDelete.add(lemmaData);
            }
        });
        pageRepository.delete(pageData);
        lemmaRepository.deleteAll(lemmaDataListToDelete);
        lemmaRepository.saveAll(lemmaDataListToUpdate);
    }

    public void insertAllData(List<PageData> pageDataList, SiteData siteData) {
        List<LemmaData> lemmasToInsert = new ArrayList<>();
        List<IndexData> indexesToInsert = new ArrayList<>();
        List<LemmaData> lemmaDataList = lemmaRepository.findAllBySite(siteData);
        for (PageData pageData : pageDataList) {
            if (pageData.getCode() >= 400) {
                continue;
            }
            HashMap<String, Integer> lemmasMap = getLemmasMap(pageData.getContent());
            for (String lemma : lemmasMap.keySet()) {
                boolean isLemmaInListToInsert = true;
                LemmaData lemmaData = findLemma(lemmasToInsert, lemma);
                if (lemmaData == null) {
                    lemmaData = findLemma(lemmaDataList, lemma);
                    isLemmaInListToInsert = false;
                }
                if (lemmaData == null) {
                    lemmaData = new LemmaData(siteData, lemma, 1);
                } else {
                    lemmaData.setFrequency(lemmaData.getFrequency() + 1);
                }
                if (!isLemmaInListToInsert) {
                    lemmasToInsert.add(lemmaData);
                }
                indexesToInsert.add(new IndexData(pageData, lemmaData, lemmasMap.get(lemma)));
            }
        }
        saveData(pageDataList, lemmasToInsert, indexesToInsert, siteData);
    }

    private HashMap<String, Integer> getLemmasMap(String content) {
        HashMap<String, Integer> lemmasMap = null;
        try {
            lemmasMap = LemmaFinder.getInstance().collectLemmas(content);
        } catch (IOException e) {
            log.error("An error occurred:", e);
        }
        return lemmasMap;
    }

    private LemmaData findLemma(List<LemmaData> lemmaDataList, String lemma) {
        return lemmaDataList.stream()
                .filter(lemmaData -> lemmaData.getLemma().equals(lemma))
                .findFirst()
                .orElse(null);
    }

    private void saveData(List<PageData> pageDataList, List<LemmaData> lemmasToInsert, List<IndexData> indexesToInsert, SiteData siteData) {
        pageRepository.saveAll(pageDataList);
        lemmaRepository.saveAll(lemmasToInsert);
        indexRepository.saveAll(indexesToInsert);
        siteData.setStatusTime(new Date());
        siteRepository.save(siteData);
    }

    public String getRelativeUrl(String url, String siteUrl) {
        if (url == null || siteUrl == null) {
            return "";
        }
        String urlWoWWW = url.replaceFirst("//www.", "//");
        String siteUrlWoWWW = siteUrl.replaceFirst("//www.", "//");
        if (!urlWoWWW.startsWith(siteUrlWoWWW)) {
            return "";
        }
        String relativeUrl = urlWoWWW.substring(siteUrlWoWWW.length());
        if (!relativeUrl.startsWith("/")) {
            relativeUrl = "/".concat(relativeUrl);
        }
        return relativeUrl;
    }

    public String getSiteUrl(SiteData site) {
        try {
            Document doc = getDocument(site.getUrl());
            return doc.location().endsWith("/")
                    ? doc.location().substring(0, doc.location().length() - 1)
                    : doc.location();
        } catch (IOException e) {
            log.error("An error occurred:", e);
            return site.getUrl();
        }
    }

    public Document getDocument(String url) throws IOException {
        return Jsoup.connect(url)
                .userAgent(account.getUserAgent())
                .referrer(account.getReferrer())
                .get();
    }
}