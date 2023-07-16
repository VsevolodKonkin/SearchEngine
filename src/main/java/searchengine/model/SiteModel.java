package searchengine.model;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import searchengine.model.enums.SiteStatus;

import javax.persistence.*;
import java.io.Serializable;

import java.util.*;

@Entity
@Setter
@Getter
@NoArgsConstructor
@Table(name = "site")
public class SiteModel implements Serializable {
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private long id;
    @Enumerated(EnumType.STRING)
    @Column(columnDefinition = "ENUM('INDEXING', 'INDEXED', 'FAILED')", nullable = false)
    private SiteStatus status;
    @Column(name = "status_time", nullable = false)
    private Date statusTime;
    @Column(name = "last_error", columnDefinition = "TEXT")
    private String lastError;
    @Column(columnDefinition = "VARCHAR(255)", nullable = false)
    private String url;
    @Column(columnDefinition = "VARCHAR(255)", nullable = false)
    private String name;
    @OneToMany(fetch = FetchType.LAZY, mappedBy = "site", cascade = CascadeType.ALL)
    private List<Page> pages = new ArrayList<>();
    @OneToMany(fetch = FetchType.LAZY, mappedBy = "site", cascade = CascadeType.ALL)
    private List<Lemma> lemma = new ArrayList<>();

    public SiteModel(SiteStatus status, Date statusTime,
                     String lastError, String url,
                     String name, List<Page> pageModelList,
                     List<Lemma> lemmaModelList) {
        this.status = status;
        this.statusTime = statusTime;
        this.lastError = lastError;
        this.url = url;
        this.name = name;
        this.pages = pageModelList;
        this.lemma = lemmaModelList;
    }
}
