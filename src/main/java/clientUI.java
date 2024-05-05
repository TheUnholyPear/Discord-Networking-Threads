import javax.swing.*;
import java.awt.*;
import java.io.File;
import java.io.IOException;
import java.io.FileWriter;

public class clientUI extends JFrame {
    public clientUI(String filePath) {
        setTitle(filePath);
        setSize(1920, 1080);

        // Load the image
        ImageIcon imageIcon = new ImageIcon(filePath);

        if (imageIcon.getImageLoadStatus() == MediaTracker.COMPLETE) {
            JLabel label = new JLabel();
            label.setIcon(imageIcon);
            getContentPane().add(label, BorderLayout.CENTER);
        } else {
            // If image loading fails, display an error message
            JOptionPane.showMessageDialog(this, "Failed to load image", "Error", JOptionPane.ERROR_MESSAGE);
        }

        // Display the frame
        setVisible(true);
    }

    public static void displayVideo(String filePath) {
        try {
            String html = "<html><body><video width='640' height='480' controls><source src='" + filePath
                    + "' type='video/mp4'></video></body></html>";
            File temp = File.createTempFile("tempfile", ".html");
            temp.deleteOnExit();

            FileWriter writer = new FileWriter(temp);
            writer.write(html);
            writer.close();

            Desktop.getDesktop().browse(temp.toURI());

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

}