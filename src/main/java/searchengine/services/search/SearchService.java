package searchengine.services.search;

import org.springframework.http.ResponseEntity;

public interface SearchService {
    ResponseEntity searchPages(String query, String url, int offset, int limit);
}
