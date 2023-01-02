package searchengine;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import searchengine.model.*;
import searchengine.morphology.MorphologyAnalyzer;
import searchengine.services.*;
import searchengine.sitemap.SiteMapBuilder;
import org.jsoup.Connection;
import org.jsoup.Jsoup;
import org.jsoup.UnsupportedMimeTypeException;
import org.jsoup.nodes.Document;
import org.jsoup.select.Elements;

import java.io.IOException;
import java.util.*;

public class SiteIndexing extends Thread {
    private static final Logger log = LogManager.getLogger();
    private final Site site;
    private final SearchSettings searchSettings;
    private final SiteRepositoryService siteRepositoryService;
    private final IndexRepositoryService indexRepositoryService;
    private final PageRepositoryService pageRepositoryService;
    private final LemmaRepositoryService lemmaRepositoryService;
    private final boolean allSite;
    private Boolean isStoppingByHuman = false;
    SiteMapBuilder builder;
    private final String url;

    public SiteIndexing(Site site,
                        SearchSettings searchSettings,
                        SiteRepositoryService siteRepositoryService,
                        IndexRepositoryService indexRepositoryService,
                        PageRepositoryService pageRepositoryService,
                        LemmaRepositoryService lemmaRepositoryService,
                        boolean allSite,
                        String url) {
        this.site = site;
        this.searchSettings = searchSettings;
        this.siteRepositoryService = siteRepositoryService;
        this.indexRepositoryService = indexRepositoryService;
        this.pageRepositoryService = pageRepositoryService;
        this.lemmaRepositoryService = lemmaRepositoryService;
        this.allSite = allSite;
        this.builder = new SiteMapBuilder(site.getUrl(), this.isInterrupted());
        this.url = url;
    }

    @Override
    public void run() {
        try {
            if (allSite) {
                runAllIndexing();
            } else {
                runOneSiteIndexing(this.url.equals("") ? site.getUrl() : this.url);
                if (!this.url.equals("")) {
                    log.info("Индексация одной страницы завершена.");
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    public void runAllIndexing() {
        isStoppingByHuman = false;
        site.setStatus(Status.INDEXING);
        site.setStatusTime(new Date());
        siteRepositoryService.save(site);
        builder.builtSiteMap();
        List<String> allSiteUrls = builder.getSiteMap();
        for (String url : allSiteUrls) {
            runOneSiteIndexing(url);
        }
        log.info("Индексация завершена.");
    }

    public void stopBuildSiteMap() {
        isStoppingByHuman = true;
        builder.stopBuildSiteMap();
    }

    public void runOneSiteIndexing(String searchUrl) {
        if (isStoppingByHuman) {
            return;
        }
        log.info("runOneSiteIndexing: " + searchUrl);
        site.setStatus(Status.INDEXING);
        site.setStatusTime(new Date());
        siteRepositoryService.save(site);

        List<String> fieldList = getFieldList();
        try {
            Page page = getSearchPage(searchUrl, site.getUrl(), site.getId());
            Page checkPage = pageRepositoryService.getPage(searchUrl.replaceAll(site.getUrl(), ""));

            if (checkPage != null) {
                prepareDbToIndexing(checkPage);
            }
            TreeMap<String, Integer> map = new TreeMap<>();
            TreeMap<String, Float> indexing = new TreeMap<>();
            for (String name : fieldList) {
                float weight = 1.0f;
                String stringByTag = getStringByTag(name, page.getContent());
                MorphologyAnalyzer analyzer = new MorphologyAnalyzer();
                TreeMap<String, Integer> tempMap = analyzer.textAnalyzer(stringByTag);
                map.putAll(tempMap);
                indexing.putAll(indexingLemmas(tempMap, weight));
            }

            lemmaToDB(map, site.getId());
            map.clear();

            pageToDb(page);

            indexingToDb(indexing, page.getPath());
            indexing.clear();

        } catch (UnsupportedMimeTypeException e) {
            site.setLastError("Формат страницы не поддерживается: " + searchUrl);
            site.setStatus(Status.FAILED);
        } catch (IOException e) {
            site.setLastError("Ошибка чтения страницы: " + searchUrl + "\n" + e.getMessage());
            site.setStatus(Status.FAILED);
        } finally {
            siteRepositoryService.save(site);
        }
        site.setStatus(Status.INDEXED);
        siteRepositoryService.save(site);
    }


    private void pageToDb(Page page) {
        pageRepositoryService.save(page);
    }

    private Page getSearchPage(String url, String baseUrl, int siteId) throws IOException {
        Page page = new Page();
        Connection.Response response = Jsoup.connect(url)
                .userAgent(searchSettings.getAgent())
                .referrer("http://www.google.com")
                .execute();

        String content = response.body();
        String path = url.replaceAll(baseUrl, "");
        int code = response.statusCode();
        page.setCode(code);
        page.setPath(path);
        page.setContent(content);
        page.setSiteId(siteId);
        return page;
    }

    private List<String> getFieldList() {
        return Arrays.asList("title", "body");
    }

    private String getStringByTag(String tag, String html) {
        String string = "";
        Document document = Jsoup.parse(html);
        Elements elements = document.select(tag);
        StringBuilder builder = new StringBuilder();
        elements.forEach(element -> builder.append(element.text()).append(" "));
        if (!builder.isEmpty()) {
            string = builder.toString();
        }
        return string;
    }

    private void lemmaToDB(TreeMap<String, Integer> lemmaMap, int siteId) {
        for (Map.Entry<String, Integer> lemma : lemmaMap.entrySet()) {
            lemmaRepositoryService.saveLemma(lemma.getKey(), lemma.getValue(), siteId);
        }
    }

    private TreeMap<String, Float> indexingLemmas(TreeMap<String, Integer> lemmas, float weight) {
        TreeMap<String, Float> map = new TreeMap<>();
        for (Map.Entry<String, Integer> lemma : lemmas.entrySet()) {
            String name = lemma.getKey();
            float w;
            if (!map.containsKey(name)) {
                w = (float) lemma.getValue() * weight;
            } else {
                w = map.get(name) + ((float) lemma.getValue() * weight);
            }
            map.put(name, w);
        }
        return map;
    }

    private void indexingToDb(TreeMap<String, Float> map, String path) {
        Page page = pageRepositoryService.getPage(path);
        int pageId = page.getId();
        int siteId = page.getSiteId();
        for (Map.Entry<String, Float> lemma : map.entrySet()) {
            String lemmaName = lemma.getKey();
            int lemmaId = lemmaRepositoryService.findLemmaIdByNameAndSiteId(lemmaName, siteId);
            Index index = new Index(pageId, lemmaId, lemma.getValue());
            indexRepositoryService.save(index);
        }
    }

    private void prepareDbToIndexing(Page page) {
        List<Index> indexingList = indexRepositoryService.getAllIndexingByPage(page);
        List<Lemma> allLemmasIdByIndexing = lemmaRepositoryService.findLemmasByIndexing(indexingList);
        List<Page> allPagesIdByIndexing = pageRepositoryService.findPagesByIndexing(indexingList);
        lemmaRepositoryService.deleteAllLemmas(allLemmasIdByIndexing);
        pageRepositoryService.deleteAllPages(allPagesIdByIndexing);
    }
}
