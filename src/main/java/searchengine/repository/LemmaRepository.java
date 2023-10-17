package searchengine.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import searchengine.model.LemmaData;
import searchengine.model.SiteData;

import java.util.List;

@Repository
public interface LemmaRepository extends JpaRepository<LemmaData, Long> {
    @Query(value = "SELECT COUNT(*) FROM search_engine.lemma", nativeQuery = true)
    Integer getLemmasCount();
    @Query(value = "SELECT COUNT(*) FROM search_engine.lemma WHERE site_id = :id", nativeQuery = true)
    Integer getLemmasCountById(long id);
    LemmaData findFirstByLemmaAndSite(String lemma, SiteData siteData);
    List<LemmaData> findAllBySite(SiteData siteData);
}