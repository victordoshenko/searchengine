package searchengine.services;

import searchengine.model.Index;
import searchengine.model.Lemma;

import java.util.List;

public interface IndexRepositoryService {
    List<Index> getAllIndexingByLemma(Lemma lemma);

    List<Index> getAllIndexingByPageId(int pageId);

    void deleteAllIndexing(List<Index> indexingList);

    Index getIndexing(Lemma lemma, int pageId);

    void save(Index indexing);
}
