package searchengine.siteIndexing;

import lombok.extern.slf4j.Slf4j;
import org.jsoup.Connection;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import searchengine.lemma.LemmaFinder;
import searchengine.model.Index;
import searchengine.model.Lemma;
import searchengine.model.Page;
import searchengine.model.SiteModel;
import searchengine.model.enums.SiteStatus;
import searchengine.repository.IndexRepository;
import searchengine.repository.LemmaRepository;
import searchengine.repository.PageRepository;
import searchengine.repository.SiteRepository;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.concurrent.RecursiveAction;
import java.util.regex.Pattern;

import static java.lang.Thread.sleep;
@Slf4j
public class SiteMapRecursiveAction extends RecursiveAction {
    private final String userAgent = "Mozilla/5.0 (Windows; U; WindowsNT 5.1; en-US; rv1.8.1.6) " +
            "Gecko/20070725 Firefox/2.0.0.6";
    private final String referrer = "http://www.google.com";
    private String url;
    private final SiteMap siteMap;
    private SiteModel siteModel;
    private PageRepository pageRepository;
    private IndexRepository indexRepository;
    private LemmaRepository lemmaRepository;
    private SiteRepository siteRepository;

    public SiteMapRecursiveAction(String url, SiteMap siteMap, SiteModel siteModel,
                                  PageRepository pageRepository, IndexRepository indexRepository,
                                  LemmaRepository lemmaRepository, SiteRepository siteRepository) {
        this.url = url;
        this.siteMap = siteMap;
        this.siteModel = siteModel;
        this.pageRepository = pageRepository;
        this.indexRepository = indexRepository;
        this.lemmaRepository = lemmaRepository;
        this.siteRepository = siteRepository;
    }

    public SiteMapRecursiveAction(SiteMap siteMap) {
        this.siteMap = siteMap;
    }

    @Override
    protected void compute() {
        try {
            if (url != null) {
                sleep(150);
                Connection connection = Jsoup.connect(url).timeout(5000).
                        userAgent(userAgent).referrer(referrer).
                        ignoreHttpErrors(true).ignoreContentType(true);
                Document doc = connection.get();
                Elements elements = doc.select("body").select("a");
                for (Element a : elements) {
                    String childUrl = a.absUrl("href");
                    if (isCorrectUrl(childUrl)) {
                        childUrl = stripParams(childUrl);
                        siteMap.addChild(new SiteMap(childUrl));
                        if (Thread.currentThread().isInterrupted()) break;
                        runOneSiteIndexing(childUrl);
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
            log.error("An error occurred:", e);
        }

        List<SiteMapRecursiveAction> tasks = new ArrayList<>();
        for (SiteMap child : siteMap.getChildren()) {
            SiteMapRecursiveAction task = new SiteMapRecursiveAction(child);
            tasks.add(task);
            task.fork();
        }
        for (SiteMapRecursiveAction task : tasks) {
            task.join();
        }
    }

    private String stripParams(String url) {
        return url.replaceAll("\\?.+","");
    }

    private boolean isCorrectUrl(String url) {
        Pattern patternRoot = Pattern.compile("^" + siteMap.getUrl());
        Pattern patternNotFile = Pattern.compile("(\\S+(\\.(?i)(jpg|png|gif|bmp|pdf))$)");
        Pattern patternNotAnchor = Pattern.compile("#([\\w\\-]+)?$");

        return patternRoot.matcher(url).lookingAt()
                && !patternNotFile.matcher(url).find()
                && !patternNotAnchor.matcher(url).find();
    }

    private void runOneSiteIndexing(String childUrl) {
        siteModel.setStatusTime(new Date());
        siteModel.setStatus(SiteStatus.INDEXING);
        siteRepository.save(siteModel);
        Page page = new Page();
        String path = childUrl.replaceAll(url, "");
        try {
            Connection.Response conResponse = Jsoup.connect(childUrl).timeout(5000).
                    userAgent(userAgent).referrer(referrer).
                    ignoreHttpErrors(true).ignoreContentType(true).execute();
            page.setSite(siteModel);
            page.setPath(path);
            page.setCode(conResponse.statusCode());
            page.setContent(conResponse.body());
            Page pageCheck = pageRepository.getPage(path);
            if (pageCheck != null) {
                pageRepository.delete(pageCheck);
            }
            pageRepository.save(page);
            LemmaFinder lemmaFinder = LemmaFinder.getInstance();
            Map<String, Integer> lemmaMap = lemmaFinder.collectLemmas(conResponse.body());
            float lemmaSum = saveLemma(lemmaMap, siteModel);
            saveIndex(lemmaMap, page, lemmaSum);
        } catch (IOException e) {
            Thread.currentThread().interrupt();
            siteModel.setLastError("Ошибка чтения страницы: " + childUrl);
            log.error("An error occurred:", e);
        } finally {
            siteRepository.save(siteModel);
        }
        synchronized (this) {
            siteModel.setStatusTime(new Date());
            siteModel.setStatus(SiteStatus.INDEXED);
            siteRepository.save(siteModel);
        }
    }

    private void saveIndex(Map<String, Integer> lemmaMap, Page page, float lemmaSum) {
        for (Map.Entry<String, Integer> lemmaFromMap : lemmaMap.entrySet()) {
            Lemma lemma = lemmaRepository.findLemma(lemmaFromMap.getKey());
            Index index = indexRepository.findByLemmaId(lemma.getId());
            if (index == null) {
                index = new Index();
                index.setPage(page);
                index.setLemma(lemma);
                index.setRank(lemmaFromMap.getValue() / lemmaSum * 100);
                indexRepository.save(index);
            }
        }
    }

    private float saveLemma(Map<String, Integer> lemmaMap, SiteModel siteModel) {
        int lemmaCount = 0;
        for (Map.Entry<String, Integer> lemmaFromMap : lemmaMap.entrySet()) {
            lemmaCount += lemmaFromMap.getValue();
            Lemma lemma = lemmaRepository.findLemma(lemmaFromMap.getKey());
            if (lemma == null) {
                lemma = new Lemma();
                lemma.setSite(siteModel);
                lemma.setLemma(lemmaFromMap.getKey());
                lemma.setFrequency(1);
            } else {
                lemma.setFrequency(lemma.getFrequency() + 1);
            }
            lemmaRepository.save(lemma);
        }
        return lemmaCount;
    }
}
