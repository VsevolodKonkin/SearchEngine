package searchengine.services.indexing;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import searchengine.SiteIndexing;
import searchengine.dto.indexing.IndexingResponse;
import searchengine.model.Site;
import searchengine.model.enums.SiteStatus;
import searchengine.repository.PageRepository;
import searchengine.repository.SiteRepository;

import java.util.Date;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadPoolExecutor;

@Service
@RequiredArgsConstructor
public class IndexingServiceImpl implements IndexingService{
    ThreadPoolExecutor threadPoolExecutor = (ThreadPoolExecutor) Executors.newFixedThreadPool(8);
    private final SiteRepository siteRepository;
    private final PageRepository pageRepository;


    @Override
    public IndexingResponse startIndexing() {
        IndexingResponse indexingResponse = new IndexingResponse();
        List<Site> siteList = siteRepository.findAll();
        boolean isIndexing;
        for (Site site : siteList) {
            if (site.getStatus().equals(SiteStatus.INDEXING)) {
                indexingResponse.setResult(false);
                indexingResponse.setError("Индексация уже запущена");
                continue;
            }
            isIndexing = isStartIndexing(site);
            if (isIndexing) {
                indexingResponse.setResult(true);
                indexingResponse.setError("");
            } else {
                indexingResponse.setResult(false);
                indexingResponse.setError("Индексацию не удалось запустить");
            }
        }
        return indexingResponse;
    }

    @Override
    public IndexingResponse stopIndexing() {
        IndexingResponse indexingResponse = new IndexingResponse();
        Iterable<Site> sites = siteRepository.findAll();
        for (Site site : sites) {
            if (isStartIndexing(site)) {
                threadPoolExecutor.shutdownNow();
                site.setStatus(SiteStatus.FAILED);
                siteRepository.save(site);

//                Формат ответа в случае успеха:
//                    'result': true
//                Формат ответа в случае ошибки:
//                    'result': false,
//                        'error': "Индексация не запущена"
            }
        }

        return indexingResponse;
    }

    private boolean isStartIndexing(Site site) {

        siteRepository.deleteBySite(site);
        pageRepository.deleteBySite(site);
        site.setStatus(SiteStatus.INDEXING);
        siteRepository.save(site);
        threadPoolExecutor.execute(new SiteIndexing(pageRepository, siteRepository, site));
        site.setName(site.getName());
        site.setStatusTime(new Date());
        site.setStatus(SiteStatus.INDEXED);
        siteRepository.save(site);
        return true;
    }
}
