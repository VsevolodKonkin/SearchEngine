package searchengine.services.search;

import searchengine.dto.search.SearchResponse;

public interface SearchService {
    SearchResponse getData(String query, String url, int offset, int limit);
}
