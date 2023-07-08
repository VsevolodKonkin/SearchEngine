package searchengine.services.indexing;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import searchengine.model.Site;
import searchengine.repository.PageRepository;
import java.util.concurrent.ForkJoinPool;

@RequiredArgsConstructor
public class SiteIndexing extends Thread {

    private final PageRepository pageRepository;
    private final String url;
    private final Site site;
    @Value("${user-agent}")
    private String userAgent;
    @Value("${referrer}")
    private String referrer;

    @Override
    public void run() {
        SiteMap siteMap = new SiteMap(url);
        SiteMapRecursiveAction siteMapRecur = new SiteMapRecursiveAction(url, siteMap,
                site, pageRepository);
        new ForkJoinPool().invoke(siteMapRecur);
    }
}
