package lab.melostudy.model.inheritance;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.OneToMany;
import lab.melostudy.model.cardinality.Track;

import java.util.ArrayList;
import java.util.List;

@Entity
public class CD extends Item {
    @Column(name = "music_company")
    public String musicCompany;

    @Column(length = 100)
    public String genre;

    // to avoid override column definition
    // and not force to use join table t_items_t_tracks
    // mappedBy refers to the attribute "cd" in Track
    @OneToMany(mappedBy = "cd")
    public List<Track> tracks = new ArrayList<>();

}
