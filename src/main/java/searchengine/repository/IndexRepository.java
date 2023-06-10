package searchengine.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.stereotype.Repository;
import searchengine.model.Index;
import searchengine.model.Lemma;
import searchengine.model.Page;

import java.util.List;

@Repository
public interface IndexRepository extends JpaRepository<Index, Long> {

    @Transactional
    @Query(value = "select * from search_index where search_index.lemma_id in :lemmas " +
            "and search_index.page_id in :pages", nativeQuery = true)
    List<Index> findByPageAndLemmas(@Param("lemmas") List<Lemma> lemmaList,
                                         @Param("pages") List<Page> pages);

}