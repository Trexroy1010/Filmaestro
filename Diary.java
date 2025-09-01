import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.sql.*;

public class Diary extends JFrame {
    private int userId;
    private JTable table;
    private DefaultTableModel model;

    public Diary(int userId) {
        this.userId = userId;
        setTitle("Diary - Watched Movies");
        setSize(700, 400);
        setLocationRelativeTo(null);
        setLayout(new BorderLayout());

        model = new DefaultTableModel();
        model.setColumnIdentifiers(new String[]{"Movie Title", "Rating", "Review", "Date Watched"});

        table = new JTable(model);
        JScrollPane scrollPane = new JScrollPane(table);
        add(scrollPane, BorderLayout.CENTER);

        fetchAndDisplayLogs();
        setVisible(true);
    }

    private void fetchAndDisplayLogs() {
        String url = "jdbc:mysql://localhost:3306/letterboxd";
        String dbUser = "root";
        String dbPass = "12345";

        String query = "SELECT movie_name, rating, review, created_at FROM movie_logs WHERE user_id = ? ORDER BY created_at DESC";

        try (Connection conn = DriverManager.getConnection(url, dbUser, dbPass);
             PreparedStatement stmt = conn.prepareStatement(query)) {

            stmt.setInt(1, userId);
            ResultSet rs = stmt.executeQuery();

            while (rs.next()) {
                String title = rs.getString("movie_name");
                float rating = rs.getFloat("rating");
                String review = rs.getString("review");
                Date date = rs.getDate("created_at");

                model.addRow(new Object[]{title, rating, review, date});
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
}
