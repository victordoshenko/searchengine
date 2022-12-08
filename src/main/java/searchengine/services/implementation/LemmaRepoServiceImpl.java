package searchengine.services.implementation;

import searchengine.services.*;
import org.springframework.stereotype.Service;
import searchengine.model.Index;
import searchengine.model.Lemma;
import searchengine.repo.LemmaRepository;

import java.util.List;

@Service
public class LemmaRepoServiceImpl implements LemmaRepositoryService {

    private final LemmaRepository lemmaRepository;

    public LemmaRepoServiceImpl(LemmaRepository lemmaRepository) {
        this.lemmaRepository = lemmaRepository;
    }

    @Override
    public List<Lemma> getLemma(String lemmaName) {
        List<Lemma> lemmas = null;
        try {
            lemmas = lemmaRepository.findByLemma(lemmaName);
        } catch (Exception e) {
            System.out.println(lemmaName);
            e.printStackTrace();
        }
        return lemmas;
    }

    @Override
    public synchronized void save(Lemma lemma) {
        lemmaRepository.save(lemma);
    }

    @Override
    public long lemmaCount() {
        return lemmaRepository.count();
    }

    @Override
    public long lemmaCount(long siteId) {
        return lemmaRepository.count(siteId);
    }

    @Override
    public synchronized void deleteAllLemmas(List<Lemma> lemmaList) {
        if (lemmaList.size() > 0) {
            lemmaRepository.deleteAll(lemmaList);
        } else {
            lemmaRepository.deleteAll();
        }
    }

    @Override
    public List<Lemma> findLemmasByIndexing(List<Index> indexingList) {
        int[] lemmaIdList = new int[indexingList.size()];
        for (int i = 0; i < indexingList.size(); i++) {
            lemmaIdList[i] = indexingList.get(i).getLemmaId();
        }
        return lemmaRepository.findByIds(lemmaIdList);
    }
}
