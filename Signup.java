import org.json.JSONArray;
import org.json.JSONObject;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.util.Random;

public class Signup implements ActionListener {
    private final String API_KEY = "4d2aab76554288a53c125c6b3628405e";

    JFrame frame = new JFrame("Filmaestro Signup Page");
    JLabel welcome = new JLabel("Welcome To FILMAESTRO", SwingConstants.CENTER);
    JLabel instruction = new JLabel("Please fill out all fields.", SwingConstants.CENTER);
    JLabel message = new JLabel("", SwingConstants.CENTER);
    JTextArea quoteTextArea = new JTextArea();

    JLabel name = new JLabel("Name:");
    JTextField nameField = new JTextField();

    JLabel password = new JLabel("Password:");
    JPasswordField passField = new JPasswordField();

    JLabel confirmPass = new JLabel("Confirm Password:");
    JPasswordField confirmPassField = new JPasswordField();

    JLabel userEmail = new JLabel("Email:");
    JTextField userMailField = new JTextField();

    JLabel age = new JLabel("Age:");
    Integer[] ages = {13,14,15,16,17,18,19,20,21,22,23,24,25,26,27,28,29,30,31,32,33,34,35,36,37,38,39,40,41,42,43,44,45,46,47,48,49,50,51,52,53,54,55,56,57,58,59,60,61,62,63,64,65,66,67,68,69,70,71,72,73,74,75,76,77,78,79,80,81,82,83,84,85,86,87,88,89,90,91,92,93,94,95,96,97,98,99,100};
    JComboBox<Integer> ageBox = new JComboBox<>(ages);

    JButton createAccount = new JButton("Create Account");
    JButton backButton = new JButton("Back");  // Back button

    Signup() {
        frame.setLayout(null);

        welcome.setBounds(100, 30, 400, 40);
        welcome.setFont(new Font("Arial", Font.BOLD, 24));

        instruction.setBounds(100, 70, 400, 20);
        instruction.setFont(new Font("Arial", Font.PLAIN, 16));

        name.setBounds(100, 150, 150, 25);
        nameField.setBounds(260, 150, 250, 30);

        password.setBounds(100, 200, 150, 25);
        passField.setBounds(260, 200, 250, 30);

        confirmPass.setBounds(100, 250, 150, 25);
        confirmPassField.setBounds(260, 250, 250, 30);

        userEmail.setBounds(100, 300, 150, 25);
        userMailField.setBounds(260, 300, 250, 30);

        age.setBounds(100, 350, 150, 25);
        ageBox.setBounds(260, 350, 100, 30);

        // Buttons
        createAccount.setBounds(120, 450, 160, 40);
        createAccount.addActionListener(this);

        backButton.setBounds(320, 450, 160, 40);
        backButton.addActionListener(e -> {
            frame.dispose();
            new login();  // Open Login page
        });

        message.setBounds(100, 500, 400, 30);
        message.setFont(new Font("Arial", Font.ITALIC, 14));

        quoteTextArea.setText(getRandomMovieQuote());
        quoteTextArea.setFont(new Font("Arial", Font.BOLD, 16));
        quoteTextArea.setForeground(Color.BLACK);
        quoteTextArea.setBackground(new Color(0, 0, 0, 0));
        quoteTextArea.setWrapStyleWord(true);
        quoteTextArea.setLineWrap(true);
        quoteTextArea.setBounds(30, 650, 540, 200);
        quoteTextArea.setEditable(false);

        frame.add(quoteTextArea);
        frame.add(welcome);
        frame.add(instruction);
        frame.add(name);
        frame.add(nameField);
        frame.add(password);
        frame.add(passField);
        frame.add(confirmPass);
        frame.add(confirmPassField);
        frame.add(userEmail);
        frame.add(userMailField);
        frame.add(age);
        frame.add(ageBox);
        frame.add(createAccount);
        frame.add(backButton);
        frame.add(message);

        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setSize(600, 900);
        frame.setLocationRelativeTo(null);
        frame.setVisible(true);
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        String userName = nameField.getText();
        String email = userMailField.getText();
        String password = new String(passField.getPassword());
        String confirmPassword = new String(confirmPassField.getPassword());
        Integer selectedAge = (Integer) ageBox.getSelectedItem();

        if (userName.isEmpty() || email.isEmpty() || password.isEmpty() || confirmPassword.isEmpty() || selectedAge == null) {
            message.setText("All fields must be filled!");
            return;
        }

        if (!password.equals(confirmPassword)) {
            message.setText("Passwords do not match!");
            return;
        }

        // Store user data in the database
        try {
            Connection conn = DriverManager.getConnection("jdbc:mysql://localhost:3306/letterboxd", "root", "0000");
            String query = "INSERT INTO users (name, email, password, age) VALUES (?, ?, ?, ?)";
            PreparedStatement pstmt = conn.prepareStatement(query);
            pstmt.setString(1, userName);
            pstmt.setString(2, email);
            pstmt.setString(3, password); // (Consider hashing this for security)
            pstmt.setInt(4, selectedAge);

            int rowsInserted = pstmt.executeUpdate();
            if (rowsInserted > 0) {
                message.setText("Account created successfully!");
            }
            conn.close();
        } catch (Exception ex) {
            ex.printStackTrace();
            message.setText("Signup failed. Try again.");
        }
    }

    private String getRandomMovieQuote() {
        String apiUrl = "https://api.themoviedb.org/3/movie/popular?api_key=" + API_KEY + "&language=en-US&page=1";
        try {
            HttpURLConnection connection = (HttpURLConnection) new URL(apiUrl).openConnection();
            connection.setRequestMethod("GET");

            InputStreamReader reader = new InputStreamReader(connection.getInputStream());
            StringBuilder response = new StringBuilder();
            int c;
            while ((c = reader.read()) != -1) {
                response.append((char) c);
            }

            JSONObject jsonResponse = new JSONObject(response.toString());
            JSONArray results = jsonResponse.getJSONArray("results");

            if (results.length() > 0) {
                Random rand = new Random();
                int randomIndex = rand.nextInt(results.length());
                JSONObject movie = results.getJSONObject(randomIndex);

                // Try fetching the tagline first
                String tagline = movie.optString("tagline", "").trim();
                String overview = movie.optString("overview", "").trim();

                if (!tagline.isEmpty()) {
                    return tagline; // If tagline exists, use it
                } else if (!overview.isEmpty()) {
                    return overview; // If no tagline, use the overview
                } else {
                    return "A great movie experience awaits you!"; // Default message
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return "A great movie experience awaits you!";
    }


    public static void main(String[] args) {
        new Signup();
    }
}
