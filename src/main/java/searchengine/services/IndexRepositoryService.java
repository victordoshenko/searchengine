package searchengine.services;

import searchengine.model.*;

import java.util.List;

public interface IndexRepositoryService {
    List<Index> getAllIndexingByLemma(Lemma lemma);

    List<Index> getAllIndexingByPage(Page page);

    void deleteAllIndexing(List<Index> indexingList);

    Index getIndexing(Lemma lemma, Page page);

    void save(Index indexing);
}
