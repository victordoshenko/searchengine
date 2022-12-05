package searchengine.repo;

import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;
import searchengine.model.Index;
import searchengine.model.Lemma;

import java.util.List;

@Repository
public interface IndexRepository extends CrudRepository<Index, Integer> {
    List<Index> findByLemma(Lemma lemma);

    List<Index> findByPageId(int pageId);

    Index findByLemmaAndPageId(Lemma lemma, int pageId);
}
