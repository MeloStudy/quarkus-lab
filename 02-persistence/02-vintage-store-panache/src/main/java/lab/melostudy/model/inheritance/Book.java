package lab.melostudy.model.inheritance;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import lab.melostudy.model.Publisher;

import java.time.LocalDate;

@Entity
public class Book extends Item {
    @Column(length = 15)
    public String isbn;

    @Column(name = "nb_of_pages")
    public Integer nbOfPages;

    @Column(name = "publication_date")
    public LocalDate publicationDate;

    @Column(length = 20)
    @Enumerated(EnumType.STRING)
    public Language language;

    @ManyToOne
    @JoinColumn(name = "publisher_fk") // redefine column name
    public Publisher publisher;
}
