package searchengine.siteIndexing;

import lombok.RequiredArgsConstructor;
import searchengine.model.SiteModel;
import searchengine.repository.PageRepository;

import java.util.concurrent.ForkJoinPool;

@RequiredArgsConstructor
public class SiteIndexing extends Thread {

    private final PageRepository pageRepository;
    private final String url;
    private final SiteModel siteModel;

    @Override
    public void run() {
        SiteMap siteMap = new SiteMap(url);
        SiteMapRecursiveAction siteMapRecur = new SiteMapRecursiveAction(url, siteMap,
                siteModel, pageRepository);
        new ForkJoinPool().invoke(siteMapRecur);
    }
}
