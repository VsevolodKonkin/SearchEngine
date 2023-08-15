package searchengine.searching;

import lombok.RequiredArgsConstructor;
import org.jsoup.Connection;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import searchengine.dto.search.DetailedSearchData;
import searchengine.dto.search.SearchData;
import searchengine.lemma.LemmaFinder;
import searchengine.model.Index;
import searchengine.model.Lemma;
import searchengine.model.SiteModel;
import searchengine.repository.IndexRepository;
import searchengine.repository.LemmaRepository;
import searchengine.repository.PageRepository;
import searchengine.repository.SiteRepository;

import java.io.IOException;
import java.util.*;

@RequiredArgsConstructor
public class Searching {
    private final IndexRepository indexRepository;
    private final LemmaRepository lemmaRepository;
    private final PageRepository pageRepository;
    private final SiteRepository siteRepository;

    List<DetailedSearchData> detailedSearchData = new ArrayList<>();

    public SearchData getData(String query, String url, int offset, int limit) {
        SearchData searchData = new SearchData();
        List<SiteModel> listSites;
        if (url == null) {
            listSites = siteRepository.findAll();
        } else {
            SiteModel site = siteRepository.findByUrl(url);
            listSites = new ArrayList<>(Collections.singletonList(site));
        }
        for (SiteModel site : listSites) {
            DetailedSearchData data = getDetailedSearchData(query, site);
            detailedSearchData.add(data);
        }


        searchData.setListData(detailedSearchData);
        return searchData;
    }

    private DetailedSearchData getDetailedSearchData(String query, SiteModel site) {
        DetailedSearchData detSearchData = new DetailedSearchData();
        detSearchData.setSite(site.getUrl());
        detSearchData.setSiteName(site.getName());
        detSearchData.setSnippet(getSnippet(query, site.getUrl()));
        detSearchData.setRelevance(getRelevance(query, site));
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

            return totalRelevance / maxRelevance;

        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private String getSnippet(String query, String url) {
        try {
            String pageContent = getPageContent(url);
            String highlightedContent = highlightQueryMatches(pageContent, query);
            String snippet = limitSnippetLength(highlightedContent);
            return snippet;
        } catch (IOException e) {
            throw new RuntimeException("Failed to fetch webpage content " + e);
        }
    }

    private String getPageContent(String url) throws IOException {
        Connection connection = Jsoup.connect(url);
        Document document = connection.get();
        String content = document.text();
        return content;
    }

    private String highlightQueryMatches(String content, String query) {
        String highlightedContent = content.replaceAll(query, "<b>$0</b>");
        return highlightedContent;
    }

    private String limitSnippetLength(String content) {
        int maxSnippetLength = 300;
        String snippet = content.substring(0, Math.min(content.length(), maxSnippetLength));
        return snippet;
    }
}
