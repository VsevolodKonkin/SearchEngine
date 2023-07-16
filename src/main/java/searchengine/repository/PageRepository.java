package searchengine.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import searchengine.model.Page;
import searchengine.model.SiteModel;


@Repository
public interface PageRepository extends JpaRepository<Page, Long> {
    @Modifying
    @Query(value = "DELETE FROM search_engine.Page WHERE search_engine.site = :site", nativeQuery = true)
    void deleteBySite(@Param("site") SiteModel siteModel);

    @Query(value = "SELECT FROM search_engine.Page WHERE search_engine.path = :path", nativeQuery = true)
    Page getPage(@Param("path") String path);

    @Query(value = "SELECT COUNT(*) FROM search_engine.Page", nativeQuery = true)
    Integer getPagesCount();

    @Query(value = "SELECT COUNT(*) FROM search_engine.page WHERE site_id = :id", nativeQuery = true)
    Integer getPagesCountById(long id);
}