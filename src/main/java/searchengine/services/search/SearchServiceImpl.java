package searchengine.services.search;

import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.jsoup.Jsoup;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import searchengine.dto.ErrorResponse;
import searchengine.dto.search.PageInfoItem;
import searchengine.dto.search.SearchResponse;
import searchengine.LemmaFinder;
import searchengine.model.LemmaData;
import searchengine.model.PageData;
import searchengine.model.SiteData;
import searchengine.model.enums.SiteStatus;
import searchengine.repository.IndexRepository;
import searchengine.repository.LemmaRepository;
import searchengine.repository.PageRepository;
import searchengine.repository.SiteRepository;
import searchengine.Snippet;

import java.io.IOException;
import java.util.*;

@Service
@Slf4j
public class SearchServiceImpl implements SearchService {
    private final PageRepository pageRepository;
    private final SiteRepository siteRepository;
    private final LemmaRepository lemmaRepository;
    private final IndexRepository indexRepository;
    private String currentQuery = "";
    private String prevQuery;
    private final List<PageInfoItem> pageInfoItems = new ArrayList<>();
    private Set<String> queryLemmas;
    private final static int SNIPPET_LENGTH = 250;

    @Autowired
    public SearchServiceImpl(PageRepository pageRepository, SiteRepository siteRepository,
                             LemmaRepository lemmaRepository, IndexRepository indexRepository) {
        this.pageRepository = pageRepository;
        this.siteRepository = siteRepository;
        this.lemmaRepository = lemmaRepository;
        this.indexRepository = indexRepository;
    }

    @Override
    public ResponseEntity searchPages(String query, String site, int offset, int limit) {
        LemmaFinder lemmaFinder;
        try {
            lemmaFinder = LemmaFinder.getInstance();
        } catch (IOException e) {
            log.error("An error occurred:", e);
            return ResponseEntity.ok(new ErrorResponse("Ошибка при создании экземпляра LemmaFinder"));
        }
        if (query.isBlank()) {
            return ResponseEntity.ok(new ErrorResponse("Задан пустой поисковый запрос"));
        }
        if (!currentQuery.isBlank()) {
            return ResponseEntity.ok(new ErrorResponse("Обрабатывается запрос \"" + currentQuery + "\""));
        }
        ArrayList<SiteData> siteDataList;
        if (site == null) {
            siteDataList = new ArrayList<>(siteRepository.findAll());
            if (siteDataList.isEmpty() || siteRepository.existsByStatus(SiteStatus.INDEXING)) {
                return ResponseEntity.ok(new ErrorResponse("Сайт(ы) не проиндексирован(ы)"));
            }
        } else {
            SiteData siteData = siteRepository.findFirstByUrl(site);
            if (siteData == null || siteData.getStatus() == SiteStatus.INDEXING) {
                return ResponseEntity.ok(new ErrorResponse("Сайт не проиндексирован"));
            }
            siteDataList = new ArrayList<>();
            siteDataList.add(siteData);
        }
        currentQuery = query;
        if (!currentQuery.equals(prevQuery)) {
            queryLemmas = lemmaFinder.collectLemmas(query).keySet();
            List<LemmaData> lemmaDataList = getLemmasFromData(queryLemmas, siteDataList);
            List<PageData> pageDataList = getPagesFromData(lemmaDataList, siteDataList);
            fillPagesInfo(lemmaDataList, pageDataList);
            prevQuery = currentQuery;
        }
        SearchResponse response = new SearchResponse();
        response.setCount(pageInfoItems.size());
        response.setData(getSubPageInfoList(offset, Math.min(pageInfoItems.size(), offset + limit)));
        response.setResult(true);
        currentQuery = "";
        return ResponseEntity.ok(response);
    }

    public List<LemmaData> getLemmasFromData(Set<String> queryLemmas, List<SiteData> siteDataList) {
        List<LemmaData> lemmaDataList = new ArrayList<>();
        for (String lemma : queryLemmas) {
            for (SiteData siteData : siteDataList) {
                if (siteData != null) {
                    LemmaData lemmaDataEntity = lemmaRepository.findFirstByLemmaAndSite(lemma, siteData);
                    if (lemmaDataEntity != null) {
                        lemmaDataList.add(lemmaDataEntity);
                        break;
                    }
                }
            }
        }
        lemmaDataList.sort(Comparator.comparingInt(LemmaData::getFrequency));
        return lemmaDataList;
    }

