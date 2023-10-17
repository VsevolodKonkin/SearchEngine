package searchengine.services.indexing;

import org.springframework.http.ResponseEntity;

public interface IndexingService {
    ResponseEntity startIndexing();
    ResponseEntity stopIndexing();
    ResponseEntity indexPage(String url);
}
