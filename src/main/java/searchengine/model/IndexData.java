package searchengine.model;

import lombok.*;

import javax.persistence.*;
@Entity
@Setter
@Getter
@NoArgsConstructor
@EqualsAndHashCode
@Table(name = "search_index")
public class IndexData {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "page_id", referencedColumnName = "id")
    private PageData page;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "lemma_id", referencedColumnName = "id")
    private LemmaData lemma;

    @Column(columnDefinition = "FLOAT", name = "rank_number", nullable = false)
    private float rank;

    public IndexData(PageData pageData, LemmaData lemmaData, float rank) {
        this.page = pageData;
        this.lemma = lemmaData;
        this.rank = rank;
    }
}
