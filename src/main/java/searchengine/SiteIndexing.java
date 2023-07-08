package searchengine;

import org.jsoup.Connection;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import searchengine.config.SitesList;
import searchengine.model.Page;
import searchengine.model.Site;
import searchengine.repository.IndexRepository;
import searchengine.repository.LemmaRepository;
import searchengine.repository.PageRepository;
import searchengine.repository.SiteRepository;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.RecursiveAction;
import java.util.regex.Pattern;

import static java.lang.Thread.sleep;

public class SiteIndexing extends Thread {
    private IndexRepository indexRepository;
    private LemmaRepository lemmaRepository;
    private PageRepository pageRepository;
    private SiteRepository siteRepository;
    private String url;
    private SitesList config;
    private Site site;

    public Page runOneSiteIndexing(String childUrl) {
        Page page = new Page();
        String path = childUrl.replaceAll(url, "");
        try {
            Connection.Response conResponse = Jsoup.connect(childUrl).timeout(5000).
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

    public void runAllSiteIndexing() {
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
//                    Page page = getPage(childUrl);
                    Page page = runOneSiteIndexing(childUrl);
                    pageRepository.save(page);

                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void run() {
       SiteMap siteMap = new SiteMap();
       List<String> listSiteMap = siteMap.createSiteMap();
       for (String page : listSiteMap) {
           page.
       }
    }

//    private Page getPage(String childUrl) {
//        Page page = new Page();
//        String path = childUrl.replaceAll(url, "");
//        try {
//            Connection.Response conResponse = Jsoup.connect(childUrl).timeout(50000).
//                    userAgent(config.getUserAgent()).referrer(config.getReferrer()).
//                    ignoreHttpErrors(true).ignoreContentType(true).execute();
//            page.setSite(site);
//            page.setPath(path);
//            page.setCode(conResponse.statusCode());
//            page.setContent(conResponse.body());
//            Page pageCheck = pageRepository.getPage(path);
//            if (pageCheck != null) {
//                pageRepository.delete(pageCheck);
//            }
//        } catch (IOException e) {
//            throw new RuntimeException(e);
//        }
//        return page;
//    }

    private String stripParams(String url) {
        return url.replaceAll("\\?.+","");
    }

    private boolean isCorrectUrl(String url) {
        Pattern patternRoot = Pattern.compile("^" + url);
        Pattern patternNotFile = Pattern.compile("([^\\s]+(\\.(?i)(jpg|png|gif|bmp|pdf))$)");
        Pattern patternNotAnchor = Pattern.compile("#([\\w\\-]+)?$");

        return patternRoot.matcher(url).lookingAt()
                && !patternNotFile.matcher(url).find()
                && !patternNotAnchor.matcher(url).find();
    }
}
