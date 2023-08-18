import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.boot.test.context.SpringBootTest;
import searchengine.Application;
import searchengine.config.Site;
import searchengine.config.SitesList;
import searchengine.dto.statistics.StatisticsResponse;
import searchengine.repository.LemmaRepository;
import searchengine.repository.PageRepository;
import searchengine.services.statistics.StatisticsServiceImpl;
import java.util.ArrayList;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@SpringBootTest(classes = Application.class)
public class StatisticsServiceImplTest {
    @Mock
    private SitesList sitesList;
    @Mock
    private PageRepository pageRepository;
    @Mock
    private LemmaRepository lemmaRepository;
    @InjectMocks
    private StatisticsServiceImpl statisticsService;

    @BeforeEach
    public void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    public void testGetStatistics() {
        List<Site> sites = new ArrayList<>();
        Site site = new Site();
        site.setUrl("http://playback.ru/");
        site.setName("PlayBack.Ru");
        sites.add(site);

        when(sitesList.getSites()).thenReturn(sites);
        when(lemmaRepository.getLemmasCount()).thenReturn(10);
        when(pageRepository.getPagesCount()).thenReturn(5);

        StatisticsResponse response = statisticsService.getStatistics();

        assertNotNull(response);
        assertNotNull(response.getStatistics());
    }
}
