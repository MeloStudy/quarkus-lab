package lab.melostudy.model.inheritance;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;

@Entity
public class CD extends Item {
    @Column(name = "music_company")
    public String musicCompany;

    @Column(length = 100)
    public String genre;

}
