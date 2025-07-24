public class MovieDetails {
    private String title;
    private String year;
    private String director;
    private String summary;
    private String posterUrl;

    public MovieDetails(String title, String year, String director, String summary, String posterUrl) {
        this.title = title;
        this.year = year;
        this.director = director;
        this.summary = summary;
        this.posterUrl = posterUrl;
    }

    public String getTitle() {
        return title;
    }

    public String getYear() {
        return year;
    }

    public String getDirector() {
        return director;
    }

    public String getSummary() {
        return summary;
    }

    public String getPosterUrl() {
        return posterUrl;
    }
}
