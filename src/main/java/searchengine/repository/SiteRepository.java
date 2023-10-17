package searchengine.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import searchengine.model.SiteData;
import searchengine.model.enums.SiteStatus;

@Repository
public interface SiteRepository extends JpaRepository<SiteData, Long> {
    SiteData findFirstByUrl(String siteUrl);
    boolean existsByStatus(SiteStatus siteStatus);
    SiteData findFirstByName(String name);
}
