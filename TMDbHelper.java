import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.util.*;
import org.json.*;

public class TMDbHelper {

    private static final String API_KEY = "4d2aab76554288a53c125c6b3628405e";

    public static List<MovieSuggestion> getTopMovieSuggestions(String query) {
        List<MovieSuggestion> suggestions = new ArrayList<>();
        try {
            String apiUrl = "https://api.themoviedb.org/3/search/movie?api_key=4d2aab76554288a53c125c6b3628405e&query=" + URLEncoder.encode(query, "UTF-8");
            URL url = new URL(apiUrl);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");

            BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream()));
            StringBuilder response = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                response.append(line);
            }
            reader.close();

            JSONObject json = new JSONObject(response.toString());
            JSONArray results = json.getJSONArray("results");

            for (int i = 0; i < Math.min(results.length(), 5); i++) {
                JSONObject movie = results.getJSONObject(i);
                String title = movie.getString("title");
                String releaseDate = movie.optString("release_date", "N/A");
                int id = movie.getInt("id");
                suggestions.add(new MovieSuggestion(title, releaseDate, id));
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return suggestions;
    }


    public static int getMovieIdFromTitle(String titleWithYear) {
        try {
            String title = titleWithYear.split("\\(")[0].trim();
            String encodedQuery = URLEncoder.encode(title, "UTF-8");
            String apiUrl = "https://api.themoviedb.org/3/search/movie?api_key=" + API_KEY + "&query=" + encodedQuery;

            String response = fetch(apiUrl);
            JSONObject json = new JSONObject(response);
            JSONArray results = json.getJSONArray("results");

            if (results.length() > 0) {
                return results.getJSONObject(0).getInt("id");
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return -1;
    }

    public static MovieDetails getMovieDetails(int movieId) {
        try {
            // Get movie details
            String movieUrl = "https://api.themoviedb.org/3/movie/" + movieId + "?api_key=" + API_KEY;
            String movieResponse = fetch(movieUrl);
            JSONObject movie = new JSONObject(movieResponse);

            String title = movie.getString("title");
            String year = movie.optString("release_date", "Unknown");
            if (!year.equals("Unknown") && year.length() >= 4) {
                year = year.substring(0, 4);
            }
            String overview = movie.optString("overview", "No description available.");
            String posterPath = movie.optString("poster_path", "");
            String posterUrl = posterPath.isEmpty() ? "" : "https://image.tmdb.org/t/p/w500" + posterPath;

            // Now fetch credits to get director
            String creditsUrl = "https://api.themoviedb.org/3/movie/" + movieId + "/credits?api_key=" + API_KEY;
            String creditsResponse = fetch(creditsUrl);
            JSONObject creditsJson = new JSONObject(creditsResponse);
            JSONArray crewArray = creditsJson.getJSONArray("crew");

            String director = "N/A";
            for (int i = 0; i < crewArray.length(); i++) {
                JSONObject crewMember = crewArray.getJSONObject(i);
                if ("Director".equalsIgnoreCase(crewMember.getString("job"))) {
                    director = crewMember.getString("name");
                    break;
                }
            }

            return new MovieDetails(title, year, director, overview, posterUrl);

        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }


    private static String fetch(String apiUrl) throws Exception {
        URL url = new URL(apiUrl);
        HttpURLConnection con = (HttpURLConnection) url.openConnection();
        con.setRequestMethod("GET");

        BufferedReader in = new BufferedReader(new InputStreamReader(con.getInputStream()));
        String inputLine;
        StringBuilder response = new StringBuilder();
        while ((inputLine = in.readLine()) != null) {
            response.append(inputLine);
        }
        in.close();

        return response.toString();
    }
}
