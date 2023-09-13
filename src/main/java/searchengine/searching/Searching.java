package searchengine.searching;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jsoup.Jsoup;
import org.springframework.stereotype.Component;
import searchengine.dto.search.DetailedSearchData;
import searchengine.dto.search.SearchData;
import searchengine.lemma.LemmaFinder;
import searchengine.model.Index;
import searchengine.model.Lemma;
import searchengine.model.Page;
import searchengine.model.SiteModel;
import searchengine.repository.IndexRepository;
import searchengine.repository.LemmaRepository;
import searchengine.repository.SiteRepository;

import java.io.IOException;
import java.util.*;
@Component
@RequiredArgsConstructor
@Slf4j
public class Searching {
    private final IndexRepository indexRepository;
    private final LemmaRepository lemmaRepository;
    private final SiteRepository siteRepository;
    private static final int MAX_PAGE_COUNT = 10;
    private static final int DEFAULT_LIMIT = 19;
    List<DetailedSearchData> detailedSearchData = new ArrayList<>();

    public SearchData getData(String query, String url, int offset, int limit) {
        SearchData searchData = new SearchData();
        List<DetailedSearchData> dataList = getDetailedSearchData(query, url);
        for (DetailedSearchData data : dataList) {
            detailedSearchData.add(data);
            detailedSearchData.sort(Comparator.comparing(DetailedSearchData::getRelevance));
            if (limit == 0) {
                detailedSearchData = detailedSearchData.subList(offset, Math.min(DEFAULT_LIMIT, detailedSearchData.size()));
            } else {
                detailedSearchData = detailedSearchData.subList(offset, Math.min(limit, detailedSearchData.size()));
            }
            detailedSearchData.removeIf(dataResult -> dataResult.getRelevance() == 0.0);
        }
        searchData.setListData(detailedSearchData);
        return searchData;
    }

    private List<DetailedSearchData> getDetailedSearchData(String query, String url) {
        List<DetailedSearchData> detSearchDataList = new ArrayList<>();
        List<SiteModel> listSites;
        if (url == null || url.isBlank()) {
            listSites = siteRepository.findAll();
        } else {
            SiteModel siteModel = siteRepository.findByUrl(url);
            listSites = new ArrayList<>(Collections.singletonList(siteModel));
        }
        for (SiteModel siteEntity : listSites) {
            DetailedSearchData detSearchData = new DetailedSearchData();
            detSearchData.setSite(siteEntity.getUrl());
            detSearchData.setSiteName(siteEntity.getName());
            detSearchData.setSnippet(getSnippet(query));
            detSearchData.setRelevance(getRelevance(query, siteEntity));
            detSearchDataList.add(detSearchData);
        }
        return detSearchDataList;
    }

    private double getRelevance(String query, SiteModel site) {
        double maxRelevance = 0.0;
        double totalRelevance = 0.0;
        List<Index> currentPages = new ArrayList<>();
        Set<String> filteredLemmas = getSetFilteredLemmas(query);
        List<String> sortedLemmas = new ArrayList<>(filteredLemmas);
        sortedLemmas.sort(Comparator.comparingInt(lemma -> {
            Lemma lemmaModel = lemmaRepository.findLemma(lemma);
            return lemmaModel != null ? lemmaModel.getFrequency() : Integer.MAX_VALUE;
        }));
        for (String lemma : sortedLemmas) {
            List<Index> indexEntries = indexRepository.findByLemmaAndSite(lemma, site.getId());
            if (indexEntries.size() > MAX_PAGE_COUNT) {
                continue;
            }
            if (currentPages.isEmpty()) {
                currentPages.addAll(indexEntries);
            } else {
                currentPages.removeIf(index -> !indexEntries.contains(index));
            }
            if (currentPages.isEmpty()) {
                break;
            }
            double rank = currentPages.stream().mapToDouble(Index::getRank).sum();
            totalRelevance += rank;
            maxRelevance += lemmaRepository.findLemma(lemma).getFrequency();
            }
        if (maxRelevance == 0.0) {
            return 0.0;
        }
        return totalRelevance / maxRelevance * 10000;
    }

    private String getSnippet(String query) {
        StringBuilder snippet = new StringBuilder();
        Set<String> filteredLemmas = getSetFilteredLemmas(query);
        for (String lemma : filteredLemmas) {
            Lemma lemmaModel = lemmaRepository.findLemma(lemma);
            if (lemmaModel != null) {
                Index indexLemma = indexRepository.findByLemmaId(lemmaModel.getId());
                if (indexLemma != null) {
                    Page page = indexLemma.getPage();
                    if (page != null) {
                        String text = Jsoup.parse(page.getContent()).text();
                        snippet.append(limitSnippetLength(text, query));
                    }
                }
            }
        }
        return snippet.toString().replaceAll(query, "<b>$0</b>");
    }

    private Set<String> getSetFilteredLemmas(String query) {
        LemmaFinder lemmaFinder = null;
        try {
            lemmaFinder = LemmaFinder.getInstance();
        } catch (IOException e) {
            e.printStackTrace();
            log.error("An error occurred:", e);
        }
        assert lemmaFinder != null;
        return lemmaFinder.getLemmaSet(query);
    }

    private String limitSnippetLength(String content, String query) {
        int maxSnippetLength = 300;
        String[] queryWords = query.split(" ");
        StringBuilder snippet = new StringBuilder();
        if (queryWords.length == 1) {
            int startIndex = content.toLowerCase().indexOf(query.toLowerCase());
            if (startIndex != -1) {
                int snippetStart = Math.max(0, startIndex - maxSnippetLength / 2);
                int snippetEnd = Math.min(content.length(), snippetStart + maxSnippetLength);
                snippet.append(content.substring(snippetStart, snippetEnd));
            }
        } else {
            for (String word : queryWords) {
                int startIndex = content.toLowerCase().indexOf(word.toLowerCase());
                if (startIndex == -1) {
                    continue;
                }
                int snippetStart = Math.max(0, startIndex - maxSnippetLength / 2);
                int snippetEnd = Math.min(content.length(), snippetStart + maxSnippetLength);
                snippet.append(content, snippetStart, startIndex);
                snippet.append("<b>");
                snippet.append(content, startIndex, snippetEnd);
                snippet.append("</b>");
            }
        }
        return snippet.toString();
    }
}
