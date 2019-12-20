package jg.proj.chess.net.client.uis;

import java.awt.BorderLayout;
import java.awt.FlowLayout;

import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JPanel;
import javax.swing.border.EmptyBorder;
import javax.swing.JLabel;
import javax.swing.SwingConstants;
import javax.swing.GroupLayout;
import javax.swing.GroupLayout.Alignment;
import java.awt.Font;

public class WaitingDialog extends JDialog {

  private final JPanel contentPanel;
  private final JLabel waitingLabel;


  /**
   * Launch the application.
   */
  public static void main(String[] args) {
    try {
      WaitingDialog dialog = new WaitingDialog("CSESS", "Waiting", "Error", "Succ");
      dialog.setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
      dialog.setVisible(true);
      
      for (int i = 0; i < 10; i++) {
        dialog.waitingLabel.setText("Waiting");
        for (int j = 0; j < 5; j++) {
          dialog.waitingLabel.setText(dialog.waitingLabel.getText()+".");
          Thread.sleep(100);
        }
      }
    } catch (Exception e) {
      e.printStackTrace();
    }
  }

  /**
   * Create the dialog.
   */
  public WaitingDialog(String title, String waitingText, String errorText, String successText) {
    contentPanel = new JPanel();
    waitingLabel = new JLabel(waitingText);
    
    setTitle(title);
    setBounds(100, 100, 496, 209);
    getContentPane().setLayout(new BorderLayout());
    contentPanel.setBorder(new EmptyBorder(5, 5, 5, 5));
    getContentPane().add(contentPanel, BorderLayout.CENTER);
    
    waitingLabel.setFont(new Font("Segoe UI", Font.PLAIN, 40));
    waitingLabel.setHorizontalAlignment(SwingConstants.CENTER);
    GroupLayout gl_contentPanel = new GroupLayout(contentPanel);
    gl_contentPanel.setHorizontalGroup(
      gl_contentPanel.createParallelGroup(Alignment.LEADING)
        .addGroup(gl_contentPanel.createSequentialGroup()
          .addContainerGap()
          .addComponent(waitingLabel, GroupLayout.DEFAULT_SIZE, 498, Short.MAX_VALUE)
          .addContainerGap())
    );
    gl_contentPanel.setVerticalGroup(
      gl_contentPanel.createParallelGroup(Alignment.LEADING)
        .addGroup(gl_contentPanel.createSequentialGroup()
          .addContainerGap()
          .addComponent(waitingLabel, GroupLayout.DEFAULT_SIZE, 135, Short.MAX_VALUE)
          .addContainerGap())
    );
    contentPanel.setLayout(gl_contentPanel);
  }
}
