package jg.proj.chess.net.client.uis;
import java.awt.BorderLayout;
import java.awt.FlowLayout;

import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.border.EmptyBorder;
import javax.swing.JLabel;
import javax.swing.GroupLayout;
import javax.swing.GroupLayout.Alignment;
import javax.swing.SwingConstants;
import java.awt.Font;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;

import javax.swing.JTextField;
import javax.swing.LayoutStyle.ComponentPlacement;

public class UsernameDialog extends JFrame {

  private final JPanel contentPanel = new JPanel();
  private JTextField userNameField;
  
  private volatile boolean demandedValidUserName;
  private volatile boolean userCancelled;
  private volatile String userName;
  /**
   * Create the dialog.
   */
  public UsernameDialog() {
    final UsernameDialog current = this;
    
    setTitle("DChess Client 1.0");
    setResizable(false);
    setBounds(100, 100, 450, 204);
    getContentPane().setLayout(new BorderLayout());
    contentPanel.setBorder(new EmptyBorder(5, 5, 5, 5));
    getContentPane().add(contentPanel, BorderLayout.CENTER);
    
    addWindowListener(new WindowAdapter() {      
      @Override
      public void windowClosed(WindowEvent e) {
        userCancelled = true;
        current.dispose();
      }
    });
    
    JLabel usernamePost = new JLabel("Enter a Username");
    usernamePost.setFont(new Font("Segoe UI", Font.PLAIN, 30));
    usernamePost.setHorizontalAlignment(SwingConstants.CENTER);
    
    userNameField = new JTextField();
    userNameField.setColumns(10);  
    userNameField.addActionListener(new ActionListener() {
      
      @Override
      public void actionPerformed(ActionEvent e) {
        String input = userNameField.getText();
        if (input.isEmpty() || input.trim().length() == 0) {
          userNameField.setText("ENTER A VALID USERNAME!!!!");
          demandedValidUserName = true;
        }
        else {
          userName = input;
        }
      }
    });
    userNameField.addFocusListener(new FocusListener() {
      
      @Override
      public void focusLost(FocusEvent e) {
        // TODO Auto-generated method stub
        
      }
      
      @Override
      public void focusGained(FocusEvent e) {
        if (demandedValidUserName) {
          userNameField.setText("");
          demandedValidUserName = false;
        }
      }
    });
    
    JButton cancelButton = new JButton("Cancel");
    cancelButton.setFont(new Font("Segoe UI", Font.PLAIN, 15));
    cancelButton.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent e) {
        userCancelled = true;
        current.dispose();
      }
    });
    
    JButton continueButton = new JButton("Continue");
    continueButton.setFont(new Font("Segoe UI", Font.PLAIN, 15));
    continueButton.addActionListener(new ActionListener() {
      
      @Override
      public void actionPerformed(ActionEvent e) {
        String input = userNameField.getText();
        if (input.isEmpty() || input.trim().length() == 0) {
          userNameField.setText("ENTER A VALID USERNAME!!!!");
          demandedValidUserName = true;
        }
        else {
          userName = input;
        }
      }
    });
    
    GroupLayout gl_contentPanel = new GroupLayout(contentPanel);
    gl_contentPanel.setHorizontalGroup(
      gl_contentPanel.createParallelGroup(Alignment.LEADING)
        .addGroup(gl_contentPanel.createSequentialGroup()
          .addContainerGap()
          .addGroup(gl_contentPanel.createParallelGroup(Alignment.LEADING)
            .addComponent(usernamePost, Alignment.TRAILING, GroupLayout.DEFAULT_SIZE, 414, Short.MAX_VALUE)
            .addComponent(userNameField, GroupLayout.DEFAULT_SIZE, 414, Short.MAX_VALUE)
            .addGroup(gl_contentPanel.createSequentialGroup()
              .addComponent(cancelButton, GroupLayout.PREFERRED_SIZE, 144, GroupLayout.PREFERRED_SIZE)
              .addPreferredGap(ComponentPlacement.RELATED, 126, Short.MAX_VALUE)
              .addComponent(continueButton, GroupLayout.PREFERRED_SIZE, 144, GroupLayout.PREFERRED_SIZE)))
          .addContainerGap())
    );
    gl_contentPanel.setVerticalGroup(
      gl_contentPanel.createParallelGroup(Alignment.LEADING)
        .addGroup(gl_contentPanel.createSequentialGroup()
          .addContainerGap()
          .addComponent(usernamePost, GroupLayout.PREFERRED_SIZE, 42, GroupLayout.PREFERRED_SIZE)
          .addPreferredGap(ComponentPlacement.UNRELATED)
          .addComponent(userNameField, GroupLayout.PREFERRED_SIZE, 39, GroupLayout.PREFERRED_SIZE)
          .addPreferredGap(ComponentPlacement.RELATED)
          .addGroup(gl_contentPanel.createParallelGroup(Alignment.LEADING)
            .addComponent(cancelButton, GroupLayout.PREFERRED_SIZE, 39, GroupLayout.PREFERRED_SIZE)
            .addComponent(continueButton, GroupLayout.PREFERRED_SIZE, 39, GroupLayout.PREFERRED_SIZE))
          .addContainerGap(113, Short.MAX_VALUE))
    );
    contentPanel.setLayout(gl_contentPanel);  
  }
  
  public String blockUntilUsername() {
    while(userName == null && !userCancelled);
    return userName;
  }
}
