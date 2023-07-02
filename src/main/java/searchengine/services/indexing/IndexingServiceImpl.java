package searchengine.services.indexing;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import searchengine.dto.indexing.IndexingResponse;
import searchengine.model.Site;
import searchengine.model.enums.SiteStatus;
import searchengine.repository.PageRepository;
import searchengine.repository.SiteRepository;

import java.util.Date;
import java.util.List;
import java.util.concurrent.ForkJoinPool;

@Service
@RequiredArgsConstructor
public class IndexingServiceImpl implements IndexingService{
    ForkJoinPool forkJoinPool = new ForkJoinPool();
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
                forkJoinPool.shutdownNow();
                site.setStatus(SiteStatus.FAILED);
                siteRepository.save(site);
            }
        }

        return indexingResponse;
    }

    private boolean isStartIndexing(Site site) {
        siteRepository.deleteBySite(site);
        pageRepository.deleteBySite(site);
        List<Site> sitesList = siteRepository.findAll();
        for(Site sites : sitesList) {
            sites.setStatus(SiteStatus.INDEXING);
            siteRepository.save(sites);
            forkJoinPool.invoke(new SitemapNodeRecursiveAction(sites.getUrl(),
                    pageRepository, siteRepository, sites));
            sites.setName(site.getName());
            sites.setStatusTime(new Date());
            sites.setStatus(SiteStatus.INDEXED);
            siteRepository.save(sites);

        }

        return true;
    }
}
