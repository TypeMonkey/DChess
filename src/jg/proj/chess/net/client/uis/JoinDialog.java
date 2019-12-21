package jg.proj.chess.net.client.uis;

import java.awt.BorderLayout;
import java.awt.FlowLayout;

import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JPanel;
import javax.swing.border.EmptyBorder;
import javax.swing.GroupLayout;
import javax.swing.GroupLayout.Alignment;
import javax.swing.JLabel;
import java.awt.Font;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.UUID;

import javax.swing.SwingConstants;
import javax.swing.JTextField;
import javax.swing.LayoutStyle.ComponentPlacement;
import javax.swing.JRadioButton;

public class JoinDialog extends JDialog {

  private final JPanel contentPanel = new JPanel();
  private JTextField idField;
  
  private JoinForm joinForm;
  
  private volatile boolean userFinished;
  private volatile int team;

  /**
   * Create the dialog.
   */
  public JoinDialog() {
    final JoinDialog current = this;
    
    setBounds(100, 100, 508, 292);
    getContentPane().setLayout(new BorderLayout());
    contentPanel.setBorder(new EmptyBorder(5, 5, 5, 5));
    getContentPane().add(contentPanel, BorderLayout.CENTER);
    
    JLabel idLabel = new JLabel("Enter Session ID Number:");
    idLabel.setHorizontalAlignment(SwingConstants.CENTER);
    idLabel.setFont(new Font("Segoe UI", Font.PLAIN, 30));
    
    idField = new JTextField();
    idField.setFont(new Font("Segoe UI", Font.PLAIN, 25));
    idField.setHorizontalAlignment(SwingConstants.CENTER);
    idField.setColumns(10);
    
    JButton cancelButton = new JButton("Cancel");
    cancelButton.setFont(new Font("Segoe UI", Font.PLAIN, 15));
    
    JButton connectButton = new JButton("Connect");
    connectButton.setFont(new Font("Segoe UI", Font.PLAIN, 15));
    
    JRadioButton team1 = new JRadioButton("Team 1");
    team1.setFont(new Font("Segoe UI", Font.PLAIN, 15));
    
    JRadioButton team2 = new JRadioButton("Team 2");
    team2.setFont(new Font("Segoe UI", Font.PLAIN, 15));
    
    JRadioButton randomTeam = new JRadioButton("Random");
    randomTeam.setFont(new Font("Segoe UI", Font.PLAIN, 15));
    GroupLayout gl_contentPanel = new GroupLayout(contentPanel);
    gl_contentPanel.setHorizontalGroup(
      gl_contentPanel.createParallelGroup(Alignment.LEADING)
        .addGroup(gl_contentPanel.createSequentialGroup()
          .addGroup(gl_contentPanel.createParallelGroup(Alignment.LEADING)
            .addGroup(gl_contentPanel.createSequentialGroup()
              .addContainerGap()
              .addComponent(idLabel, GroupLayout.DEFAULT_SIZE, 462, Short.MAX_VALUE))
            .addGroup(gl_contentPanel.createSequentialGroup()
              .addGap(20)
              .addGroup(gl_contentPanel.createParallelGroup(Alignment.LEADING, false)
                .addGroup(Alignment.TRAILING, gl_contentPanel.createSequentialGroup()
                  .addComponent(cancelButton, GroupLayout.PREFERRED_SIZE, 141, GroupLayout.PREFERRED_SIZE)
                  .addPreferredGap(ComponentPlacement.RELATED, GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                  .addComponent(connectButton, GroupLayout.PREFERRED_SIZE, 141, GroupLayout.PREFERRED_SIZE))
                .addComponent(idField, GroupLayout.PREFERRED_SIZE, 440, GroupLayout.PREFERRED_SIZE)
                .addGroup(gl_contentPanel.createSequentialGroup()
                  .addComponent(team1)
                  .addGap(29)
                  .addComponent(team2, GroupLayout.PREFERRED_SIZE, 83, GroupLayout.PREFERRED_SIZE)
                  .addGap(18)
                  .addComponent(randomTeam, GroupLayout.PREFERRED_SIZE, 97, GroupLayout.PREFERRED_SIZE)))))
          .addContainerGap())
    );
    gl_contentPanel.setVerticalGroup(
      gl_contentPanel.createParallelGroup(Alignment.LEADING)
        .addGroup(gl_contentPanel.createSequentialGroup()
          .addGap(26)
          .addComponent(idLabel)
          .addGap(18)
          .addComponent(idField, GroupLayout.PREFERRED_SIZE, 54, GroupLayout.PREFERRED_SIZE)
          .addPreferredGap(ComponentPlacement.UNRELATED)
          .addGroup(gl_contentPanel.createParallelGroup(Alignment.BASELINE)
            .addComponent(team1)
            .addComponent(randomTeam)
            .addComponent(team2))
          .addPreferredGap(ComponentPlacement.RELATED, 17, Short.MAX_VALUE)
          .addGroup(gl_contentPanel.createParallelGroup(Alignment.TRAILING, false)
            .addComponent(cancelButton, GroupLayout.DEFAULT_SIZE, GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
            .addComponent(connectButton, GroupLayout.DEFAULT_SIZE, 46, Short.MAX_VALUE))
          .addContainerGap())
    );
    contentPanel.setLayout(gl_contentPanel);   
    
    addWindowListener(new WindowAdapter() {
      public void windowClosing(WindowEvent e) {
        userFinished = true;
        joinForm = null;
      }
    });
    
    cancelButton.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent e) {
        userFinished = true;
        joinForm = null;
      }
    });
    
    connectButton.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent e) {
        String entered = idField.getText();
        try {
          joinForm = new JoinForm(UUID.fromString(entered), team);
          userFinished = true;
          System.out.println("---JOIN FORM DONE!!! "+userFinished);
        } catch (IllegalArgumentException e2) {
          idField.setText("ENTER A VALID ID!!!");
        }
      }
    });
    
    team1.setSelected(true); //default
    
    JRadioButton [] buttons = {team1, team2, randomTeam};
    
    team1.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent e) {
        team = 1;
        for (JRadioButton jRadioButton : buttons) {
          if (jRadioButton != team1) {
            jRadioButton.setSelected(false);
          }
        }
      }
    });
    
    team2.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent e) {
        team = 2;
        for (JRadioButton jRadioButton : buttons) {
          if (jRadioButton != team2) {
            jRadioButton.setSelected(false);
          }
        }
      }
    });
    
    randomTeam.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent e) {
        team = 3;
        for (JRadioButton jRadioButton : buttons) {
          if (jRadioButton != randomTeam) {
            jRadioButton.setSelected(false);
          }
        }
      }
    });
  }
  
  public JoinForm blockUntilIDAndTeam() {
    while (userFinished == false);
    System.out.println("-----JOIN FORM DONE!!!!");
    return joinForm;
  }
  
  public static class JoinForm {
    private final UUID uuid;
    private final int team;
    
    public JoinForm(UUID uuid, int team) {
      this.uuid = uuid;
      this.team = team;
    }

    public UUID getUuid() {
      return uuid;
    }

    public int getTeam() {
      return team;
    }
  }
}
