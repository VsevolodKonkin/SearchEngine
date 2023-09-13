package searchengine.services.indexing;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import searchengine.dto.indexing.IndexingResponse;
import searchengine.model.SiteModel;
import searchengine.model.enums.SiteStatus;
import searchengine.repository.IndexRepository;
import searchengine.repository.LemmaRepository;
import searchengine.repository.PageRepository;
import searchengine.repository.SiteRepository;
import searchengine.siteIndexing.SiteIndexing;

import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

@Service
@RequiredArgsConstructor
@Slf4j
public class IndexingServiceImpl implements IndexingService{
    ThreadPoolExecutor threadPoolExecutor = (ThreadPoolExecutor) Executors.newFixedThreadPool(8);
    private final SiteRepository siteRepository;
    private final PageRepository pageRepository;
    private final IndexRepository indexRepository;
    private final LemmaRepository lemmaRepository;
    private final AtomicBoolean indexingFlag = new AtomicBoolean(true);

    @Override
    public IndexingResponse startIndexing() {
        resetIndexingFlag();
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

    private void resetIndexingFlag() {
        indexingFlag.set(true);
    }

    @Override
    public IndexingResponse stopIndexing() {
        IndexingResponse indexingResponse = new IndexingResponse();
        Iterable<SiteModel> sites = siteRepository.findAll();
        boolean isIndexing = false;
        for (SiteModel siteModel : sites) {
            if (siteModel.getStatus().equals(SiteStatus.INDEXING)) {
                isIndexing = true;
                break;
            }
        }
        if (threadPoolExecutor.getActiveCount() == 0 && !isIndexing) {
            indexingResponse.setResult(false);
            indexingResponse.setError("Индексация не запущена");
        } else {
            indexingFlag.set(false);
            threadPoolExecutor.shutdownNow();
            try {
                threadPoolExecutor.awaitTermination(30, TimeUnit.SECONDS);
            } catch (Exception e) {
                e.printStackTrace();
                log.error("An error occurred:", e);
            }
            if (threadPoolExecutor.isShutdown()) {
                for (SiteModel siteModel : sites) {
                    if (siteModel.getStatus().equals(SiteStatus.INDEXING)) {
                        siteModel.setStatus(SiteStatus.FAILED);
                        siteModel.setLastError("Индексация остановлена пользователем");
                        siteRepository.save(siteModel);
                    }
                }
                indexingResponse.setResult(true);
                indexingResponse.setError("");
            } else {
                indexingResponse.setResult(false);
                indexingResponse.setError("Не удалось остановить индексацию");
            }
        }
        return indexingResponse;
    }

    @Override
    public IndexingResponse indexPage(String url) {
        IndexingResponse indexingResponse = new IndexingResponse();
        Iterable<SiteModel> sites = siteRepository.findAll();
        String siteString = "";
        SiteStatus siteStatus = null;
        SiteModel siteModel = null;
        for (SiteModel site : sites) {
            if (url.contains(site.getUrl())) {
                siteString = site.getUrl();
                siteStatus = site.getStatus();
                siteModel = site;
            }
        }
        if (siteString.isBlank()) {
            indexingResponse.setResult(false);
            indexingResponse.setError("Данная страница находится за пределами сайтов, " +
                    "указанных в конфигурационном файле");
        } else if (siteStatus.equals(SiteStatus.INDEXING)) {
            indexingResponse.setResult(false);
            indexingResponse.setError("Данная страница уже индексируется");
        } else {
            if (threadPoolExecutor.isShutdown() || threadPoolExecutor.isTerminated()) {
                threadPoolExecutor = (ThreadPoolExecutor) Executors.newFixedThreadPool(8);
            }
            SiteIndexing siteIndexing = new SiteIndexing(pageRepository, url, siteModel,
                    indexRepository, lemmaRepository, siteRepository, indexingFlag);
            threadPoolExecutor.execute(siteIndexing);
            indexingResponse.setResult(true);
            indexingResponse.setError("");
        }
        return indexingResponse;
    }

    private boolean isStartIndexing(SiteModel siteModel) {
        siteModel.setStatus(SiteStatus.INDEXING);
        siteModel.setLastError("");
        siteRepository.save(siteModel);
        if (threadPoolExecutor.getActiveCount() == 0) {
            threadPoolExecutor = (ThreadPoolExecutor) Executors.newFixedThreadPool(8);
        }
        threadPoolExecutor.execute(new SiteIndexing(pageRepository, siteModel.getUrl(), siteModel,
                indexRepository, lemmaRepository, siteRepository, indexingFlag));
          siteModel.setName(siteModel.getName());
          siteRepository.save(siteModel);
        return true;
    }
}
