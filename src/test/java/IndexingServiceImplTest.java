import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.boot.test.context.SpringBootTest;
import searchengine.Application;
import searchengine.dto.indexing.IndexingResponse;
import searchengine.model.SiteModel;
import searchengine.model.enums.SiteStatus;
import searchengine.repository.IndexRepository;
import searchengine.repository.LemmaRepository;
import searchengine.repository.PageRepository;
import searchengine.repository.SiteRepository;
import searchengine.services.indexing.IndexingServiceImpl;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ThreadPoolExecutor;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

@SpringBootTest(classes = Application.class)
public class IndexingServiceImplTest {
    @InjectMocks
    private IndexingServiceImpl indexingService;
    @Mock
    private SiteRepository siteRepository;
    @Mock
    private PageRepository pageRepository;
    @Mock
    private IndexRepository indexRepository;
    @Mock
    private LemmaRepository lemmaRepository;
    @Mock
    private ThreadPoolExecutor threadPoolExecutor;

    @BeforeEach
    public void setUp() {
        MockitoAnnotations.openMocks(this);
        indexingService = new IndexingServiceImpl(siteRepository, pageRepository, indexRepository, lemmaRepository);
    }

    @Test
    public void testStartIndexing() {
        SiteModel site1 = new SiteModel();
        site1.setStatus(SiteStatus.INDEXING);

        SiteModel site2 = new SiteModel();
        site2.setStatus(SiteStatus.INDEXING);

        when(siteRepository.findAll()).thenReturn(List.of(site1, site2));

        IndexingResponse response = indexingService.startIndexing();

        assertFalse(response.isResult());
        assertEquals("Индексация уже запущена", response.getError());
    }

    @Test
    public void testStopIndexing() {
        SiteModel site1 = new SiteModel();
        site1.setStatus(SiteStatus.INDEXED);

        SiteModel site2 = new SiteModel();
        site2.setStatus(SiteStatus.INDEXED);

        List<SiteModel> siteList = new ArrayList<>();
        siteList.add(site1);
        siteList.add(site2);

        when(siteRepository.findAll()).thenReturn(siteList);
        when(pageRepository.findAll()).thenReturn(new ArrayList<>());
        when(threadPoolExecutor.isShutdown()).thenReturn(true);

        IndexingResponse response = indexingService.stopIndexing();
        assertFalse(response.isResult());
        assertEquals("Индексация не запущена", response.getError());
        site1.setStatus(SiteStatus.INDEXING);

        when(siteRepository.findAll()).thenReturn(siteList);
        when(pageRepository.findAll()).thenReturn(new ArrayList<>());
        when(threadPoolExecutor.isShutdown()).thenReturn(false);

        response = indexingService.stopIndexing();
        assertTrue(response.isResult());
        assertEquals("", response.getError());
    }

    @Test
    public void testIndexPage() {
        SiteModel site1 = new SiteModel();
        site1.setStatus(SiteStatus.INDEXED);
        site1.setUrl("http://www.playback.ru/");

        SiteModel site2 = new SiteModel();
        site2.setStatus(SiteStatus.INDEXING);
        site2.setUrl("https://volochek.life/");

        List<SiteModel> siteList = new ArrayList<>();
        siteList.add(site1);
        siteList.add(site2);

        when(siteRepository.findAll()).thenReturn(siteList);

        IndexingResponse response = indexingService.indexPage("http://www.playback.ru/");

        assertTrue(response.isResult());
        assertEquals("", response.getError());

        response = indexingService.indexPage("https://volochek.life/");

        assertFalse(response.isResult());
        assertEquals("Данная страница уже индексируется", response.getError());
    }
}
