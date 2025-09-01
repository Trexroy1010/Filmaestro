import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import java.awt.*;
import java.awt.event.*;
import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.sql.*;
import org.json.*;

public class ProfilePanel extends JPanel {
    private JLabel profilePicLabel;
    private JLabel nameLabel;
    private JButton editButton, refreshButton;
    private JLabel[] favFilmLabels = new JLabel[4];
    private JButton diaryButton, watchlistButton;
    private int userId;
    private String username;

    private FavoriteMovie[] favoriteSelections = new FavoriteMovie[4];
    private JTextField[] searchFields = new JTextField[4];

    public ProfilePanel(int userId) {
        setLayout(null);
        setBackground(Color.DARK_GRAY);

        this.userId = userId;
        username = fetchusername(userId);

        profilePicLabel = new JLabel();
        profilePicLabel.setBounds(40, 40, 100, 100);
        profilePicLabel.setOpaque(true);
        profilePicLabel.setBackground(Color.LIGHT_GRAY);
        profilePicLabel.setHorizontalAlignment(SwingConstants.CENTER);
        profilePicLabel.setText("Profile");
        profilePicLabel.setForeground(Color.BLACK);
        profilePicLabel.addMouseListener(new MouseAdapter() {
            public void mouseClicked(MouseEvent e) {
                chooseProfileImage();
            }
        });
        add(profilePicLabel);
        loadProfilePicture();

        nameLabel = new JLabel(username);
        nameLabel.setFont(new Font("Arial", Font.BOLD, 20));
        nameLabel.setForeground(Color.WHITE);
        nameLabel.setBounds(160, 50, 300, 30);
        add(nameLabel);

        editButton = new JButton("Edit Profile");
        editButton.setBounds(160, 90, 120, 30);
        add(editButton);

        refreshButton = new JButton("Refresh");
        refreshButton.setBounds(290, 90, 100, 30);
        refreshButton.addActionListener(e -> refreshFavorites());
        add(refreshButton);

        JLabel favTitle = new JLabel("Your Top 4 Favorite Films");
        favTitle.setBounds(40, 140, 400, 30);
        favTitle.setFont(new Font("Arial", Font.BOLD, 16));
        favTitle.setForeground(Color.WHITE);
        add(favTitle);


        int xOffset = 40;
        int favY = 180;
        int buttonY = 340;

        for (int i = 0; i < 4; i++) {
            favFilmLabels[i] = new JLabel("Fav " + (i + 1));
            favFilmLabels[i].setBounds(xOffset, 160, 100, 140);
            favFilmLabels[i].setOpaque(true);
            favFilmLabels[i].setBackground(Color.GRAY);
            favFilmLabels[i].setHorizontalAlignment(SwingConstants.CENTER);
            favFilmLabels[i].setForeground(Color.WHITE);
            add(favFilmLabels[i]);
            xOffset += 110;
        }

        editButton.addActionListener(e -> openEditFavoritesDialog());


        JButton searchFriendsButton = new JButton("Search Friends");
        searchFriendsButton.setBounds(400, buttonY + 160, 140, 40);
        searchFriendsButton.addActionListener(e -> {
            String email = JOptionPane.showInputDialog("Enter email of friend to search:");
            if (email != null && !email.trim().isEmpty()) {
                new SearchFriendWindow(email);
            }
        });

        add(searchFriendsButton);

        JLabel statsLabel = new JLabel();
        statsLabel.setBounds(40, 420, 400, 100);
        statsLabel.setFont(new Font("Arial", Font.PLAIN, 14));
        statsLabel.setForeground(Color.WHITE);
        add(statsLabel);

        updateUserStats(statsLabel);


        diaryButton = new JButton("Diary");
        diaryButton.setBounds(400, 360, 100, 40);
        diaryButton.addActionListener(e -> new Diary(userId));
        add(diaryButton);

        watchlistButton = new JButton("Watchlist");
        watchlistButton.setBounds(400, 410, 100, 40);
        watchlistButton.addActionListener(e -> new WatchlistPage(userId));
        add(watchlistButton);

        loadFavoritesFromDB();
    }

