package searchengine.services.implementation;

import org.springframework.stereotype.Service;
import searchengine.model.Index;
import searchengine.model.Page;
import searchengine.model.Site;
import searchengine.repo.PageRepository;
import searchengine.services.PageRepositoryService;

import java.util.List;
import java.util.Optional;

@Service
public class PageRepoServiceImpl implements PageRepositoryService {

    private final PageRepository pageRepository;

    public PageRepoServiceImpl(PageRepository pageRepository) {
        this.pageRepository = pageRepository;
    }

    @Override
    public Page getPage(String pagePath) {
        return pageRepository.findByPath(pagePath);
    }

    @Override
    public synchronized void save(Page page) {
        pageRepository.save(page);
    }

    @Override
    public Optional<Page> findPageById(int id) {
        return pageRepository.findById(id);
    }

    @Override
    public Optional<Page> findPageByPageIdAndSite(int pageId, Site site) {
        return pageRepository.findByIdAndSite(pageId, site);
    }

    @Override
    public long pageCount() {
        return pageRepository.count();
    }

    @Override
    public long pageCount(long siteId) {
        return pageRepository.count(siteId);
    }

    @Override
    public void deletePage(Page page) {
        pageRepository.delete(page);
    }

    @Override
    public List<Page> findPagesByIndexing(List<Index> indexingList) {
        int[] pageIdList = new int[indexingList.size()];
        for (int i = 0; i < indexingList.size(); i++) {
            pageIdList[i] = indexingList.get(i).getPageId();
        }
        return pageRepository.findByIds(pageIdList);
    }

    @Override
    public synchronized void deleteAllPages(List<Page> pageList) {
        if (pageList.size() > 0) {
            pageRepository.deleteAll(pageList);
        } else {
            pageRepository.deleteAll();
        }
    }

}
