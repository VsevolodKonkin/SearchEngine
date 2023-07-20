package searchengine.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import searchengine.model.SiteModel;

@Repository
public interface SiteRepository extends JpaRepository<SiteModel, Long> {
    @Modifying
    @Query(value = "DELETE FROM search_engine.site WHERE site = :site", nativeQuery = true)
    void deleteBySite(@Param("site") SiteModel siteModel);
    @Query(value = "SELECT * FROM search_engine.site WHERE (:url IS NULL OR url LIKE :url)", nativeQuery = true)
    SiteModel findByUrl(String url);
}
