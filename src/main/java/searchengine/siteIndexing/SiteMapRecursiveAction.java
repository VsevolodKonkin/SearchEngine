package searchengine.siteIndexing;

import org.jsoup.Connection;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import searchengine.model.Page;
import searchengine.model.SiteModel;
import searchengine.repository.PageRepository;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.RecursiveAction;
import java.util.regex.Pattern;

import static java.lang.Thread.sleep;

public class SiteMapRecursiveAction extends RecursiveAction {
//    @Value("${user-agent}")
    private String userAgent = "Mozilla/5.0 (Windows; U; WindowsNT 5.1; en-US; rv1.8.1.6) " +
            "Gecko/20070725 Firefox/2.0.0.6";
//    @Value("${referrer}")
    private String referrer = "http://www.google.com";
    private String url;
    private SiteMap siteMap;
    private SiteModel siteModel;
    private PageRepository pageRepository;

    public SiteMapRecursiveAction(String url, SiteMap siteMap, SiteModel siteModel,
                                  PageRepository pageRepository) {
        this.url = url;
        this.siteMap = siteMap;
        this.siteModel = siteModel;
        this.pageRepository = pageRepository;
    }

    public SiteMapRecursiveAction(SiteMap siteMap) {
        this.siteMap = siteMap;
    }

    @Override
    protected void compute() {
        try {
            sleep(150);
            Connection connection = Jsoup.connect(url).timeout(5000).
                    userAgent(userAgent).referrer(referrer).
                    ignoreHttpErrors(true).ignoreContentType(true);
            Document doc = connection.get();
            Elements elements = doc.select("body").select("a");
            for (Element a : elements) {
                String childUrl = a.absUrl("href");
                if (isCorrectUrl(childUrl)) {
                    childUrl = stripParams(childUrl);
                    siteMap.addChild(new SiteMap(childUrl));
                    runOneSiteIndexing(childUrl);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        List<SiteMapRecursiveAction> tasks = new ArrayList<>();
        for (SiteMap child : siteMap.getChildren()) {
            SiteMapRecursiveAction task = new SiteMapRecursiveAction(child);
            tasks.add(task);
            task.fork();
        }
        for (SiteMapRecursiveAction task : tasks) {
            task.join();
        }
    }

    private String stripParams(String url) {
        return url.replaceAll("\\?.+","");
    }

    private boolean isCorrectUrl(String url) {
        Pattern patternRoot = Pattern.compile("^" + siteMap.getUrl());
        Pattern patternNotFile = Pattern.compile("([^\\s]+(\\.(?i)(jpg|png|gif|bmp|pdf))$)");
        Pattern patternNotAnchor = Pattern.compile("#([\\w\\-]+)?$");

        return patternRoot.matcher(url).lookingAt()
                && !patternNotFile.matcher(url).find()
                && !patternNotAnchor.matcher(url).find();
    }

    private void runOneSiteIndexing(String childUrl) {
        Page page = new Page();
        String path = childUrl.replaceAll(url, "");
        try {
            Connection.Response conResponse = Jsoup.connect(childUrl).timeout(5000).
                    userAgent(userAgent).referrer(referrer).
                    ignoreHttpErrors(true).ignoreContentType(true).execute();
            page.setSite(siteModel);
            page.setPath(path);
            page.setCode(conResponse.statusCode());
            page.setContent(conResponse.body());
            Page pageCheck = pageRepository.getPage(path);
            if (pageCheck != null) {
                pageRepository.delete(pageCheck);
            }
            pageRepository.save(page);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