    private void openEditFavoritesDialog() {
        JDialog dialog = new JDialog((Frame) null, "Edit Favorites", true);
        dialog.setLayout(new GridLayout(6, 1));
        for (int i = 0; i < 4; i++) {
            int idx = i;
            JTextField field = new JTextField();
            searchFields[i] = field;
            dialog.add(field);

            field.getDocument().addDocumentListener(new DocumentListener() {
                public void insertUpdate(DocumentEvent e) { updateSuggestion(field, idx); }
                public void removeUpdate(DocumentEvent e) { updateSuggestion(field, idx); }
                public void changedUpdate(DocumentEvent e) {}
            });
        }

        JButton commit = new JButton("Commit");
        commit.addActionListener(e -> {
            for (int i = 0; i < 4; i++) {
                if (favoriteSelections[i] != null) {
                    favFilmLabels[i].setText("<html><center>" + favoriteSelections[i].title + "</center></html>");
                    favFilmLabels[i].setIcon(new ImageIcon(favoriteSelections[i].image));
                    saveFavoriteToDB(i, favoriteSelections[i]);
                }
            }
            dialog.dispose();
        });
        dialog.add(commit);
        dialog.setSize(400, 300);
        dialog.setLocationRelativeTo(this);
        dialog.setVisible(true);
    }

