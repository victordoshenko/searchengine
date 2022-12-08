package searchengine.services.implementation;

import searchengine.services.*;
import searchengine.model.Index;
import searchengine.model.Lemma;
import searchengine.model.Page;
import searchengine.repo.IndexRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class IndexRepoServiceImpl implements IndexRepositoryService {

    private final IndexRepository indexRepository;

    public IndexRepoServiceImpl(IndexRepository indexRepository) {
        this.indexRepository = indexRepository;
    }

    @Override
    public List<Index> getAllIndexingByLemma(Lemma lemma) {
        return indexRepository.findByLemma(lemma);
    }

    @Override
    public List<Index> getAllIndexingByPage(Page page) {
        return indexRepository.findByPage(page);
    }

    @Override
    public Index getIndexing(Lemma lemma, Page page) {
        Index indexing = null;
        try{
            indexing = indexRepository.findByLemmaAndPage(lemma, page);
        } catch (Exception e) {
            System.out.println("lemmaId: " + lemma.getId() + " + pageId: " + page.getId() + " not unique");
            e.printStackTrace();
        }
        return indexing;
    }

    @Override
    public synchronized void save(Index index) {
        indexRepository.save(index);
    }

}
