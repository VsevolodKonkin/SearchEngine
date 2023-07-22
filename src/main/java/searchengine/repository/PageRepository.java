package searchengine.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import searchengine.model.Page;


@Repository
public interface PageRepository extends JpaRepository<Page, Long> {
    @Query(value = "SELECT * FROM search_engine.page WHERE path = :path", nativeQuery = true)
    Page getPage(String path);

    @Query(value = "SELECT COUNT(*) FROM search_engine.page", nativeQuery = true)
    Integer getPagesCount();

    @Query(value = "SELECT COUNT(*) FROM search_engine.page WHERE site_id = :id", nativeQuery = true)
    Integer getPagesCountById(long id);
}