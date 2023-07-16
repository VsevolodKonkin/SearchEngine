package searchengine.services.statistics;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import searchengine.config.Site;
import searchengine.config.SitesList;
import searchengine.dto.statistics.DetailedStatisticsItem;
import searchengine.dto.statistics.StatisticsData;
import searchengine.dto.statistics.StatisticsResponse;
import searchengine.dto.statistics.TotalStatistics;
import searchengine.model.SiteModel;
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
            if (url.contains("www.")) url = url.replaceAll("www.", "");
            SiteModel siteModel = getSiteModel(url, site.getName());
            detailedStatisticsItem.setUrl(url);
            detailedStatisticsItem.setPages(pageRepository.getPagesCountById(siteModel.getId()));
            detailedStatisticsItem.setLemmas(lemmaRepository.getLemmasCountById(siteModel.getId()));
            detailedStatisticsItem.setError(siteModel.getLastError());
            detailedStatisticsItem.setStatusTime(siteModel.getStatusTime());
            detailedStatisticsItem.setStatus(siteModel.getStatus());
            detailedStatisticsItem.setName(siteModel.getName());
            detailedStatisticsItems.add(detailedStatisticsItem);
        }
        return detailedStatisticsItems;
    }

    private SiteModel getSiteModel(String url, String name) {
        SiteModel siteModel = siteRepository.findByUrl(url);
        if (siteModel == null) {
            siteModel = new SiteModel();
            siteModel.setName(name);
            siteModel.setStatus(SiteStatus.FAILED);
            siteModel.setUrl(url);
            siteModel.setStatusTime(new Date());
            siteModel.setLastError("Индексация еще не запускалась");
            siteRepository.save(siteModel);
        }
        return siteModel;
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
