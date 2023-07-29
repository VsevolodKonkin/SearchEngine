package searchengine.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import searchengine.model.Index;

@Repository
public interface IndexRepository extends JpaRepository<Index, Long> {
    @Query(value = "SELECT * FROM search_engine.search_index WHERE (:lemmaId IS NULL OR lemma_id LIKE :lemmaId)", nativeQuery = true)
    Index findByLemmaId(Long lemmaId);
}