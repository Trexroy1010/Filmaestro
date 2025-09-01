import javax.swing.*;
import javax.swing.event.*;
import java.awt.*;
import java.awt.event.*;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.*;
import java.sql.*;
import java.util.*;
import org.json.*;

public class WatchlistPage extends JFrame {
    private int userId;
    private JTextField searchField;
    private JList<MovieSuggestion> suggestionList;
    private DefaultListModel<MovieSuggestion> suggestionModel;
    private JPanel watchlistPanel;
    private JScrollPane watchlistScroll;

    public WatchlistPage(int userId) {
        this.userId = userId;
        setTitle("Your Watchlist");
        setSize(600, 700);
        setLocationRelativeTo(null);
        setLayout(new BorderLayout());
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);

        // ─── Top: Search bar + suggestions ─────────────────────────────────────
        JPanel topPanel = new JPanel(new BorderLayout());
        searchField = new JTextField();
        suggestionModel = new DefaultListModel<>();
        suggestionList = new JList<>(suggestionModel);
        suggestionList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);

        suggestionList.addMouseListener(new MouseAdapter() {
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2) {
                    MovieSuggestion sel = suggestionList.getSelectedValue();
                    if (sel != null) addMovieToWatchlist(sel);
                }
            }
        });

        topPanel.add(searchField, BorderLayout.NORTH);
        topPanel.add(new JScrollPane(suggestionList), BorderLayout.CENTER);
        add(topPanel, BorderLayout.NORTH);

        searchField.getDocument().addDocumentListener(new DocumentListener() {
            public void insertUpdate(DocumentEvent e) { updateSuggestions(); }
            public void removeUpdate(DocumentEvent e) { updateSuggestions(); }
            public void changedUpdate(DocumentEvent e) {}
        });

        // ─── Center: Personalized watchlist ────────────────────────────────────
        watchlistPanel = new JPanel(new GridLayout(0, 5, 10, 10));  // 5 columns
        watchlistScroll = new JScrollPane(watchlistPanel);
        watchlistScroll.getVerticalScrollBar().setUnitIncrement(16);  // smoother scroll
        add(watchlistScroll, BorderLayout.CENTER);


        loadWatchlist();
        setVisible(true);
    }

    private void updateSuggestions() {
        suggestionModel.clear();
        String q = searchField.getText().trim();
        if (q.length() < 2) return;

        new Thread(() -> {
            try {
                String api = "https://api.themoviedb.org/3/search/movie?api_key=4d2aab76554288a53c125c6b3628405e"
                        + "&query=" + URLEncoder.encode(q, "UTF-8");
                String resp = fetch(api);
                JSONArray arr = new JSONObject(resp).getJSONArray("results");

                SwingUtilities.invokeLater(() -> {
                    for (int i = 0; i < Math.min(5, arr.length()); i++) {
                        JSONObject m = arr.getJSONObject(i);
                        suggestionModel.addElement(new MovieSuggestion(
                                m.getInt("id"),
                                m.getString("title"),
                                m.optString("release_date", "").split("-")[0],
                                m.optString("poster_path", "")
                        ));
                    }
                });
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        }).start();
    }

    private void addMovieToWatchlist(MovieSuggestion movie) {
        try (Connection conn = DriverManager.getConnection(
                "jdbc:mysql://localhost:3306/letterboxd", "root", "0000")) {
            String sql = "INSERT IGNORE INTO watchlist (user_id, movie_name, movie_id) VALUES (?, ?, ?)";
            PreparedStatement st = conn.prepareStatement(sql);
            st.setInt(1, userId);
            st.setString(2, movie.title);
            st.setInt(3, movie.id);  // TMDb movie ID
            st.executeUpdate();


            // Add UI element
            SwingUtilities.invokeLater(() -> {
                watchlistPanel.add(createWatchlistPanel(movie));
                watchlistPanel.revalidate();
            });
        } catch (SQLException ex) {
            JOptionPane.showMessageDialog(this, "Movie already in watchlist.");
        }
    }

    private void loadWatchlist() {
        try (Connection conn = DriverManager.getConnection(
                "jdbc:mysql://localhost:3306/letterboxd", "root", "0000")) {
            String sql = "SELECT movie_name FROM watchlist WHERE user_id = ?";
            PreparedStatement st = conn.prepareStatement(sql);
            st.setInt(1, userId);
            ResultSet rs = st.executeQuery();
            while (rs.next()) {
                String title = rs.getString("movie_name");
                MovieSuggestion m = searchMovieByTitle(title);

                if (m != null) {
                    watchlistPanel.add(createWatchlistPanel(m));
                }


            }
        } catch (SQLException ex) {
            ex.printStackTrace();
        }
    }

    private JPanel createWatchlistPanel(MovieSuggestion m) {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setPreferredSize(new Dimension(100, 150));
        panel.setBackground(Color.BLACK);
        JLabel picLabel = new JLabel();
        picLabel.addMouseListener(new MouseAdapter() {
            public void mouseClicked(MouseEvent e) {
                int confirm = JOptionPane.showConfirmDialog(panel,
                        "Remove this movie from your watchlist?", "Confirm", JOptionPane.YES_NO_OPTION);
                if (confirm == JOptionPane.YES_OPTION) {
                    removeFromWatchlist(m.id, panel);
                }
            }
        });


        try {
            if (m.posterPath != null && !m.posterPath.isEmpty()) {
                URL imgUrl = new URL("https://image.tmdb.org/t/p/w200" + m.posterPath);
                ImageIcon icon = new ImageIcon(imgUrl);
                Image img = icon.getImage().getScaledInstance(100, 150, Image.SCALE_SMOOTH);
                picLabel = new JLabel(new ImageIcon(img));
                panel.add(picLabel, BorderLayout.CENTER);
            } else {
                JLabel placeholder = new JLabel("No Image", SwingConstants.CENTER);
                placeholder.setPreferredSize(new Dimension(100, 150));
                placeholder.setForeground(Color.WHITE);
                panel.add(placeholder, BorderLayout.CENTER);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        return panel;
    }


    private MovieSuggestion searchMovieByTitle(String title) {
        try {
            String api = "https://api.themoviedb.org/3/search/movie?api_key=4d2aab76554288a53c125c6b3628405e&query=" + URLEncoder.encode(title, "UTF-8");
            String resp = fetch(api);
            JSONArray arr = new JSONObject(resp).getJSONArray("results");
            if (arr.length() > 0) {
                JSONObject m = arr.getJSONObject(0);
                return new MovieSuggestion(
                        m.getInt("id"),
                        m.getString("title"),
                        m.optString("release_date", "").split("-")[0],
                        m.optString("poster_path", "")
                );
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    private void removeFromWatchlist(int movieId, JPanel panel) {
        try (Connection conn = DriverManager.getConnection(
                "jdbc:mysql://localhost:3306/letterboxd", "root", "12345")) {
            String sql = "DELETE FROM watchlist WHERE user_id = ? AND movie_id = ?";
            PreparedStatement st = conn.prepareStatement(sql);
            st.setInt(1, userId);
            st.setInt(2, movieId);

            st.executeUpdate();
            watchlistPanel.remove(panel);
            watchlistPanel.revalidate();
            watchlistPanel.repaint();
        } catch (SQLException ex) {
            ex.printStackTrace();
        }
    }

    // JSON fetching helper
    private static String fetch(String addr) throws Exception {
        HttpURLConnection c = (HttpURLConnection)new URL(addr).openConnection();
        c.setRequestMethod("GET");
        BufferedReader rd = new BufferedReader(new InputStreamReader(c.getInputStream()));
        StringBuilder sb = new StringBuilder(); String line;
        while ((line = rd.readLine()) != null) sb.append(line);
        rd.close();
        return sb.toString();
    }

    // Simple DTO to hold movie metadata
    private static class MovieSuggestion {
        int id; String title, year, posterPath;
        MovieSuggestion(int id, String t, String y, String pp) {
            this.id = id; this.title = t + " (" + y + ")";
            this.year = y; this.posterPath = pp;
        }
        public String toString() { return title; }
    }
}
