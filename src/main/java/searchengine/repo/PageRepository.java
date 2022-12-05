package searchengine.repo;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import searchengine.model.Page;
import searchengine.model.Site;

import java.util.Optional;

@Repository
public interface PageRepository extends CrudRepository<Page, Integer> {
    Page findByPath(String path);

    Optional<Page> findByIdAndSite(int id, Site site);

    @Query(value = "SELECT count(*) from Page where site_id = :id")
    long count(@Param("id") long id);
}