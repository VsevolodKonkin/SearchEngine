package searchengine.dto.statistics;

import lombok.Data;
import searchengine.model.enums.SiteStatus;

import java.util.Date;

@Data
public class DetailedStatisticsItem {
    private String url;
    private String name;
    private SiteStatus status;
    private Date statusTime;
    private String error;
    private int pages;
    private int lemmas;
}
