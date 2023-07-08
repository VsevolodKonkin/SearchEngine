package searchengine;

import org.jsoup.Connection;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import searchengine.model.Page;

import java.util.ArrayList;
import java.util.List;

public class SiteMap {

    public List<String> createSiteMap(){
        List<String> siteMap = new ArrayList<>();
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
        return siteMap;
    }


}
