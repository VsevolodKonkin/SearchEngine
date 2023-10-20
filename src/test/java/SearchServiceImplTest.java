import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.ResponseEntity;
import searchengine.Application;
import searchengine.dto.ErrorResponse;
import searchengine.repository.IndexRepository;
import searchengine.repository.LemmaRepository;
import searchengine.repository.PageRepository;
import searchengine.repository.SiteRepository;
import searchengine.services.search.SearchServiceImpl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest(classes = Application.class)
public class SearchServiceImplTest {
    @InjectMocks
    private SearchServiceImpl searchService;
    @Mock
    private SiteRepository siteRepository;
    @Mock
    private PageRepository pageRepository;
    @Mock
    private IndexRepository indexRepository;
    @Mock
    private LemmaRepository lemmaRepository;

    @BeforeEach
    public void setUp() {
        MockitoAnnotations.openMocks(this);
        searchService = new SearchServiceImpl(pageRepository, siteRepository, lemmaRepository, indexRepository);
    }

    @Test
    void testSearchPagesWithEmptyQuery() {
        String query = "";
        String site = "http://www.playback.ru";
        int offset = 0;
        int limit = 10;

        ResponseEntity response = searchService.searchPages(query, site, offset, limit);
        assertTrue(response.getBody() instanceof ErrorResponse);
        ErrorResponse errorResponse = (ErrorResponse) response.getBody();
        assertEquals("Задан пустой поисковый запрос", errorResponse.getError());
    }
}
