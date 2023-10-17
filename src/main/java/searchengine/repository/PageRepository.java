package searchengine.repository;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import searchengine.model.PageData;
import searchengine.model.SiteData;

import java.util.List;


@Repository
public interface PageRepository extends JpaRepository<PageData, Long> {
    @Query(value = "SELECT COUNT(*) FROM search_engine.page", nativeQuery = true)
    Integer getPagesCount();
    @Query(value = "SELECT COUNT(*) FROM search_engine.page WHERE site_id = :id", nativeQuery = true)
    Integer getPagesCountById(@Param("id")long id);
    @Query(value = """
            SELECT p FROM PageData p
            JOIN IndexData i ON p = i.page\s
            JOIN LemmaData l ON l = i.lemma\s
            JOIN SiteData s ON s = p.site\s
            WHERE l.lemma = :lemma AND s = :site""")
    List<PageData> findAllByLemmaAndSite(String lemma, SiteData site, Pageable pageable);
    PageData findFirstByPathAndSite(String path, SiteData site);
    boolean existsByPathAndSite(String path, SiteData site);
    int countBySite(SiteData site);
    List<PageData> findFirst500BySite(SiteData site);
}