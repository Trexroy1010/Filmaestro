import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import java.awt.*;
import java.awt.event.*;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.util.Vector;
import org.json.JSONArray;
import org.json.JSONObject;

public class LogPopup extends JDialog {
    private JTextField searchField;
    private JTextArea reviewArea;
    private JButton[] starButtons = new JButton[5];
    private int selectedRating = 0;
    private JList<String> resultList;
    private DefaultListModel<String> listModel;
    private final String API_KEY = "4d2aab76554288a53c125c6b3628405e";
    private JList<String> suggestionList;
    private DefaultListModel<String> suggestionModel;
    private JScrollPane suggestionScrollPane;
    private int userId;
    JButton submitButton = new JButton("Submit Review");
    public LogPopup(JFrame parent, int userId) {
        super(parent, "Log a Movie", true);
        setLayout(new BorderLayout());
        setSize(500, 500);
        setLocationRelativeTo(parent);

        // Search Panel
        JPanel searchPanel = new JPanel(new BorderLayout());
        searchField = new JTextField();
        listModel = new DefaultListModel<>();
        resultList = new JList<>(listModel);
        JScrollPane listScrollPane = new JScrollPane(resultList);
        listScrollPane.setPreferredSize(new Dimension(0, 100));

        searchPanel.add(new JLabel(" Search Movie: "), BorderLayout.NORTH);
        searchPanel.add(searchField, BorderLayout.CENTER);
        searchPanel.add(listScrollPane, BorderLayout.SOUTH);
        add(searchPanel, BorderLayout.NORTH);

        // Listener to search API
        searchField = new JTextField();
        suggestionModel = new DefaultListModel<>();
        suggestionList = new JList<>(suggestionModel);
        suggestionScrollPane = new JScrollPane(suggestionList);
        suggestionScrollPane.setVisible(false);

// Show suggestions below the search field
        JPanel suggestionPanel = new JPanel(new BorderLayout());
        suggestionPanel.add(searchField, BorderLayout.NORTH);
        suggestionPanel.add(suggestionScrollPane, BorderLayout.CENTER);

        searchField.getDocument().addDocumentListener(new DocumentListener() {
            @Override public void insertUpdate(DocumentEvent e) { searchMovies(); }
            @Override public void removeUpdate(DocumentEvent e) { searchMovies(); }
            @Override public void changedUpdate(DocumentEvent e) {}
        });

        searchField.addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                if (e.getKeyCode() == KeyEvent.VK_DOWN) {
                    suggestionList.requestFocusInWindow();
                    suggestionList.setSelectedIndex(0);
                }
            }
        });

        suggestionList.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                String selectedValue = suggestionList.getSelectedValue();
                if (selectedValue != null) {
                    searchField.setText(selectedValue);
                    suggestionScrollPane.setVisible(false);
                }
            }
        });

        suggestionList.addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                if (e.getKeyCode() == KeyEvent.VK_ENTER) {
                    String selectedValue = suggestionList.getSelectedValue();
                    if (selectedValue != null) {
                        searchField.setText(selectedValue);
                        suggestionScrollPane.setVisible(false);
                    }
                }
            }
        });

        searchPanel.add(suggestionPanel, BorderLayout.CENTER);


        // Review Area
        reviewArea = new JTextArea(8, 30);
        reviewArea.setLineWrap(true);
        reviewArea.setWrapStyleWord(true);
        JScrollPane scrollPane = new JScrollPane(reviewArea);
        JPanel reviewPanel = new JPanel(new BorderLayout());
        reviewPanel.setBorder(BorderFactory.createTitledBorder("Write your review:"));
        reviewPanel.add(scrollPane, BorderLayout.CENTER);
        add(reviewPanel, BorderLayout.CENTER);

        // Star Rating
        JPanel ratingPanel = new JPanel();
        ratingPanel.setBorder(BorderFactory.createTitledBorder("Rate the movie:"));
        for (int i = 0; i < 5; i++) {
            final int starIndex = i + 1;
            starButtons[i] = new JButton("☆");
            starButtons[i].setFont(new Font("SansSerif", Font.PLAIN, 20));
            starButtons[i].setFocusPainted(false);
            starButtons[i].addActionListener(e -> {
                selectedRating = starIndex;
                updateStars();
            });
            ratingPanel.add(starButtons[i]);
        }

        // Submit Button
        submitButton.addActionListener(e -> {
            String movieName = searchField.getText();
            String review = reviewArea.getText();

            if (movieName.isEmpty() || selectedRating == 0) {
                JOptionPane.showMessageDialog(this, "Please enter a movie name and select a rating.");
                return;
            }

            // Insert to DB
            DatabaseHelper.insertMovieLog(userId, movieName, review, selectedRating);

            System.out.println("Movie: " + movieName);
            System.out.println("Review: " + review);
            System.out.println("Rating: " + selectedRating);

            JOptionPane.showMessageDialog(this, "Your review has been added!");
            dispose(); // Close the popup
        });


        JPanel bottomPanel = new JPanel(new BorderLayout());
        bottomPanel.add(ratingPanel, BorderLayout.NORTH);
        bottomPanel.add(submitButton, BorderLayout.SOUTH);
        add(bottomPanel, BorderLayout.SOUTH);
    }

    private void fetchMovies(String query) {
        new Thread(() -> {
            try {
                String urlStr = "https://api.themoviedb.org/3/search/movie?api_key=" + API_KEY + "&query=" + query.replace(" ", "%20");
                URL url = new URL(urlStr);
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("GET");

                BufferedReader in = new BufferedReader(new InputStreamReader(conn.getInputStream()));
                StringBuilder response = new StringBuilder();
                String inputLine;
                while ((inputLine = in.readLine()) != null) {
                    response.append(inputLine);
                }
                in.close();

                JSONObject json = new JSONObject(response.toString());
                JSONArray results = json.getJSONArray("results");

                SwingUtilities.invokeLater(() -> {
                    listModel.clear();
                    for (int i = 0; i < Math.min(5, results.length()); i++) {
                        JSONObject movie = results.getJSONObject(i);
                        String title = movie.getString("title");
                        String year = movie.optString("release_date", "N/A").split("-")[0];
                        listModel.addElement(title + " (" + year + ")");
                    }
                });

            } catch (Exception ex) {
                ex.printStackTrace();
            }
        }).start();
    }
    private void searchMovies() {
        String query = searchField.getText().trim();
        if (query.length() < 2) {
            suggestionScrollPane.setVisible(false);
            return;
        }

        try {
            String apiKey = "4d2aab76554288a53c125c6b3628405e";
            String urlStr = "https://api.themoviedb.org/3/search/movie?api_key=" + apiKey +
                    "&query=" + URLEncoder.encode(query, "UTF-8");

            HttpURLConnection conn = (HttpURLConnection) new URL(urlStr).openConnection();
            conn.setRequestMethod("GET");

            BufferedReader in = new BufferedReader(new InputStreamReader(conn.getInputStream()));
            String inputLine,Builder = "";
            StringBuilder content = new StringBuilder();
            while ((inputLine = in.readLine()) != null) content.append(inputLine);
            in.close();

            JSONObject json = new JSONObject(content.toString());
            JSONArray results = json.getJSONArray("results");

            SwingUtilities.invokeLater(() -> {
                suggestionModel.clear();
                for (int i = 0; i < Math.min(5, results.length()); i++) {
                    JSONObject movie = results.getJSONObject(i);
                    String title = movie.getString("title");
                    String year = movie.optString("release_date", "N/A").split("-")[0];
                    suggestionModel.addElement(title + " (" + year + ")");
                }
                suggestionScrollPane.setVisible(!suggestionModel.isEmpty());
                revalidate();
            });

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void updateStars() {
        for (int i = 0; i < 5; i++) {
            starButtons[i].setText(i < selectedRating ? "★" : "☆");
        }
    }
    public class DatabaseHelper {
        private static final String DB_URL = "jdbc:mysql://localhost:3306/letterboxd";
        private static final String DB_USER = "root";
        private static final String DB_PASSWORD = "12345";

        public static void insertMovieLog(int userId, String movieName, String review, int rating) {
            String query = "INSERT INTO movie_logs (user_id, movie_name, review, rating) VALUES (?, ?, ?, ?)";

            try (Connection conn = DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD);
                 PreparedStatement stmt = conn.prepareStatement(query)) {

                stmt.setInt(1, userId);
                stmt.setString(2, movieName);
                stmt.setString(3, review);
                stmt.setInt(4, rating);
                stmt.executeUpdate();

            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

}
