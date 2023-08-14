package searchengine.searching;

import lombok.RequiredArgsConstructor;
import searchengine.dto.search.DetailedSearchData;
import searchengine.dto.search.SearchData;
import searchengine.lemma.LemmaFinder;
import searchengine.model.SiteModel;
import searchengine.repository.IndexRepository;
import searchengine.repository.LemmaRepository;
import searchengine.repository.PageRepository;
import searchengine.repository.SiteRepository;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

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
        detSearchData.setSnippet(getSnippet(query));
        detSearchData.setRelevance(getRelevance(query, site));
        return detSearchData;
    }

    private double getRelevance(String query, SiteModel site) {
        try {
            LemmaFinder lemmaFinder = LemmaFinder.getInstance();
            Map<String, Integer> lemmaMap = lemmaFinder.collectLemmas(query);

        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        return 0.0;
    }

    private String getSnippet(String query) {
        return null;
    }
}
