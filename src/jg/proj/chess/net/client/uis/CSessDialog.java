package jg.proj.chess.net.client.uis;

import java.awt.BorderLayout;
import java.awt.FlowLayout;

import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JPanel;
import javax.swing.border.EmptyBorder;

import jg.proj.chess.net.ServerRequest;
import jg.proj.chess.net.client.PendingRequest;

import javax.swing.GroupLayout;
import javax.swing.GroupLayout.Alignment;
import javax.swing.JLabel;
import java.awt.Font;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

import javax.swing.SwingConstants;
import javax.swing.JRadioButton;
import javax.swing.LayoutStyle.ComponentPlacement;
import javax.swing.JTextField;

public class CSessDialog extends JDialog {

  private final JPanel contentPanel = new JPanel();
  private JTextField textField;
  private JTextField textField_1;
  
  private JLabel lblWhichTeamWould = new JLabel("Which team would you like to join?");
  private JRadioButton rdbtnTeam = new JRadioButton("Team 1");
  private JRadioButton rdbtnTeam_1 = new JRadioButton("Team 2");
  private JLabel lblEnforcePrisonersDillema = new JLabel("Enforce Prisoner's Dillema?");
  private JRadioButton rdbtnYes = new JRadioButton("Yes");
  private JRadioButton rdbtnNo = new JRadioButton("No");
  private JLabel lblAllowInvalidVotes = new JLabel("Allow Invalid Votes?");
  private JRadioButton radioButton = new JRadioButton("Yes");
  private JRadioButton rdbtnNo_1 = new JRadioButton("No");
  private JLabel lblAllowPlayersTo = new JLabel("Allow Players to Join During Session?");
  private JRadioButton radioButton_1 = new JRadioButton("Yes");
  private JRadioButton radioButton_2 = new JRadioButton("No");
  private JLabel lblHowManySeconds = new JLabel("How many seconds to allow for voting?");
  private JLabel lblMinimumAmountOf = new JLabel("Minimum Amount of Players per Team?");
  private JButton btnNewButton = new JButton("Cancel");
  private JButton btnSubmit = new JButton("Submit");
  
  private volatile boolean userCancelled;
  private volatile boolean finishedForm;
  
  private CSessForm form = new CSessForm();
  
  public static class CSessForm{
    private boolean joinTeamOne = true;
    private boolean enforceDillema = false;
    private boolean allowInvalidVotes = false;
    private boolean allowLateJoins = true;
    private long secondsForVoting = 15;
    private int minPlayerAmnt = 1;
    
    public void setJoinTeamOne(boolean joinTeamOne) {
      this.joinTeamOne = joinTeamOne;
    }
    
    public void setEnforceDillema(boolean enforceDillema) {
      this.enforceDillema = enforceDillema;
    }
    
    public void setAllowInvalidVotes(boolean allowInvalidVotes) {
      this.allowInvalidVotes = allowInvalidVotes;
    }
    
    public void setAllowLateJoins(boolean allowLateJoines) {
      this.allowLateJoins = allowLateJoines;
    }
    
    public void setSecondsForVoting(long secondsForVoting) {
      this.secondsForVoting = secondsForVoting;
    }
    
    public void setMinPlayerAmnt(int minPlayerAmnt) {
      this.minPlayerAmnt = minPlayerAmnt;
    }
    
    public boolean isJoinTeamOne() {
      return joinTeamOne;
    }
    
    public boolean isEnforceDillema() {
      return enforceDillema;
    }
    
    public boolean isAllowInvalidVotes() {
      return allowInvalidVotes;
    }
    
    public boolean isAllowLateJoines() {
      return allowLateJoins;
    }
    
    public long getSecondsForVoting() {
      return secondsForVoting;
    }
    
    public int getMinPlayerAmnt() {
      return minPlayerAmnt;
    }
    
    public PendingRequest asCsessRequest() {
      int teamID = joinTeamOne ? 1 : 2;
      return new PendingRequest(ServerRequest.CSESS, teamID,
          enforceDillema,
          secondsForVoting,
          minPlayerAmnt,
          allowInvalidVotes,
          allowLateJoins);
    }
  }

