package searchengine.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import searchengine.model.Index;

import java.util.List;

@Repository
public interface IndexRepository extends JpaRepository<Index, Long> {
    @Query(value = "SELECT * FROM search_engine.search_index WHERE (:lemmaId IS NULL OR lemma_id LIKE :lemmaId)", nativeQuery = true)
    Index findByLemmaId(Long lemmaId);
    @Query(value = "SELECT * FROM search_engine.search_index si WHERE si.lemma_id = " +
            "(SELECT l.id FROM lemma l WHERE l.lemma = :lemma) AND " +
            "si.page_id IN (SELECT p.id FROM page p WHERE p.site_id = :siteId)", nativeQuery = true)
    List<Index> findByLemmaAndSite(@Param("lemma") String lemma, @Param("siteId") long siteId);
}