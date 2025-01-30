import javax.swing.*;
import java.awt.*;

public class Main {
    public static void main(String[] args) {
        JFrame frame = new JFrame("Empty Frame");
        frame.setSize(1080, 720);
        Toolkit toolkit = Toolkit.getDefaultToolkit();
        Dimension screenDimensions = toolkit.getScreenSize();
        int x = (screenDimensions.width - frame.getWidth()) / 2;
        int y = (screenDimensions.height - frame.getHeight()) / 2;
        frame.setLocation(x, y);

        frame.setLayout(null);
        frame.setVisible(true);
    }
}