  /**
   * Launch the application.
   */
  public static void main(String[] args) {
    try {
      CSessDialog dialog = new CSessDialog();
      dialog.setVisible(true);
      
      CSessForm form = dialog.blockUntilFinished();
      System.out.println(form.asCsessRequest());
    } catch (Exception e) {
      e.printStackTrace();
    }
  }
  
  /**
   * Create the dialog.
   */
  public CSessDialog() {
    final JDialog current = this;
    
    setTitle("Create A Session");
    setBounds(100, 100, 444, 515);
    setResizable(true);
    addWindowListener(new WindowAdapter() {
      @Override
      public void windowClosed(WindowEvent e) {
        userCancelled = true;
        super.windowClosed(e);
      }
    });
    setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
    
    getContentPane().setLayout(new BorderLayout());
    contentPanel.setBorder(new EmptyBorder(5, 5, 5, 5));
    getContentPane().add(contentPanel, BorderLayout.CENTER);
    
    lblWhichTeamWould.setHorizontalAlignment(SwingConstants.CENTER);
    lblWhichTeamWould.setFont(new Font("Segoe UI", Font.PLAIN, 17));
    
    rdbtnTeam.setSelected(true);
    rdbtnTeam.setFont(new Font("Segoe UI", Font.BOLD, 15));
    rdbtnTeam.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent e) {
        if (rdbtnTeam_1.isSelected()) {
          rdbtnTeam_1.setSelected(false);
        }
        form.setJoinTeamOne(true);
      }
    });
    
    rdbtnTeam_1.setFont(new Font("Segoe UI", Font.BOLD, 15));
    rdbtnTeam_1.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent e) {
        if (rdbtnTeam.isSelected()) {
          rdbtnTeam.setSelected(false);
        }
        form.setJoinTeamOne(false);
      }
    });
    
    lblEnforcePrisonersDillema.setHorizontalAlignment(SwingConstants.CENTER);
    lblEnforcePrisonersDillema.setFont(new Font("Segoe UI", Font.PLAIN, 17));
    
    lblEnforcePrisonersDillema.setToolTipText("If enforced, this session will disallow communication between players (be it among or between teams)");
    
    rdbtnYes.setFont(new Font("Segoe UI", Font.BOLD, 15));
    rdbtnYes.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent e) {
        if (rdbtnNo.isSelected()) {
          rdbtnNo.setSelected(false);
        }
        form.setEnforceDillema(true);
      }
    });
    
    rdbtnNo.setFont(new Font("Segoe UI", Font.BOLD, 15));
    rdbtnNo.setSelected(true);
    rdbtnNo.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent e) {
        if (rdbtnYes.isSelected()) {
          rdbtnYes.setSelected(false);
        }
        form.setEnforceDillema(false);
      }
    });
    
    lblAllowInvalidVotes.setHorizontalAlignment(SwingConstants.CENTER);
    lblAllowInvalidVotes.setFont(new Font("Segoe UI", Font.PLAIN, 17));
    String allowInvToolTip = "<html>If allowed, the server will not filter invalid votes <br>"+
                                   "from being counted at the end of the voting process. <br>"+
                                   "This means that a team can agree on an invalid move at the end <br>"+
                                   "of the voting duration. </html>";
    
    lblAllowInvalidVotes.setToolTipText(allowInvToolTip);
    
    radioButton.setFont(new Font("Segoe UI", Font.BOLD, 15));
    radioButton.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent e) {
        if (rdbtnNo_1.isSelected()) {
          rdbtnNo_1.setSelected(false);
        }
        form.setAllowInvalidVotes(true);
      }
    });
    
    rdbtnNo_1.setFont(new Font("Segoe UI", Font.BOLD, 15));
    rdbtnNo_1.setSelected(true);
    rdbtnNo_1.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent e) {
        if (radioButton.isSelected()) {
          radioButton.setSelected(false);
        }
        form.setAllowInvalidVotes(false);
      }
    });
    
    lblAllowPlayersTo.setHorizontalAlignment(SwingConstants.CENTER);
    lblAllowPlayersTo.setFont(new Font("Segoe UI", Font.PLAIN, 17));
    lblAllowPlayersTo.setToolTipText("Allows layers to join mid-game.");
    
    radioButton_1.setFont(new Font("Segoe UI", Font.BOLD, 15));
    radioButton_1.setSelected(true);
    radioButton_1.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent e) {
        if (radioButton_2.isSelected()) {
          radioButton_2.setSelected(false);
        }
        form.setAllowLateJoins(true);
      }
    });
    
    radioButton_2.setFont(new Font("Segoe UI", Font.BOLD, 15));
    radioButton_2.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent e) {
        if (radioButton_1.isSelected()) {
          radioButton_1.setSelected(false);
        }
        form.setAllowLateJoins(false);
      }
    });
    
    lblHowManySeconds.setHorizontalAlignment(SwingConstants.CENTER);
    lblHowManySeconds.setFont(new Font("Segoe UI", Font.PLAIN, 17));
    
    textField = new JTextField();
    textField.setColumns(10);
    
    lblMinimumAmountOf.setHorizontalAlignment(SwingConstants.CENTER);
    lblMinimumAmountOf.setFont(new Font("Segoe UI", Font.PLAIN, 17));
    
    textField_1 = new JTextField();
    textField_1.setColumns(10);
    
    btnNewButton.setFont(new Font("Segoe UI", Font.BOLD, 17));
    btnNewButton.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent e) {
        userCancelled = true;
        current.dispose();
      }
    });
    
    btnSubmit.setFont(new Font("Segoe UI", Font.BOLD, 17));
    btnSubmit.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent e) {
        String secondsForVoting = textField.getText();
        String minTeamCnt = textField_1.getText();
        
        boolean secondCorrect = false;
        boolean teamCntCorrect = false;
        
        try {
          long seconds = Long.parseLong(secondsForVoting);
          if (seconds >= 15) {
            form.setSecondsForVoting(seconds);
            secondCorrect = true;
          }
          else {
            throw new NumberFormatException();
          }
        } catch (NumberFormatException e2) {
          textField.setText("MUST BE AT LEAST 15 SECONDS!");
          secondCorrect = false;
        }
        
        try {
          int teamCnt = Integer.parseInt(minTeamCnt);
          if (teamCnt >= 1) {
            form.setMinPlayerAmnt(teamCnt);
            teamCntCorrect = true;
          }
          else {
            throw new NumberFormatException();
          }
        } catch (NumberFormatException e2) {
          textField_1.setText("MUST BE AT LEAST 1!");
          teamCntCorrect = false;
        }
        
        finishedForm = teamCntCorrect && secondCorrect;
        if (finishedForm) {
          current.dispose();
        }
        System.out.println(finishedForm ? "finished" : "not finished");
      }
    });
    
    
    GroupLayout gl_contentPanel = new GroupLayout(contentPanel);
    gl_contentPanel.setHorizontalGroup(
      gl_contentPanel.createParallelGroup(Alignment.TRAILING)
        .addGroup(gl_contentPanel.createSequentialGroup()
          .addContainerGap()
          .addComponent(lblWhichTeamWould, GroupLayout.DEFAULT_SIZE, 398, Short.MAX_VALUE)
          .addContainerGap())
        .addGroup(gl_contentPanel.createSequentialGroup()
          .addGap(76)
          .addComponent(rdbtnTeam)
          .addPreferredGap(ComponentPlacement.RELATED, 118, Short.MAX_VALUE)
          .addComponent(rdbtnTeam_1, GroupLayout.PREFERRED_SIZE, 77, GroupLayout.PREFERRED_SIZE)
          .addGap(70))
        .addGroup(gl_contentPanel.createSequentialGroup()
          .addContainerGap()
          .addComponent(lblEnforcePrisonersDillema, GroupLayout.PREFERRED_SIZE, 398, GroupLayout.PREFERRED_SIZE)
          .addContainerGap(GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        .addGroup(gl_contentPanel.createSequentialGroup()
          .addGap(76)
          .addComponent(rdbtnYes, GroupLayout.PREFERRED_SIZE, 77, GroupLayout.PREFERRED_SIZE)
          .addPreferredGap(ComponentPlacement.RELATED, 119, Short.MAX_VALUE)
          .addComponent(rdbtnNo, GroupLayout.PREFERRED_SIZE, 77, GroupLayout.PREFERRED_SIZE)
          .addGap(69))
        .addGroup(gl_contentPanel.createSequentialGroup()
          .addContainerGap()
          .addComponent(lblAllowInvalidVotes, GroupLayout.PREFERRED_SIZE, 398, GroupLayout.PREFERRED_SIZE)
          .addContainerGap(GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        .addGroup(gl_contentPanel.createSequentialGroup()
          .addGap(76)
          .addComponent(radioButton, GroupLayout.PREFERRED_SIZE, 77, GroupLayout.PREFERRED_SIZE)
          .addPreferredGap(ComponentPlacement.RELATED, 117, Short.MAX_VALUE)
          .addComponent(rdbtnNo_1, GroupLayout.PREFERRED_SIZE, 77, GroupLayout.PREFERRED_SIZE)
          .addGap(71))
        .addGroup(gl_contentPanel.createSequentialGroup()
          .addContainerGap()
          .addComponent(lblAllowPlayersTo, GroupLayout.PREFERRED_SIZE, 398, GroupLayout.PREFERRED_SIZE)
          .addContainerGap(GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        .addGroup(gl_contentPanel.createSequentialGroup()
          .addGap(74)
          .addComponent(radioButton_1, GroupLayout.PREFERRED_SIZE, 77, GroupLayout.PREFERRED_SIZE)
          .addPreferredGap(ComponentPlacement.RELATED, 115, Short.MAX_VALUE)
          .addComponent(radioButton_2, GroupLayout.PREFERRED_SIZE, 77, GroupLayout.PREFERRED_SIZE)
          .addGap(75))
        .addGroup(gl_contentPanel.createSequentialGroup()
          .addContainerGap()
          .addComponent(lblHowManySeconds, GroupLayout.PREFERRED_SIZE, 398, GroupLayout.PREFERRED_SIZE)
          .addContainerGap(GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        .addGroup(gl_contentPanel.createSequentialGroup()
          .addGap(76)
          .addComponent(textField, GroupLayout.PREFERRED_SIZE, 250, GroupLayout.PREFERRED_SIZE)
          .addContainerGap(92, Short.MAX_VALUE))
        .addGroup(Alignment.LEADING, gl_contentPanel.createSequentialGroup()
          .addContainerGap()
          .addComponent(lblMinimumAmountOf, GroupLayout.PREFERRED_SIZE, 398, GroupLayout.PREFERRED_SIZE)
          .addContainerGap(GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        .addGroup(Alignment.LEADING, gl_contentPanel.createSequentialGroup()
          .addGap(75)
          .addComponent(textField_1, GroupLayout.PREFERRED_SIZE, 250, GroupLayout.PREFERRED_SIZE)
          .addContainerGap(93, Short.MAX_VALUE))
        .addGroup(Alignment.LEADING, gl_contentPanel.createSequentialGroup()
          .addComponent(btnNewButton, GroupLayout.PREFERRED_SIZE, 127, GroupLayout.PREFERRED_SIZE)
          .addPreferredGap(ComponentPlacement.RELATED, 164, Short.MAX_VALUE)
          .addComponent(btnSubmit, GroupLayout.PREFERRED_SIZE, 127, GroupLayout.PREFERRED_SIZE))
    );
    gl_contentPanel.setVerticalGroup(
      gl_contentPanel.createParallelGroup(Alignment.LEADING)
        .addGroup(gl_contentPanel.createSequentialGroup()
          .addContainerGap()
          .addComponent(lblWhichTeamWould, GroupLayout.PREFERRED_SIZE, 32, GroupLayout.PREFERRED_SIZE)
          .addPreferredGap(ComponentPlacement.RELATED)
          .addGroup(gl_contentPanel.createParallelGroup(Alignment.BASELINE)
            .addComponent(rdbtnTeam_1, GroupLayout.PREFERRED_SIZE, 29, GroupLayout.PREFERRED_SIZE)
            .addComponent(rdbtnTeam))
          .addPreferredGap(ComponentPlacement.RELATED)
          .addComponent(lblEnforcePrisonersDillema, GroupLayout.PREFERRED_SIZE, 32, GroupLayout.PREFERRED_SIZE)
          .addPreferredGap(ComponentPlacement.RELATED)
          .addGroup(gl_contentPanel.createParallelGroup(Alignment.TRAILING)
            .addComponent(rdbtnYes, GroupLayout.PREFERRED_SIZE, 29, GroupLayout.PREFERRED_SIZE)
            .addComponent(rdbtnNo, GroupLayout.PREFERRED_SIZE, 29, GroupLayout.PREFERRED_SIZE))
          .addPreferredGap(ComponentPlacement.RELATED)
          .addComponent(lblAllowInvalidVotes, GroupLayout.PREFERRED_SIZE, 32, GroupLayout.PREFERRED_SIZE)
          .addPreferredGap(ComponentPlacement.RELATED)
          .addGroup(gl_contentPanel.createParallelGroup(Alignment.TRAILING)
            .addComponent(radioButton, GroupLayout.PREFERRED_SIZE, 29, GroupLayout.PREFERRED_SIZE)
            .addComponent(rdbtnNo_1, GroupLayout.PREFERRED_SIZE, 29, GroupLayout.PREFERRED_SIZE))
          .addPreferredGap(ComponentPlacement.RELATED)
          .addComponent(lblAllowPlayersTo, GroupLayout.PREFERRED_SIZE, 32, GroupLayout.PREFERRED_SIZE)
          .addPreferredGap(ComponentPlacement.RELATED)
          .addGroup(gl_contentPanel.createParallelGroup(Alignment.TRAILING)
            .addComponent(radioButton_1, GroupLayout.PREFERRED_SIZE, 29, GroupLayout.PREFERRED_SIZE)
            .addComponent(radioButton_2, GroupLayout.PREFERRED_SIZE, 29, GroupLayout.PREFERRED_SIZE))
          .addPreferredGap(ComponentPlacement.RELATED)
          .addComponent(lblHowManySeconds, GroupLayout.PREFERRED_SIZE, 32, GroupLayout.PREFERRED_SIZE)
          .addPreferredGap(ComponentPlacement.RELATED)
          .addComponent(textField, GroupLayout.PREFERRED_SIZE, 30, GroupLayout.PREFERRED_SIZE)
          .addPreferredGap(ComponentPlacement.RELATED)
          .addComponent(lblMinimumAmountOf, GroupLayout.PREFERRED_SIZE, 32, GroupLayout.PREFERRED_SIZE)
          .addPreferredGap(ComponentPlacement.RELATED)
          .addComponent(textField_1, GroupLayout.PREFERRED_SIZE, 30, GroupLayout.PREFERRED_SIZE)
          .addPreferredGap(ComponentPlacement.UNRELATED)
          .addGroup(gl_contentPanel.createParallelGroup(Alignment.TRAILING)
            .addComponent(btnNewButton, GroupLayout.DEFAULT_SIZE, 42, Short.MAX_VALUE)
            .addComponent(btnSubmit, GroupLayout.PREFERRED_SIZE, 42, GroupLayout.PREFERRED_SIZE)))
    );
    contentPanel.setLayout(gl_contentPanel);
  }
  
  /**
   * Returns the filled out CSessForm
   * @return the csess form or null if user cancelled
   */
  public CSessForm blockUntilFinished() {
    while (!finishedForm && !userCancelled);
    if (userCancelled) {
      return null;
    }
    return form;
  }
}
