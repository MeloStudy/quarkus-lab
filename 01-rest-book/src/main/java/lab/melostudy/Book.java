package lab.melostudy;

public class Book {
    public Integer id;
    public String title;
    public String author;
    public Integer year;

    public Book(Integer id, String title,
                String author, Integer year
    ) {
        this.id = id;
        this.title = title;
        this.author = author;
        this.year = year;
    }
}
