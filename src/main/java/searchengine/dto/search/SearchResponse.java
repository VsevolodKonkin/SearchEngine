package searchengine.dto.search;

import lombok.Data;

@Data
public class SearchResponse {
    private boolean result;
    private int count;
    private SearchData data;
    private String error;
}
