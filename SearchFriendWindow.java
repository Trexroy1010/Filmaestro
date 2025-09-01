import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.sql.*;

public class SearchFriendWindow extends JFrame {
    public SearchFriendWindow(String email) {
        setTitle("Friend Profile");
        setSize(400, 600);
        setLocationRelativeTo(null);
        setLayout(null);
        getContentPane().setBackground(Color.DARK_GRAY);

        // --- Fetch data ---
        int userId = -1;
        String[] favoriteFilms = new String[4];
        int watched = 0, watchlist = 0, reviews = 0;
        String latestEntries = "";

        try (Connection conn = DriverManager.getConnection("jdbc:mysql://localhost:3306/letterboxd", "root", "12345")) {
            // Get userId from email
            PreparedStatement st = conn.prepareStatement("SELECT id FROM users WHERE email = ?");
            st.setString(1, email);
            ResultSet rs = st.executeQuery();
            if (rs.next()) userId = rs.getInt("id");

            if (userId == -1) throw new Exception("User not found");

            // Get favorite films
            st = conn.prepareStatement("SELECT movie_title FROM favorites WHERE user_id = ? LIMIT 4");
            st.setInt(1, userId);
            rs = st.executeQuery();
            int i = 0;
            while (rs.next() && i < 4) favoriteFilms[i++] = rs.getString("movie_title");

            // Get stats
            st = conn.prepareStatement("SELECT COUNT(*) FROM movie_logs WHERE user_id = ?");
            st.setInt(1, userId);
            rs = st.executeQuery();
            if (rs.next()) watched = rs.getInt(1);

            st = conn.prepareStatement("SELECT COUNT(*) FROM watchlist WHERE user_id = ?");
            st.setInt(1, userId);
            rs = st.executeQuery();
            if (rs.next()) watchlist = rs.getInt(1);

            st = conn.prepareStatement("SELECT COUNT(*) FROM movie_logs WHERE user_id = ? AND review IS NOT NULL AND review != ''");
            st.setInt(1, userId);
            rs = st.executeQuery();
            if (rs.next()) reviews = rs.getInt(1);

            // Get latest reviews
            st = conn.prepareStatement("SELECT movie_name, review FROM movie_logs WHERE user_id = ? AND review IS NOT NULL AND review != '' ORDER BY id DESC LIMIT 2");
            st.setInt(1, userId);
            rs = st.executeQuery();
            while (rs.next()) {
                latestEntries += rs.getString("movie_name") + ":\n" + rs.getString("review") + "\n\n";
            }

        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, "User not found or error loading profile.");
            e.printStackTrace();
            dispose();
            return;
        }

        // --- Display ---
        JLabel favLabel = new JLabel("Favorite Films:");
        favLabel.setBounds(30, 20, 300, 25);
        favLabel.setForeground(Color.WHITE);
        add(favLabel);

        for (int i = 0; i < 4; i++) {
            JLabel lbl = new JLabel((favoriteFilms[i] != null ? favoriteFilms[i] : "-"));
            lbl.setBounds(30, 50 + (i * 30), 300, 25);
            lbl.setForeground(Color.LIGHT_GRAY);
            add(lbl);
        }

        JLabel stats = new JLabel("Stats:");
        stats.setBounds(30, 190, 300, 25);
        stats.setForeground(Color.WHITE);
        add(stats);

        JLabel statValues = new JLabel("Watched: " + watched + ", Watchlist: " + watchlist + ", Reviews: " + reviews);
        statValues.setBounds(30, 220, 300, 25);
        statValues.setForeground(Color.LIGHT_GRAY);
        add(statValues);

        JLabel latest = new JLabel("Latest Entries:");
        latest.setBounds(30, 260, 300, 25);
        latest.setForeground(Color.WHITE);
        add(latest);

        JTextArea latestText = new JTextArea(latestEntries);
        latestText.setBounds(30, 290, 320, 200);
        latestText.setWrapStyleWord(true);
        latestText.setLineWrap(true);
        latestText.setEditable(false);
        latestText.setBackground(Color.LIGHT_GRAY);
        add(latestText);

        setVisible(true);
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            String email = JOptionPane.showInputDialog("Enter email of friend to search:");
            if (email != null && !email.trim().isEmpty()) new SearchFriendWindow(email);
        });
    }
}
