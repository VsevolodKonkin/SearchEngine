package searchengine;

import lombok.extern.slf4j.Slf4j;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import searchengine.model.PageData;
import searchengine.model.SiteData;
import searchengine.model.enums.SiteStatus;
import searchengine.services.indexing.IndexingServiceImpl;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.RecursiveTask;
import java.util.stream.Collectors;
@Slf4j
public class SiteResearcher extends RecursiveTask<PageData> {
    private final PageData pageData;
    private final SiteData siteData;
    private final IndexingServiceImpl indexingService;
    private final List<PageData> pageDataStore;
    public SiteResearcher(PageData pageData, List<PageData> pageDataDataStore, IndexingServiceImpl indexService) {
        this.pageData = pageData;
        this.indexingService = indexService;
        this.siteData = pageData.getSite();
        this.pageDataStore = pageDataDataStore;
    }

    @Override
    protected PageData compute() {
        if (siteData.getStatus() == SiteStatus.FAILED) {
            return pageData;
        }
        Document doc;
        try {
            doc = indexingService.getDocument(siteData.getUrl().concat(pageData.getPath()));
        } catch (IOException e) {
            pageData.setCode(404);
            return pageData;
        }
        pageData.setCode(doc.connection().response().statusCode());
        pageData.setContent(doc.html());
        List<SiteResearcher> siteResearcherList = getUrlChildResearcherList(doc);
        for (SiteResearcher siteResearcher : siteResearcherList) {
            siteResearcher.join();
        }
        synchronized (pageDataStore) {
            List<PageData> pagesToInsert = pageDataStore
                    .stream()
                    .filter(p -> p.getCode() > 0)
                    .collect(Collectors.toList());
            if (pagesToInsert.size() > 500) {
                indexingService.insertAllData(pagesToInsert, siteData);
                pageDataStore.removeAll(pagesToInsert);
            }
        }
        return pageData;
    }

    private List<SiteResearcher> getUrlChildResearcherList(Document doc) {
        List<SiteResearcher> siteResearcherList = new ArrayList<>();
        Elements elements = doc.select("a[href~=^[^#?]+$]");
        for (Element element : elements) {
            String urlChild = element.attr("abs:href");
            String relativeUrlChild = indexingService.getRelativeUrl(urlChild, siteData.getUrl());
            if (relativeUrlChild.isBlank() || relativeUrlChild.length() > PageData.MAX_LENGTH_PATH) {
                continue;
            }
            PageData pageDataChild;
            synchronized (pageDataStore) {
                if (indexingService.getPageRepository().existsByPathAndSite(relativeUrlChild, siteData)
                        || pageDataStore.stream().anyMatch(p -> p.getPath().equals(relativeUrlChild))) {
                    continue;
                }
                pageDataChild = new PageData(siteData, relativeUrlChild, 0, "");
                pageDataStore.add(pageDataChild);
            }
            SiteResearcher siteResearcher = new SiteResearcher(pageDataChild, pageDataStore, indexingService);
            siteResearcher.fork();
            siteResearcherList.add(siteResearcher);
        }
        return siteResearcherList;
    }
}
