public class MovieSuggestion {
    private String title;
    private String releaseDate;
    private int id;

    public MovieSuggestion(String title, String releaseDate, int id) {
        this.title = title;
        this.releaseDate = releaseDate;
        this.id = id;
    }

    public String getTitle() { return title; }
    public String getReleaseDate() { return releaseDate; }
    public int getId() { return id; }

    @Override
    public String toString() {
        return title + " (" + releaseDate + ")";
    }
}
