import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;

import javax.imageio.ImageIO;
import javax.swing.ImageIcon;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.OverlayLayout;

public class ImageTest {
  
  public static void main(String[] args) throws IOException {
    
    BufferedImage icon = ImageIO.read(new File("imgs/intro/wigand.jpg"));
    System.out.println(icon.getHeight());
    System.out.println(icon.getWidth());
    
    
    JFrame jFrame = new JFrame();
    jFrame.getContentPane().setLayout(new OverlayLayout(jFrame.getContentPane()));
    
    JLabel background = new JLabel(new ImageIcon(icon));
    
    jFrame.getContentPane().add(background);
    
    JLabel label1 = new JLabel("Easy Way");
    label1.setHorizontalTextPosition(JLabel.CENTER);
    label1.setVerticalTextPosition(JLabel.CENTER);
    label1.setForeground(Color.BLACK);
    
    jFrame.getContentPane().add(label1);
        
    jFrame.setUndecorated(true); // Remove title bar
    jFrame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
    jFrame.pack();

    jFrame.setLocationRelativeTo(null);
        
    jFrame.setVisible(true);
  }
}
