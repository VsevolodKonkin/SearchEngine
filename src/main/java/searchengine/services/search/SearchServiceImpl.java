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
import java.util.stream.Collectors;

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
            LemmaFinder lemmaFinder = createLemmaFinder();
            if (query.isBlank()) {
                return ResponseEntity.ok(new ErrorResponse("Задан пустой поисковый запрос"));
            }
            if (!currentQuery.isBlank()) {
                return ResponseEntity.ok(new ErrorResponse("Обрабатывается запрос \"" + currentQuery + "\""));
            }
            ArrayList<SiteData> siteDataList = getSiteDataList(site);
            currentQuery = query;
            if (!currentQuery.equals(prevQuery)) {
                queryLemmas = getQueryLemmas(lemmaFinder, query, siteDataList).keySet();
                List<LemmaData> lemmaDataList = getLemmasFromData(queryLemmas, siteDataList);
                List<PageData> pageDataList = getPagesFromData(lemmaDataList, siteDataList);
                fillPagesInfo(lemmaDataList, pageDataList);
                prevQuery = currentQuery;
            }
            SearchResponse response = createSearchResponse(offset, limit);
            currentQuery = "";
            return ResponseEntity.ok(response);
    }

    private LemmaFinder createLemmaFinder() {
        try {
            return LemmaFinder.getInstance();
        } catch (IOException e) {
            log.error("An error occurred:", e);
            return null;
        }
    }

    private ArrayList<SiteData> getSiteDataList(String site) {
        if (site == null) {
            ArrayList<SiteData> siteDataList = new ArrayList<>(siteRepository.findAll());
            if (siteDataList.isEmpty() || siteRepository.existsByStatus(SiteStatus.INDEXING)) {
                throw new RuntimeException("Сайт(ы) не проиндексирован(ы)");
            }
            return siteDataList;
        } else {
            SiteData siteData = siteRepository.findFirstByUrl(site);
            if (siteData == null || siteData.getStatus() == SiteStatus.INDEXING) {
                throw new RuntimeException("Сайт не проиндексирован");
            }
            ArrayList<SiteData> siteDataList = new ArrayList<>();
            siteDataList.add(siteData);
            return siteDataList;
        }
    }

    private Map<String, Integer> getQueryLemmas(LemmaFinder lemmaFinder, String query, List<SiteData> siteDataList) {
        return lemmaFinder.getLemmas(query);
    }

    private SearchResponse createSearchResponse(int offset, int limit) {
        SearchResponse response = new SearchResponse();
        response.setCount(pageInfoItems.size());
        response.setData(fillSubPageInfoList(offset, Math.min(pageInfoItems.size(), offset + limit)));
        response.setResult(true);
        return response;
    }

    public List<LemmaData> getLemmasFromData(Set<String> queryLemmas, List<SiteData> siteDataList) {
        return queryLemmas.stream()
                .map(lemma -> siteDataList.stream()
                        .filter(Objects::nonNull)
                        .map(siteData -> lemmaRepository.findFirstByLemmaAndSite(lemma, siteData))
                        .filter(Objects::nonNull)
                        .findFirst()
                        .orElse(null)
                )
                .filter(Objects::nonNull)
                .sorted(Comparator.comparingInt(LemmaData::getFrequency))
                .collect(Collectors.toList());
    }

    public List<PageData> getPagesFromData(List<LemmaData> lemmaDataList, List<SiteData> siteDataList) {
        if (lemmaDataList.isEmpty()) {
            return new ArrayList<>();
        }
        List<PageData> pageDataList = getPagesForLemmas(lemmaDataList, siteDataList);
        return getFilteredPagesByLemmaData(lemmaDataList, pageDataList);
    }

    private List<PageData> getPagesForLemmas(List<LemmaData> lemmaDataList, List<SiteData> siteDataList) {
        List<PageData> pageDataList = new ArrayList<>();
        for (LemmaData lemmaData : lemmaDataList) {
            String lemma = lemmaData.getLemma();
            siteDataList.stream()
                    .filter(Objects::nonNull)
                    .map(siteData -> pageRepository.findAllByLemmaAndSite(lemma, siteData, PageRequest.of(0, 500)))
                    .forEach(pageDataList::addAll);
        }
        return pageDataList;
    }


    private List<PageData> getFilteredPagesByLemmaData(List<LemmaData> lemmaDataList, List<PageData> pageDataList) {
        List<PageData> filteredPageDataList = new ArrayList<>();
        for (PageData pageData : pageDataList) {
            boolean existsInAllLemmas = existsInAllLemmas(lemmaDataList, pageData);
            if (existsInAllLemmas) {
                filteredPageDataList.add(pageData);
            }
        }
        return filteredPageDataList;
    }

    private boolean existsInAllLemmas(List<LemmaData> lemmaDataList, PageData pageData) {
        for (LemmaData lemmaData : lemmaDataList) {
            if (!indexRepository.existsByLemma_LemmaAndPage(lemmaData.getLemma(), pageData)) {
                return false;
            }
        }
        return true;
    }


    public void fillPagesInfo(List<LemmaData> lemmaDataDataList, List<PageData> pageDataDataList) {
        Set<PageInfoItem> uniquePageInfoItems = new HashSet<>();
        for (PageData pageData : pageDataDataList) {
            float absRelevance = 0;
            for (LemmaData lemmaData : lemmaDataDataList) {
                absRelevance += indexRepository.findFirstByLemma_LemmaAndPage(lemmaData.getLemma(), pageData).getRank();
            }
            PageInfoItem pageInfoItem = new PageInfoItem();
            pageInfoItem.setPageData(pageData);
            pageInfoItem.setRelevance(absRelevance);
            uniquePageInfoItems.add(pageInfoItem);
        }
        pageInfoItems.clear();
        pageInfoItems.addAll(uniquePageInfoItems);
        if (!pageInfoItems.isEmpty()) {
            pageInfoItems.sort(Comparator.comparing(PageInfoItem::getRelevance).reversed());
            float maxAbsRelevance = (float) pageInfoItems.get(0).getRelevance();
            pageInfoItems.forEach(p -> p.setRelevance(p.getRelevance() / maxAbsRelevance));
        }
    }

    public List<PageInfoItem> fillSubPageInfoList(int fromIndex, int toIndex){
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
