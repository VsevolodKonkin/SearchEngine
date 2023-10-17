package searchengine.services.statistics;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import searchengine.config.Site;
import searchengine.config.SitesList;
import searchengine.dto.statistics.DetailedStatisticsItem;
import searchengine.dto.statistics.StatisticsData;
import searchengine.dto.statistics.StatisticsResponse;
import searchengine.dto.statistics.TotalStatistics;
import searchengine.model.SiteData;
import searchengine.model.enums.SiteStatus;
import searchengine.repository.LemmaRepository;
import searchengine.repository.PageRepository;
import searchengine.repository.SiteRepository;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

@Service
@RequiredArgsConstructor
public class StatisticsServiceImpl implements StatisticsService {


    private final SitesList sites;
    private final SiteRepository siteRepository;
    private final PageRepository pageRepository;
    private final LemmaRepository lemmaRepository;

    @Override
    public StatisticsResponse getStatistics() {
        TotalStatistics totalStatistics = getTotalStatistics();
        List<DetailedStatisticsItem> detailedStatisticsItems = getDetailedStatisticsItem();
        return getStatisticsResponse(totalStatistics, detailedStatisticsItems);
    }

    private TotalStatistics getTotalStatistics() {
        TotalStatistics totalStatistics = new TotalStatistics();
        totalStatistics.setSites(sites.getSites().size());
        totalStatistics.setLemmas(lemmaRepository.getLemmasCount());
        totalStatistics.setPages(pageRepository.getPagesCount());
        totalStatistics.setIndexing(true);
        return totalStatistics;
    }

    private List<DetailedStatisticsItem> getDetailedStatisticsItem() {
        List<DetailedStatisticsItem> detailedStatisticsItems = new ArrayList<>();
        List<Site> sitesLists = sites.getSites();
        for (Site site : sitesLists) {
            DetailedStatisticsItem detailedStatisticsItem = new DetailedStatisticsItem();
            String url = site.getUrl();
            SiteData siteData = getSiteModel(url, site.getName());
            if (url.endsWith("/")) url = url.substring(0, url.length() - 1);
            detailedStatisticsItem.setUrl(url);
            detailedStatisticsItem.setPages(pageRepository.getPagesCountById(siteData.getId()));
            detailedStatisticsItem.setLemmas(lemmaRepository.getLemmasCountById(siteData.getId()));
            detailedStatisticsItem.setError(siteData.getLastError());
            detailedStatisticsItem.setStatusTime(siteData.getStatusTime());
            detailedStatisticsItem.setStatus(siteData.getStatus());
            detailedStatisticsItem.setName(siteData.getName());
            detailedStatisticsItems.add(detailedStatisticsItem);
        }
        return detailedStatisticsItems;
    }

    private SiteData getSiteModel(String url, String name) {
        if (url.endsWith("/")) url = url.substring(0, url.length() - 1);
        SiteData siteData = siteRepository.findFirstByUrl(url);
        if (siteData == null) {
            siteData = new SiteData();
            siteData.setName(name);
            siteData.setStatus(SiteStatus.FAILED);
            siteData.setUrl(url);
            siteData.setStatusTime(new Date());
            siteData.setLastError("Индексация еще не запускалась");
            siteRepository.save(siteData);
        }
        return siteData;
    }

    private StatisticsResponse getStatisticsResponse(TotalStatistics totalStatistics,
                                                     List<DetailedStatisticsItem> detailedStatisticsItems) {
        StatisticsResponse statisticsResponse = new StatisticsResponse();
        StatisticsData statisticsData = new StatisticsData();
        statisticsData.setDetailed(detailedStatisticsItems);
        statisticsData.setTotal(totalStatistics);
        statisticsResponse.setStatistics(statisticsData);
        statisticsResponse.setResult(true);
        return statisticsResponse;
    }
}
