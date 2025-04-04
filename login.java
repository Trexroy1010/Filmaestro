import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.Random;
import java.util.Scanner;
import javax.imageio.ImageIO;
import org.json.JSONArray;
import org.json.JSONObject;

public class login extends JFrame implements ActionListener {
    private BufferedImage posterImage;
    private final String API_KEY = "4d2aab76554288a53c125c6b3628405e"; // Replace with your TMDB API key

    // UI Components
    private JTextField userIDField = new JTextField();
    private JPasswordField userPasswordField = new JPasswordField();
    private JLabel userIDLabel = new JLabel("User ID: ");
    private JLabel userPasswordLabel = new JLabel("Password: ");
    private JLabel messageLabel = new JLabel();
    private JLabel noid = new JLabel("Don't have an account? Sign up now:");
    private JButton loginButton = new JButton("Login");
    private JButton resetButton = new JButton("Reset");
    private JButton signup = new JButton("Signup");

    public login() {
        setTitle("Filmaestro - Login");
        setSize(600, 900);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);
        setLayout(null); // Use absolute positioning

        // Fetch background image (movie poster)
        String imageUrl = getUpcomingMoviePoster();
        if (imageUrl != null) {
            try {
                posterImage = ImageIO.read(new URL(imageUrl));
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        // Custom JPanel for background
        JPanel backgroundPanel = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                if (posterImage != null) {
                    g.drawImage(posterImage, 0, 0, getWidth(), getHeight(), this);
                }
            }
        };
        backgroundPanel.setBounds(0, 0, 600, 900);
        backgroundPanel.setLayout(null);

        // Style UI Components
        userIDLabel.setBounds(50, 500, 100, 25);
        userIDLabel.setForeground(Color.YELLOW);
        userIDLabel.setFont(new Font("Arial", Font.PLAIN, 24));
        userPasswordLabel.setBounds(50, 550, 200, 25);
        userPasswordLabel.setForeground(Color.YELLOW);
        userPasswordLabel.setFont(new Font("Arial", Font.PLAIN, 24));
        messageLabel.setBounds(180, 650, 250, 35);
        messageLabel.setFont(new Font("Arial", Font.ITALIC, 18));
        messageLabel.setForeground(Color.WHITE);

        userIDField.setBounds(250, 500, 200, 30);
        userPasswordField.setBounds(250, 550, 200, 30);
        userIDField.setBackground(Color.YELLOW);
        userPasswordField.setBackground(Color.YELLOW);


        loginButton.setBounds(150, 600, 100, 35);
        loginButton.addActionListener(this);
        loginButton.setFocusable(false);
        loginButton.setBackground(Color.YELLOW);

        resetButton.setBounds(350, 600, 100, 35);
        resetButton.addActionListener(this);
        resetButton.setFocusable(false);
        resetButton.setBackground(Color.YELLOW);

        noid.setBounds(120, 700, 350, 25);
        noid.setForeground(Color.YELLOW);
        noid.setFont(new Font("Arial", Font.ITALIC, 20));



        signup.setBounds(250, 730, 105, 30);
        signup.setFocusable(false);
        signup.addActionListener(this);
        signup.setBackground(Color.YELLOW);

        // Add components to background panel
        backgroundPanel.add(userIDLabel);
        backgroundPanel.add(userPasswordLabel);
        backgroundPanel.add(messageLabel);
        backgroundPanel.add(userIDField);
        backgroundPanel.add(userPasswordField);
        backgroundPanel.add(loginButton);
        backgroundPanel.add(resetButton);
        backgroundPanel.add(noid);
        backgroundPanel.add(signup);

        // Add panel to frame
        setContentPane(backgroundPanel);
        setVisible(true);
    }

    private String getUpcomingMoviePoster() {
        String apiUrl = "https://api.themoviedb.org/3/movie/upcoming?api_key=" + API_KEY + "&language=en-US&page=1";

        try {
            HttpURLConnection connection = (HttpURLConnection) new URL(apiUrl).openConnection();
            connection.setRequestMethod("GET");

            Scanner scanner = new Scanner(connection.getInputStream());
            StringBuilder jsonResponse = new StringBuilder();
            while (scanner.hasNext()) {
                jsonResponse.append(scanner.nextLine());
            }
            scanner.close();

            JSONObject jsonObject = new JSONObject(jsonResponse.toString());
            JSONArray results = jsonObject.getJSONArray("results");

            if (results.length() > 0) {
                Random rand = new Random();
                int randomIndex = rand.nextInt(results.length());
                JSONObject movie = results.getJSONObject(randomIndex);
                String posterPath = movie.getString("poster_path");
                return "https://image.tmdb.org/t/p/w500" + posterPath;
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        if (e.getSource() == signup) {
            // Signup button clicked (handle separately)
            Signup singup = new Signup();
            this.dispose();
        } else if (e.getSource() == loginButton) {
            // Authenticate login
            String username = userIDField.getText();
            String password = String.valueOf(userPasswordField.getPassword());

            if (authenticate(username, password)) {
                messageLabel.setForeground(Color.GREEN);
                messageLabel.setText("Login successful!");
            } else {
                messageLabel.setForeground(Color.RED);
                messageLabel.setText("Invalid username or password.");
            }
        } else if (e.getSource() == resetButton) {
            // Clear fields
            userIDField.setText("");
            userPasswordField.setText("");
        }
    }

    private boolean authenticate(String username, String password) {
        String url = "jdbc:mysql://localhost:3306/letterboxd"; // Database URL
        String user = "root"; // Database username
        String pass = "0000"; // Database password

        String query = "SELECT * FROM users WHERE name = ? AND password = ?";

        try (Connection conn = DriverManager.getConnection(url, user, pass);
             PreparedStatement stmt = conn.prepareStatement(query)) {

            stmt.setString(1, username);
            stmt.setString(2, password);

            ResultSet rs = stmt.executeQuery();

            return rs.next(); // If there is a result, login is successful

        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    public static void main(String[] args) {
        new login();
    }
}
