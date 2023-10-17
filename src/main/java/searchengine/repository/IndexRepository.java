package searchengine.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import searchengine.model.IndexData;
import searchengine.model.PageData;

@Repository
public interface IndexRepository extends JpaRepository<IndexData, Long> {
    boolean existsByLemma_LemmaAndPage(String lemma, PageData pageData);
    IndexData findFirstByLemma_LemmaAndPage(String lemma, PageData pageData);
}