    private void updateSuggestion(JTextField field, int index) {
        String text = field.getText().trim();
        if (text.length() < 2) return;

        new Thread(() -> {
            try {
                String api = "https://api.themoviedb.org/3/search/movie?api_key=4d2aab76554288a53c125c6b3628405e&query=" + URLEncoder.encode(text, "UTF-8");
                String result = fetch(api);
                JSONArray arr = new JSONObject(result).getJSONArray("results");
                if (arr.length() > 0) {
                    JSONObject obj = arr.getJSONObject(0);
                    String posterPath = obj.optString("poster_path", "");
                    ImageIcon icon = null;
                    if (!posterPath.isEmpty()) {
                        URL imgUrl = new URL("https://image.tmdb.org/t/p/w200" + posterPath);
                        Image img = new ImageIcon(imgUrl).getImage().getScaledInstance(100, 140, Image.SCALE_SMOOTH);
                        icon = new ImageIcon(img);
                    }
                    FavoriteMovie movie = new FavoriteMovie(
                            obj.getString("title"),
                            obj.optString("release_date", "").split("-")[0],
                            posterPath,
                            icon != null ? icon.getImage() : null
                    );
                    favoriteSelections[index] = movie;
                }
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        }).start();
    }

    private void refreshFavorites() {
        for (int i = 0; i < 4; i++) {
            if (favoriteSelections[i] != null) {
                favFilmLabels[i].setText("<html><center>" + favoriteSelections[i].title + "</center></html>");
                favFilmLabels[i].setIcon(new ImageIcon(favoriteSelections[i].image));
            }
        }
    }

    private void loadFavoritesFromDB() {
        try (Connection conn = DriverManager.getConnection("jdbc:mysql://localhost:3306/letterboxd", "root", "0000")) {
            PreparedStatement stmt = conn.prepareStatement("SELECT slot, movie_title, poster_path FROM favorites WHERE user_id = ?");
            stmt.setInt(1, userId);
            ResultSet rs = stmt.executeQuery();
            while (rs.next()) {
                int slot = rs.getInt("slot");
                String title = rs.getString("movie_title");
                String poster = rs.getString("poster_path");
                ImageIcon icon = null;
                if (poster != null && !poster.isEmpty()) {
                    URL imgUrl = new URL("https://image.tmdb.org/t/p/w200" + poster);
                    Image img = new ImageIcon(imgUrl).getImage().getScaledInstance(100, 140, Image.SCALE_SMOOTH);
                    icon = new ImageIcon(img);
                }
                favFilmLabels[slot].setText("<html><center>" + title + "</center></html>");
                if (icon != null) favFilmLabels[slot].setIcon(icon);
                favoriteSelections[slot] = new FavoriteMovie(title, "", poster, icon != null ? icon.getImage() : null);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void saveFavoriteToDB(int slot, FavoriteMovie movie) {
        try (Connection conn = DriverManager.getConnection("jdbc:mysql://localhost:3306/letterboxd", "root", "0000")) {
            PreparedStatement stmt = conn.prepareStatement(
                    "REPLACE INTO favorites (user_id, slot, movie_title, poster_path) VALUES (?, ?, ?, ?)"
            );
            stmt.setInt(1, userId);
            stmt.setInt(2, slot);
            stmt.setString(3, movie.title);
            stmt.setString(4, movie.posterPath);
            stmt.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private String fetchusername(int userId) {
        try (Connection conn = DriverManager.getConnection("jdbc:mysql://localhost:3306/letterboxd", "root", "0000");
             PreparedStatement stmt = conn.prepareStatement("SELECT name FROM users WHERE id = ?")) {
            stmt.setInt(1, userId);
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) return rs.getString("name");
        } catch (Exception e) { e.printStackTrace(); }
        return "User";
    }

    private void chooseProfileImage() {
        JFileChooser chooser = new JFileChooser();
        int result = chooser.showOpenDialog(this);
        if (result == JFileChooser.APPROVE_OPTION) {
            File file = chooser.getSelectedFile();
            String path = file.getAbsolutePath();
            ImageIcon icon = new ImageIcon(path);
            Image img = icon.getImage().getScaledInstance(100, 100, Image.SCALE_SMOOTH);
            profilePicLabel.setIcon(new ImageIcon(img));
            profilePicLabel.setText("");
            saveProfilePicPathToDB(userId, path);
        }
    }

    private void loadProfilePicture() {
        try (Connection conn = DriverManager.getConnection("jdbc:mysql://localhost:3306/letterboxd", "root", "0000");
             PreparedStatement stmt = conn.prepareStatement("SELECT profile_pic_path FROM users WHERE id = ?")) {
            stmt.setInt(1, userId);
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                String path = rs.getString("profile_pic_path");
                if (path != null && !path.isEmpty()) {
                    ImageIcon icon = new ImageIcon(path);
                    Image img = icon.getImage().getScaledInstance(100, 100, Image.SCALE_SMOOTH);
                    profilePicLabel.setIcon(new ImageIcon(img));
                    profilePicLabel.setText("");
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void saveProfilePicPathToDB(int userId, String path) {
        try (Connection conn = DriverManager.getConnection("jdbc:mysql://localhost:3306/letterboxd", "root", "0000");
             PreparedStatement stmt = conn.prepareStatement("UPDATE users SET profile_pic_path = ? WHERE id = ?")) {
            stmt.setString(1, path);
            stmt.setInt(2, userId);
            stmt.executeUpdate();
        } catch (Exception e) { e.printStackTrace(); }
    }

    private String fetch(String urlStr) throws Exception {
        HttpURLConnection c = (HttpURLConnection) new URL(urlStr).openConnection();
        c.setRequestMethod("GET");
        BufferedReader rd = new BufferedReader(new InputStreamReader(c.getInputStream()));
        StringBuilder sb = new StringBuilder();
        String line;
        while ((line = rd.readLine()) != null) sb.append(line);
        rd.close();
        return sb.toString();
    }

    private static class FavoriteMovie {
        String title, year, posterPath;
        Image image;
        FavoriteMovie(String t, String y, String pp, Image img) {
            this.title = t;
            this.year = y;
            this.posterPath = pp;
            this.image = img;
        }
    }
    private void updateUserStats(JLabel statsLabel) {
        int watched = 0, watchlist = 0, reviews = 0;

        try (Connection conn = DriverManager.getConnection("jdbc:mysql://localhost:3306/letterboxd", "root", "12345")) {
            PreparedStatement st1 = conn.prepareStatement("SELECT COUNT(*) FROM movie_logs WHERE user_id = ?");
            st1.setInt(1, userId);
            ResultSet rs1 = st1.executeQuery();
            if (rs1.next()) watched = rs1.getInt(1);

            PreparedStatement st2 = conn.prepareStatement("SELECT COUNT(*) FROM watchlist WHERE user_id = ?");
            st2.setInt(1, userId);
            ResultSet rs2 = st2.executeQuery();
            if (rs2.next()) watchlist = rs2.getInt(1);

            PreparedStatement st3 = conn.prepareStatement("SELECT COUNT(*) FROM movie_logs WHERE user_id = ? AND review IS NOT NULL AND review != ''");
            st3.setInt(1, userId);
            ResultSet rs3 = st3.executeQuery();
            if (rs3.next()) reviews = rs3.getInt(1);
        } catch (SQLException e) {
            e.printStackTrace();
        }

        statsLabel.setText("<html>Watched: " + watched + "<br>Watchlist: " + watchlist + "<br>Reviews: " + reviews + "</html>");
    }



    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            JFrame f = new JFrame("Profile");
            f.setSize(600, 500);
            f.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            f.add(new ProfilePanel(1));
            f.setVisible(true);
        });
    }
}
