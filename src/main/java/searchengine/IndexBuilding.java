package searchengine;

import searchengine.model.Site;
import searchengine.model.Status;
import searchengine.services.*;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

@Component
public class IndexBuilding {

    private final static Log log = LogFactory.getLog(IndexBuilding.class);
    private final SearchSettings searchSettings;
    private final SiteRepositoryService siteRepositoryService;
    private final IndexRepositoryService indexRepositoryService;
    private final PageRepositoryService pageRepositoryService;
    private final LemmaRepositoryService lemmaRepositoryService;

    public IndexBuilding(SearchSettings searchSettings,
                         SiteRepositoryService siteRepositoryService,
                         IndexRepositoryService indexRepositoryService,
                         PageRepositoryService pageRepositoryService,
                         LemmaRepositoryService lemmaRepositoryService) {
        this.searchSettings = searchSettings;
        this.siteRepositoryService = siteRepositoryService;
        this.indexRepositoryService = indexRepositoryService;
        this.pageRepositoryService = pageRepositoryService;
        this.lemmaRepositoryService = lemmaRepositoryService;
    }

    ThreadPoolExecutor executor = (ThreadPoolExecutor) Executors.newFixedThreadPool(3);

    public boolean allSiteIndexing() throws InterruptedException {
        boolean isIndexing;
        List<Site> siteList = getSiteListFromConfig();
        for (Site site : siteList) {
            isIndexing = startSiteIndexing(site);
            if (!isIndexing){
                stopSiteIndexing();
                return false;
            }
        }
        return true;
    }

    public String checkedSiteIndexing(String url) throws InterruptedException {
        List<Site> siteList = siteRepositoryService.getAllSites();
        String baseUrl = "";
        for(Site site : siteList) {
            if(site.getStatus() != Status.INDEXED) {
                return "false";
            }
            if(url.contains(site.getUrl())){
                baseUrl = site.getUrl();
            }
        }
        if(baseUrl.isEmpty()){
            return "not found";
        } else {
            Site site = siteRepositoryService.getSite(baseUrl);
            site.setUrl(url);
            SiteIndexing indexing = new SiteIndexing(
                    site,
                    searchSettings,
                    siteRepositoryService,
                    indexRepositoryService,
                    pageRepositoryService,
                    lemmaRepositoryService,
                    false);
            executor.execute(indexing);
            site.setUrl(baseUrl);
            siteRepositoryService.save(site);
            return "true";
        }
    }

    private boolean startSiteIndexing(Site site) throws InterruptedException {
        Site site1 = siteRepositoryService.getSite(site.getUrl());
        if (site1 == null) {
            siteRepositoryService.save(site);
            SiteIndexing indexing = new SiteIndexing(
                    siteRepositoryService.getSite(site.getUrl()),
                    searchSettings,
                    siteRepositoryService,
                    indexRepositoryService,
                    pageRepositoryService,
                    lemmaRepositoryService,
                    true);
            executor.execute(indexing);
            return true;
        } else {
            if (!site1.getStatus().equals(Status.INDEXING)){
                SiteIndexing indexing = new SiteIndexing(
                        siteRepositoryService.getSite(site.getUrl()),
                        searchSettings,
                        siteRepositoryService,
                        indexRepositoryService,
                        pageRepositoryService,
                        lemmaRepositoryService,
                        true);
                executor.execute(indexing);
                return true;
            } else {
                return false;
            }
        }
    }

    public boolean stopSiteIndexing(){
        boolean isThreadAlive = false;
        if(executor.getActiveCount() == 0){
            return false;
        }

        executor.shutdownNow();
        try {
            isThreadAlive = executor.awaitTermination(5,TimeUnit.MINUTES);
        } catch (InterruptedException e) {
            log.error("Ошибка закрытия потоков: " + e);
        }
        if (isThreadAlive){
            List<Site> siteList = siteRepositoryService.getAllSites();
            for(Site site : siteList) {
                site.setStatus(Status.FAILED);
                siteRepositoryService.save(site);
            }
        }
        return isThreadAlive;
    }

    private List<Site> getSiteListFromConfig() {
        List<Site> siteList = new ArrayList<>();
        List<HashMap<String, String>> sites = searchSettings.getSite();
        for (HashMap<String, String> map : sites) {
            String url = "";
            String name = "";
            for (Map.Entry<String, String> siteInfo : map.entrySet()) {
                if (siteInfo.getKey().equals("name")) {
                    name = siteInfo.getValue();
                }
                if (siteInfo.getKey().equals("url")) {
                    url = siteInfo.getValue();
                }
            }
            Site site = new Site();
            site.setUrl(url);
            site.setName(name);
            site.setStatus(Status.FAILED);
            siteList.add(site);
        }
        return siteList;
    }
}
