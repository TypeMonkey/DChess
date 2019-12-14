package jg.proj.chess.net.client.uis;

import java.awt.Dimension;
import java.awt.Toolkit;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;

import javax.imageio.ImageIO;
import javax.swing.ImageIcon;
import javax.swing.JDialog;
import javax.swing.JLabel;

public class IntroScreen extends JDialog{
  
  public IntroScreen(String image) throws IOException {
    BufferedImage icon = ImageIO.read(new File(image));
    
    JLabel imageLabel = new JLabel(new ImageIcon(icon));
    
    add(imageLabel);
    
    /**
     * It seems when you open multiple dialogs, setLocationRelativeTo(null) becomes
     * unrealiable when centering components to the screen.
     * 
     * So, you have to do it manually....
     * 
     * Got the code from here: https://stackoverflow.com/questions/213266/how-do-i-center-a-jdialog-on-screen
     * with a bit of modification
     */
    final Toolkit toolkit = Toolkit.getDefaultToolkit();
    final Dimension screenSize = toolkit.getScreenSize();
    final int x = (screenSize.width - icon.getWidth()) / 2;
    final int y = (screenSize.height - icon.getHeight()) / 2;
    setLocation(x, y);
    
    setResizable(false);
    setUndecorated(true);
    pack();
  }
  
}
