package searchengine.controllers;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import searchengine.dto.indexing.IndexingResponse;
import searchengine.dto.search.SearchResponse;
import searchengine.dto.statistics.StatisticsResponse;
import searchengine.services.indexing.IndexingService;
import searchengine.services.search.SearchService;
import searchengine.services.statistics.StatisticsService;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
@Slf4j
public class ApiController {

    private final StatisticsService statisticsService;
    private final IndexingService indexingService;
    private final SearchService searchService;

    @GetMapping("/statistics")
    public ResponseEntity<StatisticsResponse> statistics() {
        try {
            StatisticsResponse response = statisticsService.getStatistics();
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Error getting statistics", e);
            StatisticsResponse response = new StatisticsResponse();
            response.setResult(false);
            response.setError("Произошла внутренняя ошибка сервера: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    @GetMapping("/startIndexing")
    public ResponseEntity<IndexingResponse> startIndexing(){
        try {
            IndexingResponse indexingResponse = indexingService.startIndexing();
            if (indexingResponse.isResult()) {
                return ResponseEntity.ok(indexingResponse);
            } else {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(indexingResponse);
            }
        } catch (Exception e) {
            log.error("Error starting indexing", e);
            IndexingResponse indexingResponse = new IndexingResponse();
            indexingResponse.setResult(false);
            indexingResponse.setError("Произошла внутренняя ошибка сервера: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(indexingResponse);
        }
    }


    @GetMapping("/stopIndexing")
    public ResponseEntity<IndexingResponse> stopIndexing() {
        try {
            IndexingResponse indexingResponse = indexingService.stopIndexing();
            if (indexingResponse.isResult()) {
                return ResponseEntity.ok(indexingResponse);
            } else {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(indexingResponse);
            }
        } catch (Exception e) {
            log.error("Error stopping indexing", e);
            IndexingResponse indexingResponse = new IndexingResponse();
            indexingResponse.setResult(false);
            indexingResponse.setError("Произошла внутренняя ошибка сервера: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(indexingResponse);
        }
    }

    @PostMapping("/indexPage")
    public ResponseEntity<IndexingResponse> indexPage(@RequestParam String url) {
        try {
            IndexingResponse indexingResponse = indexingService.indexPage(url);
            if (indexingResponse.isResult()) {
                return ResponseEntity.ok(indexingResponse);
            } else {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(indexingResponse);
            }
        } catch (Exception e) {
            log.error("Error indexing page", e);
            IndexingResponse indexingResponse = new IndexingResponse();
            indexingResponse.setResult(false);
            indexingResponse.setError("Произошла внутренняя ошибка сервера: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(indexingResponse);
        }
    }

    @GetMapping("/search")
    public ResponseEntity<SearchResponse> search(@RequestParam String query,
                                                 @RequestParam(required = false) String url,
                                                 @RequestParam(required = false, defaultValue = "0") int offset,
                                                 @RequestParam(required = false, defaultValue = "20") int limit) {
        try {
            if (query.isBlank()) {
                SearchResponse searchResponse = new SearchResponse();
                searchResponse.setResult(false);
                searchResponse.setError("Задан пустой поисковый запрос");
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(searchResponse);
            }
            SearchResponse searchResponse = searchService.getSearch(query, url, offset, limit);
            if (searchResponse.isResult()) {
                return ResponseEntity.ok(searchResponse);
            } else {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(searchResponse);
            }
        } catch (Exception e) {
            log.error("Error executing search", e);
            SearchResponse searchResponse = new SearchResponse();
            searchResponse.setResult(false);
            searchResponse.setError("Произошла внутренняя ошибка сервера: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(searchResponse);
        }
    }
}
