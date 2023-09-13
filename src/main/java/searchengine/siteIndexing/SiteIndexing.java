package searchengine.siteIndexing;

import lombok.RequiredArgsConstructor;
import searchengine.model.SiteModel;
import searchengine.repository.IndexRepository;
import searchengine.repository.LemmaRepository;
import searchengine.repository.PageRepository;
import searchengine.repository.SiteRepository;

import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.atomic.AtomicBoolean;

@RequiredArgsConstructor
public class SiteIndexing extends Thread {

    private final PageRepository pageRepository;
    private final String url;
    private final SiteModel siteModel;
    private final IndexRepository indexRepository;
    private final LemmaRepository lemmaRepository;
    private final SiteRepository siteRepository;
    private final AtomicBoolean indexingFlag;

    @Override
    public void run() {
        SiteMap siteMap = new SiteMap(url);
        if (!Thread.currentThread().isInterrupted()) {
            SiteMapRecursiveAction siteMapRecur = new SiteMapRecursiveAction(url, siteMap,
                    siteModel, pageRepository, indexRepository, lemmaRepository, siteRepository,
                    indexingFlag);
            ForkJoinPool forkJoinPool = new ForkJoinPool();
            forkJoinPool.invoke(siteMapRecur);
        } else {
            Thread.currentThread().interrupt();
        }
    }
}
