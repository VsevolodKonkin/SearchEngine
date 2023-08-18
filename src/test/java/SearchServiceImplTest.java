import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.boot.test.context.SpringBootTest;
import searchengine.Application;
import searchengine.dto.search.SearchData;
import searchengine.dto.search.SearchResponse;
import searchengine.searching.Searching;
import searchengine.services.search.SearchService;
import searchengine.services.search.SearchServiceImpl;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@SpringBootTest(classes = Application.class)
public class SearchServiceImplTest {
    @InjectMocks
    private SearchServiceImpl searchService;
    @Mock
    private Searching searching;

    @BeforeEach
    public void setUp() {
        MockitoAnnotations.openMocks(this);
        searchService = new SearchServiceImpl(searching);
    }

    @Test
    public void testGetDataWithEmptyQuery() {
        SearchResponse response = searchService.getData("", "http://playback.ru/", 0, 10);

        assertFalse(response.isResult());
        assertEquals("Задан пустой поисковый запрос", response.getError());
        assertNull(response.getData());
    }
}
