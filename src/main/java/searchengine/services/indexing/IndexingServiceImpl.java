package searchengine.services.indexing;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import searchengine.dto.indexing.IndexingResponse;
import searchengine.model.SiteModel;
import searchengine.model.enums.SiteStatus;
import searchengine.repository.PageRepository;
import searchengine.repository.SiteRepository;
import searchengine.siteIndexing.SiteIndexing;

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
        List<SiteModel> siteModelList = siteRepository.findAll();
        boolean isIndexing;
        for (SiteModel siteModel : siteModelList) {
            if (siteModel.getStatus().equals(SiteStatus.INDEXING)) {
                indexingResponse.setResult(false);
                indexingResponse.setError("Индексация уже запущена");
                continue;
            }
            isIndexing = isStartIndexing(siteModel);
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
        Iterable<SiteModel> sites = siteRepository.findAll();
        for (SiteModel siteModel : sites) {
            if (isStartIndexing(siteModel)) {
                threadPoolExecutor.shutdownNow();
                siteModel.setStatus(SiteStatus.FAILED);
                siteRepository.save(siteModel);
                indexingResponse.setResult(true);
                indexingResponse.setError("");
            }
            indexingResponse.setResult(false);
            indexingResponse.setError("Индексация не запущена");
        }
        return indexingResponse;
    }

    private boolean isStartIndexing(SiteModel siteModel) {
        siteModel.setStatus(SiteStatus.INDEXING);
        siteRepository.save(siteModel);
        threadPoolExecutor.execute(new SiteIndexing(pageRepository, siteModel.getUrl(), siteModel));
        siteModel.setName(siteModel.getName());
        siteModel.setStatusTime(new Date());
        siteModel.setStatus(SiteStatus.INDEXED);
        siteRepository.save(siteModel);
        return true;
    }
}
