package searchengine.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import searchengine.model.Lemma;
import searchengine.model.Page;
import searchengine.model.Site;

import java.util.Collection;
import java.util.List;


@Repository
public interface PageRepository extends JpaRepository<Page, Long> {
    @Transactional
    long countBySiteId(Site siteId);

    @Transactional
    Page getById(long pageID);

    @Transactional
    Iterable<Page> findBySiteId(Site sitePath);

    @Transactional
    @Query(value = "SELECT * FROM search_index JOIN Page ON Page.id = search_index.page_id WHERE search_index.lemma_id IN :lemmas", nativeQuery = true)
    List<Page> findByLemmaList(@Param("lemmas") Collection<Lemma> lemmas);

}