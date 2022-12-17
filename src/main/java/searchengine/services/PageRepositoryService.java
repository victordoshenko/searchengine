package searchengine.services;

import searchengine.model.Index;
import searchengine.model.Page;
import searchengine.model.Site;

import java.util.List;
import java.util.Optional;

public interface PageRepositoryService {
    Page getPage (String pagePath);
    void save(Page page);
    Optional<Page> findPageById(int id);
    Optional<Page> findPageByPageIdAndSite(int pageId, Site site);
    long pageCount();
    long pageCount(long siteId);
    void deletePage(Page page);
    List<Page> findPagesByIndexing(List<Index> indexingList);
    void deleteAllPages(List<Page> pageList);
}
