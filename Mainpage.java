import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;
import java.io.File;
import java.net.URL;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class Mainpage extends JFrame {
    JFrame frame = new JFrame("Home - MovieLog");

    JLabel welcomeLabel = new JLabel();
    JButton logButton = new JButton("+ Log");
    JButton logoutButton = new JButton("Log Out");
    JButton refreshButton = new JButton("Refresh");

    JLabel profilePictureLabel = new JLabel();

    JPanel topPanel = new JPanel();
    JPanel activityPanel = new JPanel();
    JScrollPane scrollPane;


    String userName;
    private int userId;

    public Mainpage(int userId) {
        this.userId = userId;
        userName = fetchUserName(userId);
        welcomeLabel.setText("Welcome, " + userName + "!");

        frame.setSize(600, 900);
        frame.setLayout(null);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setLocationRelativeTo(null);

        setupTopPanel();
        setupActivityPanel();

        frame.setVisible(true);
    }

    private String fetchUserName(int userId) {
        String url = "jdbc:mysql://localhost:3306/letterboxd";
        String dbUser = "root";
        String dbPass = "0000";

        String query = "SELECT name FROM users WHERE id = ?";

        try (Connection conn = DriverManager.getConnection(url, dbUser, dbPass);
             PreparedStatement stmt = conn.prepareStatement(query)) {

            stmt.setInt(1, userId);
            ResultSet rs = stmt.executeQuery();

            if (rs.next()) {
                return rs.getString("name");
            } else {
                return "User";
            }

        } catch (Exception e) {
            e.printStackTrace();
            return "User";
        }
    }

    private void setupTopPanel() {
        topPanel.setLayout(null);
        topPanel.setBounds(0, 0, 600, 80);

        String profilePicPath = fetchProfilePicPath(userId);
        if (profilePicPath != null && !profilePicPath.isEmpty()) {
            ImageIcon icon = new ImageIcon(profilePicPath);
            Image img = icon.getImage().getScaledInstance(50, 50, Image.SCALE_SMOOTH);
            profilePictureLabel.setIcon(new ImageIcon(img));
        } else {
            profilePictureLabel.setText("No Photo");
        }
        profilePictureLabel.setBounds(20, 10, 50, 50);
        profilePictureLabel.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        profilePictureLabel.addMouseListener(new MouseAdapter() {
            public void mouseClicked(MouseEvent evt) {
                JOptionPane.showMessageDialog(null, "Change Profile Pic (coming soon)");
            }
        });
        topPanel.add(profilePictureLabel);

        // Fetch profile picture path from DB
        String picPath = fetchProfilePicPath(userId);
        ImageIcon profileIcon;
        if (picPath != null && !picPath.isEmpty()) {
            Image img = new ImageIcon(picPath).getImage().getScaledInstance(60, 60, Image.SCALE_SMOOTH);
            profileIcon = new ImageIcon(img);
        } else {
            profileIcon = new ImageIcon(new BufferedImage(60, 60, BufferedImage.TYPE_INT_ARGB)); // blank
        }

        profilePictureLabel.setIcon(profileIcon);
        profilePictureLabel.setBounds(20, 10, 60, 60);
        profilePictureLabel.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        profilePictureLabel.setToolTipText("Click to change picture");
        topPanel.add(profilePictureLabel);

// Add click listener to change picture
        profilePictureLabel.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseClicked(java.awt.event.MouseEvent evt) {
                chooseAndSaveProfilePicture();
            }
        });

        welcomeLabel.setText("Welcome back \"" + userName + "\"");
        welcomeLabel.setFont(new Font("SansSerif", Font.BOLD, 18));
        welcomeLabel.setBounds(20, 20, 300, 30);
        welcomeLabel.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        welcomeLabel.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseClicked(java.awt.event.MouseEvent evt) {
                JFrame profileFrame = new JFrame("Your Profile");
                profileFrame.setSize(600, 900);
                profileFrame.setLocationRelativeTo(frame);
                profileFrame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
                profileFrame.add(new ProfilePanel(userId));  // Passing userId to personalize later
                profileFrame.setVisible(true);
            }
        });


        logButton.setBounds(350, 20, 100, 30);
        logButton.addActionListener(e -> {
            LogPopup logPopup = new LogPopup(this, userId);
            logPopup.setVisible(true);
        });

        logoutButton.setBounds(450, 20, 100, 30);
        logoutButton.addActionListener(e -> {
            frame.dispose();
            new login();
        });
        refreshButton.setBounds(380, 50, 100, 30);
        refreshButton.addActionListener(e -> {
            activityPanel.removeAll(); // Clear old content

            List<JPanel> newPanels = fetchAndCreateReviewPanels();
            for (JPanel panel : newPanels) {
                activityPanel.add(panel);
            }

            activityPanel.revalidate();
            activityPanel.repaint();
        });


        topPanel.add(welcomeLabel);
        topPanel.add(logButton);
        topPanel.add(logoutButton);
        topPanel.add(refreshButton);

        frame.add(topPanel);
        topPanel.setBackground(Color.LIGHT_GRAY);
    }

    private void setupActivityPanel() {
        activityPanel.setLayout(new BoxLayout(activityPanel, BoxLayout.Y_AXIS));
        activityPanel.setBackground(Color.YELLOW);

        List<JPanel> reviewPanels = fetchAndCreateReviewPanels();
        for (JPanel panel : reviewPanels) {
            activityPanel.add(panel);
        }

        scrollPane = new JScrollPane(activityPanel);
        scrollPane.setBounds(0, 100, 600, 700);
        scrollPane.getVerticalScrollBar().setUnitIncrement(16);
        frame.add(scrollPane);
    }

    private List<JPanel> fetchAndCreateReviewPanels() {
        List<JPanel> panels = new ArrayList<>();
        String url = "jdbc:mysql://localhost:3306/letterboxd";
        String dbUser = "root";
        String dbPass = "12345";
        String query = "SELECT movie_logs.*, users.name AS reviewer_name FROM movie_logs JOIN users ON movie_logs.user_id = users.id ORDER BY movie_logs.id DESC";

        try (Connection conn = DriverManager.getConnection(url, dbUser, dbPass);
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(query)) {

            while (rs.next()) {
                String movieName = rs.getString("movie_name");
                String reviewText = rs.getString("review");
                int likes = rs.getInt("likes");
                int dislikes = rs.getInt("dislikes");
                int rating = rs.getInt("rating");
                StringBuilder stars = new StringBuilder();
                String reviewerName = rs.getString("reviewer_name");

                for (int i = 0; i < rating; i++) {
                    stars.append("★");
                }
                for (int i = rating; i < 5; i++) {
                    stars.append("☆");
                }
                JLabel reviewerLabel = new JLabel("Reviewed by: " + reviewerName);
                reviewerLabel.setFont(new Font("Arial", Font.ITALIC, 12));
                reviewerLabel.setBounds(70, 125, 200, 20); // adjust as needed


                JLabel starLabel = new JLabel("Rating: " + stars.toString());
                starLabel.setFont(new Font("Serif", Font.PLAIN, 18));


                MovieDetails details = TMDbHelper.getMovieDetails(TMDbHelper.getMovieIdFromTitle(movieName));

                JPanel postPanel = new JPanel();
                postPanel.setLayout(null);
                postPanel.setPreferredSize(new Dimension(580, 200));
                postPanel.setBorder(BorderFactory.createLineBorder(Color.GRAY));

                JLabel poster = new JLabel();
                poster.setBounds(10, 10, 50, 75);
                try {
                    URL urlImg = new URL(details.getPosterUrl());
                    ImageIcon icon = new ImageIcon(urlImg);
                    Image img = icon.getImage().getScaledInstance(50, 75, Image.SCALE_SMOOTH);
                    poster.setIcon(new ImageIcon(img));
                } catch (Exception e) {
                    poster.setText("No Poster");
                }
                postPanel.add(poster);

                JLabel titleLabel = new JLabel("" + movieName);
                titleLabel.setFont(new Font("Arial", Font.BOLD, 14));
                titleLabel.setBounds(70, 10, 400, 20);
                titleLabel.setForeground(Color.BLUE);
                titleLabel.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
                titleLabel.addMouseListener(new java.awt.event.MouseAdapter() {
                    public void mouseClicked(java.awt.event.MouseEvent e) {
                        new MovieDetailsPopup(details).setVisible(true);
                    }
                });
                postPanel.add(titleLabel);

                JLabel infoLabel = new JLabel("(" + details.getYear() + ") - " + details.getDirector());
                infoLabel.setBounds(90, 10, 400, 60);
                postPanel.add(infoLabel);

                JTextArea reviewArea = new JTextArea(reviewText);
                reviewArea.setLineWrap(true);
                reviewArea.setWrapStyleWord(true);
                reviewArea.setEditable(false);
                JScrollPane reviewScroll = new JScrollPane(reviewArea);
                reviewScroll.setBounds(70, 60, 500, 60);
                postPanel.add(reviewScroll);

                JButton likeButton = new JButton("Like (" + likes + ")");
                likeButton.setBounds(70, 125, 100, 25);
                int reviewId = rs.getInt("id"); // Get it while rs is still open
                likeButton.addActionListener(e -> updateLikeDislike(reviewId, true));
                postPanel.add(likeButton);

                JButton dislikeButton = new JButton("Dislike (" + dislikes + ")");
                dislikeButton.setBounds(180, 125, 100, 25);
                dislikeButton.addActionListener(e -> {
                    try {
                        updateLikeDislike(rs.getInt("id"), false);
                    } catch (SQLException ex) {
                        ex.printStackTrace();
                    }
                });
                postPanel.add(dislikeButton);
                reviewerName = rs.getString("reviewer_name");
                starLabel = new JLabel("Rating: " + stars.toString() + " — by " + reviewerName);
                starLabel.setFont(new Font("San-serif", Font.PLAIN, 14));
                starLabel.setBounds(300, 10, 250, 20); // Adjust width as needed
                postPanel.add(starLabel);


                postPanel.add(starLabel);

                panels.add(postPanel);



            }

        } catch (Exception e) {
            e.printStackTrace();
        }
        return panels;
    }

    private void updateLikeDislike(int logId, boolean isLike) {
        String field = isLike ? "likes" : "dislikes";
        String url = "jdbc:mysql://localhost:3306/letterboxd";
        String dbUser = "root";
        String dbPass = "0000";

        String update = "UPDATE movie_logs SET " + field + " = " + field + " + 1 WHERE id = ?";
        try (Connection conn = DriverManager.getConnection(url, dbUser, dbPass);
             PreparedStatement stmt = conn.prepareStatement(update)) {

            stmt.setInt(1, logId);
            stmt.executeUpdate();

            frame.dispose();
            new Mainpage(userId);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    private String fetchProfilePicPath(int userId) {
        String url = "jdbc:mysql://localhost:3306/letterboxd";
        String dbUser = "root";
        String dbPass = "0000";

        String query = "SELECT profile_pic_path FROM users WHERE id = ?";
        try (Connection conn = DriverManager.getConnection(url, dbUser, dbPass);
             PreparedStatement stmt = conn.prepareStatement(query)) {

            stmt.setInt(1, userId);
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                return rs.getString("profile_pic_path");
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }
    private void chooseAndSaveProfilePicture() {
        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setDialogTitle("Choose Profile Picture");
        int result = fileChooser.showOpenDialog(null);
        if (result == JFileChooser.APPROVE_OPTION) {
            File selectedFile = fileChooser.getSelectedFile();
            String imagePath = ((java.io.File) selectedFile).getAbsolutePath();

            // Save path to DB
            saveProfilePicPathToDB(userId, imagePath);

            // Update profile pic visually
            Image img = new ImageIcon(imagePath).getImage().getScaledInstance(60, 60, Image.SCALE_SMOOTH);
            profilePictureLabel.setIcon(new ImageIcon(img));
        }
    }
    private void saveProfilePicPathToDB(int userId, String path) {
        String url = "jdbc:mysql://localhost:3306/letterboxd";
        String dbUser = "root";
        String dbPass = "0000";

        String updateQuery = "UPDATE users SET profile_pic_path = ? WHERE id = ?";

        try (Connection conn = DriverManager.getConnection(url, dbUser, dbPass);
             PreparedStatement stmt = conn.prepareStatement(updateQuery)) {

            stmt.setString(1, path);
            stmt.setInt(2, userId);
            stmt.executeUpdate();

        } catch (SQLException e) {
            e.printStackTrace();
        }
    }



    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new Mainpage(1));
    }
}