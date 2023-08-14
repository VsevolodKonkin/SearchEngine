package searchengine.dto.search;

import lombok.Data;

@Data
public class DetailedSearchData {
    private String site;
    private String siteName;
    private String snippet;
    private double relevance;
}