    public List<PageData> getPagesFromData(List<LemmaData> lemmaDataList, List<SiteData> siteDataList) {
        if (lemmaDataList.isEmpty()) {
            return new ArrayList<>();
        }
        List<PageData> pageDataList = new ArrayList<>();
        for (LemmaData lemmaData : lemmaDataList) {
            String lemma = lemmaData.getLemma();
            for (SiteData siteData : siteDataList) {
                if (siteData != null) {
                    List<PageData> pages = pageRepository.findAllByLemmaAndSite(lemma, siteData, PageRequest.of(0, 500));
                    pageDataList.addAll(pages);
                }
            }
        }
        List<PageData> filteredPageDataList = new ArrayList<>();
        for (PageData pageData : pageDataList) {
            boolean existsInAllLemmas = true;
            for (LemmaData lemmaData : lemmaDataList) {
                if (!indexRepository.existsByLemma_LemmaAndPage(lemmaData.getLemma(), pageData)) {
                    existsInAllLemmas = false;
                    break;
                }
            }
            if (existsInAllLemmas) {
                filteredPageDataList.add(pageData);
            }
        }
        return filteredPageDataList;
    }

    public void fillPagesInfo(List<LemmaData> lemmaDataDataList, List<PageData> pageDataDataList) {
        pageInfoItems.clear();
        for (PageData pageData : pageDataDataList) {
            float absRelevance = 0;
            for (LemmaData lemmaData : lemmaDataDataList) {
                absRelevance += indexRepository.findFirstByLemma_LemmaAndPage(lemmaData.getLemma(), pageData).getRank();
            }
            PageInfoItem pageInfoItem = new PageInfoItem();
            pageInfoItem.setPageData(pageData);
            pageInfoItem.setRelevance(absRelevance);
            pageInfoItems.add(pageInfoItem);
        }
        if (!pageInfoItems.isEmpty()) {
            pageInfoItems.sort(Comparator.comparing(PageInfoItem::getRelevance).reversed());
            float maxAbsRelevance = (float) pageInfoItems.get(0).getRelevance();
            pageInfoItems.forEach(p -> p.setRelevance(p.getRelevance() / maxAbsRelevance));
        }
    }

    public List<PageInfoItem> getSubPageInfoList(int fromIndex, int toIndex){
        List<PageInfoItem> subPageInfoList = pageInfoItems.subList(fromIndex, toIndex);
        for (PageInfoItem pageInfoItem: subPageInfoList){
            PageData pageData = pageInfoItem.getPageData();
            pageInfoItem.setSite(pageData.getSite().getUrl());
            pageInfoItem.setSiteName(pageData.getSite().getName());
            pageInfoItem.setUri(pageData.getPath());
            pageInfoItem.setTitle(Jsoup.parse(pageData.getContent()).title());
            pageInfoItem.setSnippet(getSnippetText(pageData, queryLemmas));
        }
        return subPageInfoList;
    }

    @SneakyThrows
    public String getSnippetText(PageData pageData, Set<String> queryLemmas) {
        LemmaFinder lemmaFinder = LemmaFinder.getInstance();
        String text = pageData.getContent();
        List<Snippet> snippetList = lemmaFinder.getSnippetList(text, queryLemmas);
        snippetList.sort(Comparator.comparingInt(f -> f.getQueryWordsIndexes().size()));
        String snippetText = "";
        int snippetIndex = snippetList.size() - 1;
        while (snippetIndex >= 0 && snippetText.length() < SNIPPET_LENGTH) {
            snippetText = snippetText
                    .concat(snippetList.get(snippetIndex)
                            .getFormattedText(SNIPPET_LENGTH - snippetText.length()))
                    .concat("|");
            snippetIndex--;
        }
        return snippetText;
    }
}
