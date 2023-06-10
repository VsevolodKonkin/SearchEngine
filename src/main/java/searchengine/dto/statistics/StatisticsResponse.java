package searchengine.dto.statistics;

import lombok.Data;
import org.springframework.beans.factory.annotation.Autowired;

@Data
public class StatisticsResponse {
    private boolean result;
    private StatisticsData statistics;
}
