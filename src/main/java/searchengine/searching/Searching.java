package searchengine.searching;

import lombok.RequiredArgsConstructor;
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
public class Searching {
    private final IndexRepository indexRepository;
    private final LemmaRepository lemmaRepository;
    private final SiteRepository siteRepository;

    List<DetailedSearchData> detailedSearchData = new ArrayList<>();

    public SearchData getData(String query, String url, int offset, int limit) {
        SearchData searchData = new SearchData();
        DetailedSearchData data = getDetailedSearchData(query, url);
        detailedSearchData.add(data);
        detailedSearchData.sort(Comparator.comparing(DetailedSearchData::getRelevance));
        if (limit == 0) {
            int defaultLimit = 19;
            detailedSearchData = detailedSearchData.subList(offset, Math.min(defaultLimit, detailedSearchData.size()));
        } else {
            detailedSearchData = detailedSearchData.subList(offset, Math.min(limit, detailedSearchData.size()));
        }
        detailedSearchData.removeIf(dataResult -> dataResult.getRelevance() == 0.0);
        searchData.setListData(detailedSearchData);
        return searchData;
    }

    private DetailedSearchData getDetailedSearchData(String query, String url) {
        DetailedSearchData detSearchData = new DetailedSearchData();
        List<SiteModel> listSites;
        if (url == null || url.isBlank()) { // доделать
            listSites = siteRepository.findAll();
        } else {
            SiteModel siteModel = siteRepository.findByUrl(url);
            listSites = new ArrayList<>(Collections.singletonList(siteModel));
        }
        for (SiteModel siteEntity : listSites) {
            detSearchData.setSite(siteEntity.getUrl());
            detSearchData.setSiteName(siteEntity.getName());
            detSearchData.setSnippet(getSnippet(query));
            detSearchData.setRelevance(getRelevance(query, siteEntity));
        }

        return detSearchData;
    }

    private double getRelevance(String query, SiteModel site) {
        try {
            LemmaFinder lemmaFinder = LemmaFinder.getInstance();
            Set<String> filteredLemmas = lemmaFinder.getLemmaSet(query);
            List<String> sortedLemmas = new ArrayList<>(filteredLemmas);

            sortedLemmas.sort(Comparator.comparingInt(lemma -> {
                Lemma lemmaModel = lemmaRepository.findLemma(lemma);
                return lemmaModel != null ? lemmaModel.getFrequency() : Integer.MAX_VALUE;
            }));

            double maxRelevance = 0.0;
            double totalRelevance = 0.0;

            List<Index> currentPages = new ArrayList<>();

            int maxPageCount = 10;

            for (String lemma : sortedLemmas) {
                List<Index> indexEntries = indexRepository.findByLemmaAndSite(lemma, site.getId());
                if (indexEntries.size() > maxPageCount) {
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
            double relevance = totalRelevance / maxRelevance * 10000;
            return relevance;

        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private String getSnippet(String query) {
        try {
            LemmaFinder lemmaFinder = LemmaFinder.getInstance();
            Set<String> filteredLemmas = lemmaFinder.getLemmaSet(query);
            List<Lemma> lemmaList = new ArrayList<>();
            for (String lemma : filteredLemmas) {
                Lemma lemmaModel = lemmaRepository.findLemma(lemma);
                lemmaList.add(lemmaModel);
            }
            List<Index> indexList = new ArrayList<>();
            for (Lemma lemma : lemmaList) {
                if (lemma != null) {
                    Index indexLemma = indexRepository.findByLemmaId(lemma.getId());
                    indexList.add(indexLemma);
                }
            }
            List<Page> pageList = new ArrayList<>();
            for (Index id : indexList) {
                Page page = id.getPage();
                pageList.add(page);
            }
            StringBuilder snippet = new StringBuilder();
            for (Page page : pageList) {
                String text = Jsoup.parse(page.getContent()).text();
                snippet.append(limitSnippetLength(text));
            }
            return snippet.toString().replaceAll(query, "<b>$0</b>");
        } catch (IOException e) {
            throw new RuntimeException("Failed to fetch webpage content " + e);
        }
    }

    private String limitSnippetLength(String content) {
        int maxSnippetLength = 300;
        return content.substring(0, Math.min(content.length(), maxSnippetLength));
    }
}
