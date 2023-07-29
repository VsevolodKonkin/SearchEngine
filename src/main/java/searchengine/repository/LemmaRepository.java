package searchengine.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import searchengine.model.Lemma;

@Repository
public interface LemmaRepository extends JpaRepository<Lemma, Long> {
    @Query(value = "SELECT COUNT(*) FROM search_engine.lemma", nativeQuery = true)
    Integer getLemmasCount();

    @Query(value = "SELECT COUNT(*) FROM search_engine.lemma WHERE site_id = :id", nativeQuery = true)
    Integer getLemmasCountById(long id);

    @Query(value = "SELECT * FROM search_engine.lemma WHERE (:lemma IS NULL OR lemma LIKE :lemma)", nativeQuery = true)
    Lemma findLemma(String lemma);
}