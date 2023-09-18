package searchengine.services.search;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import searchengine.dto.search.SearchResponse;
import searchengine.searching.Searching;
@Service
@RequiredArgsConstructor
public class SearchServiceImpl implements SearchService {

    private final Searching searching;

    @Override
    public SearchResponse getSearch(String query, String url, int offset, int limit) {
        SearchResponse searchResponse = new SearchResponse();
        if (query.isBlank()) {
            searchResponse.setResult(false);
            searchResponse.setError("Задан пустой поисковый запрос");
        } else if (url == null) {
            searchResponse = searchAllSites(query, offset, limit, searchResponse);
        } else {
            searchResponse = searchSite(query, url, offset, limit, searchResponse);
        }
        return searchResponse;
    }

    private SearchResponse searchSite(String query, String url, int offset, int limit, SearchResponse searchResponse) {
        if (searching.getData(query, url, offset, limit).getListData().isEmpty()) {
            searchResponse.setResult(false);
            searchResponse.setError("По вашему запросу ничего не найдено");
        } else {
            searchResponse.setResult(true);
            searchResponse.setData(searching.getData(query, url, offset, limit));
            searchResponse.setError("");
            searchResponse.setCount(searchResponse.getData().getListData().size());
        }
        return searchResponse;
    }


    private SearchResponse searchAllSites(String query, int offset, int limit, SearchResponse searchResponse) {
        if (searching.getData(query, null, offset, limit).getListData().isEmpty()) {
            searchResponse.setResult(false);
            searchResponse.setError("По вашему запросу ничего не найдено");
        } else {
            searchResponse.setResult(true);
            searchResponse.setData(searching.getData(query, null, offset, limit));
            searchResponse.setError("");
            searchResponse.setCount(searchResponse.getData().getListData().size());
        }
        return searchResponse;
    }
}
