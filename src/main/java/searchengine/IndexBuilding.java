package searchengine;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.stereotype.Component;
import searchengine.model.Site;
import searchengine.model.Status;
import searchengine.services.IndexRepositoryService;
import searchengine.services.LemmaRepositoryService;
import searchengine.services.PageRepositoryService;
import searchengine.services.SiteRepositoryService;

import java.util.*;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadPoolExecutor;

@Component
public class IndexBuilding {

    private static final Logger log = LogManager.getLogger();
    private final SearchSettings searchSettings;
    private final SiteRepositoryService siteRepositoryService;
    private final IndexRepositoryService indexRepositoryService;
    private final PageRepositoryService pageRepositoryService;
    private final LemmaRepositoryService lemmaRepositoryService;
    private final List<SiteIndexing> siteIndexingList = Collections.synchronizedList(new ArrayList<>());

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
            if (!isIndexing) {
                stopSiteIndexing();
                return false;
            }
        }
        return true;
    }

    public String checkedSiteIndexing(String url) throws InterruptedException {
        List<Site> siteList = siteRepositoryService.getAllSites();
        String baseUrl = "";
        for (Site site : siteList) {
            if (site.getStatus() != Status.INDEXED) {
                return "false";
            }
            if (url.contains(site.getUrl())) {
                baseUrl = site.getUrl();
            }
        }
        if (baseUrl.isEmpty()) {
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
                    false,
                    url);
            siteIndexingList.add(indexing);
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
                    true,
                    "");
            siteIndexingList.add(indexing);
            executor.execute(indexing);
            return true;
        } else {
            if (!site1.getStatus().equals(Status.INDEXING)) {
                SiteIndexing indexing = new SiteIndexing(
                        siteRepositoryService.getSite(site.getUrl()),
                        searchSettings,
                        siteRepositoryService,
                        indexRepositoryService,
                        pageRepositoryService,
                        lemmaRepositoryService,
                        true,
                        "");
                siteIndexingList.add(indexing);
                executor.execute(indexing);
                return true;
            } else {
                return false;
            }
        }
    }

    public boolean stopSiteIndexing() {
        if (executor.getActiveCount() == 0) {
            return false;
        }
        for (SiteIndexing siteIndexing : siteIndexingList) {
            siteIndexing.stopBuildSiteMap();
        }
        siteIndexingList.clear();
        List<Site> siteList = siteRepositoryService.getAllSites();
        for (Site site : siteList) {
            site.setStatus(Status.FAILED);
            site.setLastError("Индексация остановлена пользователем");
            siteRepositoryService.save(site);
        }
        return true;
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
