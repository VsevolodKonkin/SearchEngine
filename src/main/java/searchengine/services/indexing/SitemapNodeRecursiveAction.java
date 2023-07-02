package searchengine.services.indexing;


import lombok.RequiredArgsConstructor;
import org.jsoup.Connection;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import searchengine.config.SitesList;
import searchengine.model.Page;
import searchengine.model.Site;
import searchengine.repository.PageRepository;
import searchengine.repository.SiteRepository;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.RecursiveAction;
import java.util.regex.Pattern;
import static java.lang.Thread.sleep;

public class SitemapNodeRecursiveAction extends RecursiveAction {

    private String url;
    private SitesList config;
    private PageRepository pageRepository;
    private SiteRepository siteRepository;
    private Site site;

    public SitemapNodeRecursiveAction(String url, PageRepository pageRepository,
                                      SiteRepository siteRepository, Site site) {
        this.url = url;
        this.pageRepository = pageRepository;
        this.siteRepository = siteRepository;
        this.site = site;
    }

    @Override
    protected void compute() {
        try {
            sleep(150);
            Connection connection = Jsoup.connect(url).timeout(50000).
                    userAgent(config.getUserAgent()).referrer(config.getReferrer()).
                    ignoreHttpErrors(true).ignoreContentType(true);
            Document doc = connection.get();
            Elements elements = doc.select("body").select("a");
            for (Element a : elements) {
                String childUrl = a.absUrl("href");
                if (isCorrectUrl(childUrl)) {
                    childUrl = stripParams(childUrl);
                    Page page = getPage(childUrl);
                    pageRepository.save(page);

                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        List<SitemapNodeRecursiveAction> tasks = new ArrayList<>();
        for (SitemapNode child : node.getChildren()) {
            SitemapNodeRecursiveAction task = new SitemapNodeRecursiveAction(child);
            tasks.add(task);
            task.fork();
        }
        for (SitemapNodeRecursiveAction task : tasks) {
            task.join();
        }
    }

    private Page getPage(String childUrl) {
        Page page = new Page();
        String path = childUrl.replaceAll(url, "");
        try {
            Connection.Response conResponse = Jsoup.connect(childUrl).timeout(50000).
                    userAgent(config.getUserAgent()).referrer(config.getReferrer()).
                    ignoreHttpErrors(true).ignoreContentType(true).execute();
            page.setSite(site);
            page.setPath(path);
            page.setCode(conResponse.statusCode());
            page.setContent(conResponse.body());
            Page pageCheck = pageRepository.getPage(path);
            if (pageCheck != null) {
                pageRepository.delete(pageCheck);
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        return page;
    }

    private String stripParams(String url) {
        return url.replaceAll("\\?.+","");
    }

    private boolean isCorrectUrl(String url) {
        Pattern patternRoot = Pattern.compile("^" + node.getUrl());
        Pattern patternNotFile = Pattern.compile("([^\\s]+(\\.(?i)(jpg|png|gif|bmp|pdf))$)");
        Pattern patternNotAnchor = Pattern.compile("#([\\w\\-]+)?$");

        return patternRoot.matcher(url).lookingAt()
                && !patternNotFile.matcher(url).find()
                && !patternNotAnchor.matcher(url).find();
    }


}
