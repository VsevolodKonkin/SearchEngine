package searchengine.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
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
    @Modifying
    @Query(value = "DELETE FROM Page p WHERE p.site = :site", nativeQuery = true)
    void deleteBySite(@Param("site") Site site);

    @Query(value = "SELECT FROM Page p WHERE p.path = :path", nativeQuery = true)
    Page getPage(@Param("path") String path);
}