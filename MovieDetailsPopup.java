import javax.swing.*;
import java.awt.*;

public class MovieDetailsPopup extends JDialog {
    public MovieDetailsPopup(MovieDetails details) {
        setTitle("Movie Details");
        setSize(400, 600);
        setLocationRelativeTo(null);
        setLayout(new BorderLayout(10, 10));

        JLabel titleLabel = new JLabel(details.getTitle() + " (" + details.getYear() + ")");
        titleLabel.setFont(new Font("SansSerif", Font.BOLD, 18));
        JLabel directorLabel = new JLabel("Director: " + details.getDirector());

        JTextArea summaryArea = new JTextArea(details.getSummary());
        summaryArea.setWrapStyleWord(true);
        summaryArea.setLineWrap(true);
        summaryArea.setEditable(false);

        JScrollPane summaryScrollPane = new JScrollPane(summaryArea);
        summaryScrollPane.setPreferredSize(new Dimension(380, 200));

        ImageIcon posterIcon = null;
        try {
            posterIcon = new ImageIcon(new java.net.URL(details.getPosterUrl()));
            Image img = posterIcon.getImage().getScaledInstance(200,300, Image.SCALE_SMOOTH);
            posterIcon = new ImageIcon(img);
        } catch (Exception e) {
            e.printStackTrace();
        }

        JLabel posterLabel = new JLabel(posterIcon);
        posterLabel.setHorizontalAlignment(JLabel.CENTER);

        // Top panel for title and director
        JPanel topPanel = new JPanel(new GridLayout(2, 1));
        topPanel.add(titleLabel);
        topPanel.add(directorLabel);

        // Center panel for poster and summary stacked vertically
        JPanel centerPanel = new JPanel();
        centerPanel.setLayout(new BoxLayout(centerPanel, BoxLayout.Y_AXIS));
        centerPanel.add(posterLabel);
        centerPanel.add(Box.createRigidArea(new Dimension(0, 10)));  // spacing
        centerPanel.add(summaryScrollPane);

        add(topPanel, BorderLayout.NORTH);
        add(centerPanel, BorderLayout.CENTER);
    }
}
