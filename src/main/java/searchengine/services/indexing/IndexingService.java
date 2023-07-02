package searchengine.services.indexing;

import searchengine.dto.indexing.IndexingResponse;
import searchengine.dto.statistics.StatisticsResponse;

public interface IndexingService {
    IndexingResponse startIndexing();
    IndexingResponse stopIndexing();
}
