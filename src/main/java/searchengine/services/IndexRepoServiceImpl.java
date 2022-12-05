package searchengine.services;

import searchengine.model.Index;
import searchengine.model.Lemma;
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
    public List<Index> getAllIndexingByPageId(int pageId) {
        return indexRepository.findByPageId(pageId);
    }

    @Override
    public synchronized void deleteAllIndexing(List<Index> indexingList){
        indexRepository.deleteAll(indexingList);
    }

    @Override
    public Index getIndexing(Lemma lemma, int pageId) {
        Index indexing = null;
        try{
            indexing = indexRepository.findByLemmaAndPageId(lemma, pageId);
        } catch (Exception e) {
            System.out.println("lemmaId: " + lemma.getId() + " + pageId: " + pageId + " not unique");
            e.printStackTrace();
        }
        return indexing;
    }

    @Override
    public synchronized void save(Index index) {
        indexRepository.save(index);
    }

}
