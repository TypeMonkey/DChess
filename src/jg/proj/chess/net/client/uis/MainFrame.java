package jg.proj.chess.net.client.uis;
import java.awt.Color;
import java.awt.EventQueue;

import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.border.EmptyBorder;
import javax.swing.text.DefaultCaret;

import jg.proj.chess.core.Board;
import jg.proj.chess.core.DefaultBoardPreparer;
import jg.proj.chess.net.ServerRequest;
import jg.proj.chess.net.client.GameClient;
import jg.proj.chess.net.client.PendingRequest;
import jg.proj.chess.net.client.Reactor;
import jg.proj.chess.net.client.RequestFuture;
import jg.proj.chess.net.client.SessionInfo;
import jg.proj.chess.net.client.RequestFuture.Status;
import jg.proj.chess.net.client.uis.CSessDialog.CSessForm;
import jg.proj.chess.net.client.uis.JoinDialog.JoinForm;
import jg.proj.chess.net.server.SessionRules;

import javax.swing.AbstractAction;
import javax.swing.GroupLayout;
import javax.swing.GroupLayout.Alignment;
import javax.swing.JTextPane;
import javax.swing.KeyStroke;
import javax.swing.LayoutStyle.ComponentPlacement;
import javax.swing.ScrollPaneConstants;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JLabel;
import java.awt.Font;
import java.awt.GraphicsEnvironment;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import javax.swing.SwingConstants;
import javax.swing.UIManager;
import javax.swing.border.BevelBorder;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JEditorPane;
import javax.swing.JMenuBar;
import javax.swing.JMenu;
import javax.swing.JMenuItem;


public class MainFrame extends JFrame implements Reactor{

  private final GameClient client;
  private final BoardDisplay display;

  private JPanel contentPane = new JPanel();

  /**
   * Displays session chat messages
   */
  private JTextArea chatList = new JTextArea();

  /**
   * Displays the player list of team 2
   */
  private JTextArea t2PlayList = new JTextArea();

  /**
   * Displays the player list of team 1
   */
  private JTextArea t1PlayList = new JTextArea();

  /**
   * Where the user enters commands / messages
   */
  private JTextArea textArea = new JTextArea();  

  private JScrollPane chatScrollPane = new JScrollPane();

  /**
   * The main display for the game. Where the board is displayed
   */
  private JTextPane boardDisplay = new JTextPane();
  private JButton btnSend = new JButton("Send");  
  private JButton btnClear = new JButton("Clear");
  private JScrollPane scrollPane = new JScrollPane();  
  private JScrollPane teamOnePanel = new JScrollPane();  
  private JScrollPane teamTwoPanel = new JScrollPane();


  private JLabel teamTwoPost = new JLabel("Team Two");
  private JLabel teamOnePost = new JLabel("Team One");
  private JMenuBar menuBar = new JMenuBar();
  private JMenu connectMenuItem = new JMenu("Connect");
  private final JMenuItem createPortal = new JMenuItem("Create A Session");
  private final JMenuItem joinPortal = new JMenuItem("Join A Session");


  /**
   * Launch the application.
   */
  /*
  public static void main(String[] args) throws Exception{
    MainFrame frame = new MainFrame();
    frame.setLocationRelativeTo(null);



    EventQueue.invokeLater(new Runnable() {
      public void run() {
        try {
          frame.setVisible(true);
        } catch (Exception e) {
          e.printStackTrace();
        }
      }
    });

    final String html = "%s";
    String xString = String.format(html, "Lorem ipsum dolor sit amet, consectetur adipiscing elit. "
        + "Aenean eu nulla urna. Donec sit amet risus nisl, a porta enim. Quisque luctus, "
        + "ligula eu scelerisque gravida, tellus quam vestibulum urna, ut aliquet sapien "
        + "purus sed erat. Pellentesque consequat vehicula magna, eu aliquam magna interdum "
        + "porttitor. Class aptent taciti sociosqu ad litora torquent per conubia nostra, "
        + "per inceptos himenaeos. Sed sollicitudin sapien non leo tempus lobortis. Morbi "
        + "semper auctor ipsum, a semper quam elementum a. Aliquam eget sem metus."
        + "semper auctor ipsum, a semper quam elementum a. Aliquam eget sem metus.");

    System.out.println(xString);
    //String messages = "<font color=green>this will be green</font><br>";
    frame.chatList.setText(xString+"\n");

    Board board = new Board(8, 8);
    board.initialize(new DefaultBoardPreparer());
    frame.updateBoard(board.parsableToString());

    while (true) {
      frame.chatList.append("-----------------------\n");
      frame.chatList.append(xString+"\n");


      frame.t1PlayList.append("______________________\n");
      frame.t1PlayList.append(xString+"\n");

      frame.t2PlayList.append("______________________\n");
      frame.t2PlayList.append(xString+"\n");
      Thread.sleep(1000);
    }

  }
   */

  /**
   * Create the frame.
   */
  public MainFrame(GameClient gameClient) {
    this.client = gameClient;
    this.display = new BoardDisplay("", "");

    setTitle("DChess Client 1.0");
    setResizable(false);
    setLocationRelativeTo(null);
    addWindowListener(new WindowAdapter() {
      @Override
      public void windowClosing(WindowEvent e) {
        client.submitRequest(new RequestFuture(new PendingRequest(ServerRequest.DISC), Reactor.BLANK_REACTOR));
        try {
          client.disconnect();
        } catch (InterruptedException e1) {
          e1.printStackTrace();
        }
        super.windowClosing(e);
      }
    });
    setBounds(100, 100, 1031, 718);

    setJMenuBar(menuBar);

    menuBar.add(connectMenuItem);

    connectMenuItem.add(createPortal);

    connectMenuItem.add(joinPortal);
    setContentPane(contentPane);

    initComponents();
    initLayout();
  }

  private void initComponents() {
    contentPane.setBorder(new EmptyBorder(5, 5, 5, 5));

    boardDisplay.setEditable(false);
    boardDisplay.setFont(new Font("Consolas", Font.PLAIN, 20));

    teamOnePost.setHorizontalAlignment(SwingConstants.CENTER);
    teamOnePost.setFont(new Font("Segoe UI", Font.PLAIN, 25));

    teamTwoPost.setHorizontalAlignment(SwingConstants.CENTER);
    teamTwoPost.setFont(new Font("Segoe UI", Font.PLAIN, 25));

    teamOnePanel.setBorder(new BevelBorder(BevelBorder.LOWERED, null, null, null, null));    
    teamTwoPanel.setBorder(new BevelBorder(BevelBorder.LOWERED, null, null, null, null));

    btnSend.setFont(new Font("Segoe UI", Font.BOLD, 18)); 
    
    final MainFrame mainFrame = this;
    btnSend.addActionListener(new ActionListener() {

      @Override
      public void actionPerformed(ActionEvent e) {
        String inputText = textArea.getText();

        client.parseInput(inputText, mainFrame);
        textArea.setText("");
      }
    });

    btnClear.setFont(new Font("Segoe UI", Font.BOLD, 18));
    btnClear.addActionListener(new ActionListener() {

      @Override
      public void actionPerformed(ActionEvent e) {
        textArea.setText("");
      }
    });

    chatScrollPane.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);

    createPortal.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent e) {
        CSessDialog dialog = new CSessDialog();
        dialog.setModal(true);       
        dialog.setVisible(true);

        CSessForm form = dialog.blockUntilFinished();
        dialog.dispose();

        System.out.println("--CSESS GAVE: "+form);
        if (form != null) {
          client.submitRequest(new RequestFuture(form.asCsessRequest(), mainFrame));
        }        
      }
    });

    joinPortal.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent e) {
        JoinDialog dialog = new JoinDialog();        
        dialog.setModal(true);
        dialog.setVisible(true);

        System.out.println("---OPENED JOIN FORM!!!");
        JoinForm form = dialog.getForm();
        dialog.dispose();
        
        System.out.println("---ATTEMPTING TO JOIN!!!! "+form.getUuid().toString());
        if (form != null) {
          client.submitRequest(new RequestFuture(new PendingRequest(ServerRequest.JOIN, form.getUuid().toString(), form.getTeam()), mainFrame));
        }
      }
    });

    chatList.setWrapStyleWord(true);
    chatList.setLineWrap(true);
    chatList.setEditable(false);
    chatList.setFocusable(false);
    chatList.setBackground(UIManager.getColor("Label.background"));
    chatList.setFont(UIManager.getFont("Label.font"));
    chatList.setBorder(UIManager.getBorder("Label.border"));
    chatList.setBackground(Color.WHITE);

    t1PlayList.setWrapStyleWord(true);
    t1PlayList.setLineWrap(true);
    t1PlayList.setEditable(false);
    t1PlayList.setFocusable(false);
    t1PlayList.setBackground(UIManager.getColor("Label.background"));
    t1PlayList.setFont(UIManager.getFont("Label.font"));
    t1PlayList.setBorder(UIManager.getBorder("Label.border"));
    t1PlayList.setBackground(Color.WHITE);

    t2PlayList.setWrapStyleWord(true);
    t2PlayList.setLineWrap(true);
    t2PlayList.setEditable(false);
    t2PlayList.setFocusable(false);
    t2PlayList.setBackground(UIManager.getColor("Label.background"));
    t2PlayList.setFont(UIManager.getFont("Label.font"));
    t2PlayList.setBorder(UIManager.getBorder("Label.border"));
    t2PlayList.setBackground(Color.WHITE);

    textArea.setWrapStyleWord(true);
    textArea.setLineWrap(true);
    textArea.setBackground(UIManager.getColor("Label.background"));
    textArea.setFont(UIManager.getFont("Label.font"));
    textArea.setBorder(UIManager.getBorder("Label.border"));
    textArea.setBackground(Color.WHITE);
    
    /*
    textArea.addFocusListener(new FocusListener() {

      @Override
      public void focusLost(FocusEvent e) {}

      @Override
      public void focusGained(FocusEvent e) {
        if (threwCommandError) {
          threwCommandError = false;
          textArea.setForeground(Color.BLACK);
          textArea.setText("");
        }
      }
    });
    */

    //setup enter button listener for textArea
    KeyStroke stroke = KeyStroke.getKeyStroke("ENTER");
    Object action = textArea.getInputMap(JComponent.WHEN_FOCUSED).get(stroke);
    textArea.getActionMap().put(action, new AbstractAction() {

      @Override
      public void actionPerformed(ActionEvent e) {
        btnSend.doClick();
      }
    });

    //DefaultCaret caret = (DefaultCaret) chatList.getCaret();
    //caret.setUpdatePolicy(DefaultCaret.ALWAYS_UPDATE);

    boardDisplay.setContentType("text/html");
    boardDisplay.putClientProperty(JEditorPane.HONOR_DISPLAY_PROPERTIES, Boolean.TRUE);   
  }

  private void initLayout() {

    GroupLayout gl_contentPane = new GroupLayout(contentPane);
    gl_contentPane.setHorizontalGroup(
        gl_contentPane.createParallelGroup(Alignment.LEADING)
        .addGroup(gl_contentPane.createSequentialGroup()
            .addContainerGap()
            .addGroup(gl_contentPane.createParallelGroup(Alignment.TRAILING, false)
                .addGroup(Alignment.LEADING, gl_contentPane.createSequentialGroup()
                    .addComponent(btnSend, GroupLayout.PREFERRED_SIZE, 152, GroupLayout.PREFERRED_SIZE)
                    .addPreferredGap(ComponentPlacement.RELATED, GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(btnClear, GroupLayout.PREFERRED_SIZE, 150, GroupLayout.PREFERRED_SIZE))
                .addComponent(scrollPane, Alignment.LEADING)
                .addComponent(boardDisplay, Alignment.LEADING, GroupLayout.DEFAULT_SIZE, 520, Short.MAX_VALUE))
            .addGroup(gl_contentPane.createParallelGroup(Alignment.LEADING)
                .addGroup(gl_contentPane.createSequentialGroup()
                    .addPreferredGap(ComponentPlacement.RELATED, 33, Short.MAX_VALUE)
                    .addComponent(teamOnePanel, GroupLayout.PREFERRED_SIZE, 202, GroupLayout.PREFERRED_SIZE)
                    .addPreferredGap(ComponentPlacement.RELATED, 23, Short.MAX_VALUE)
                    .addComponent(teamTwoPanel, GroupLayout.PREFERRED_SIZE, 202, GroupLayout.PREFERRED_SIZE)
                    .addGap(25))
                .addGroup(gl_contentPane.createSequentialGroup()
                    .addGap(30)
                    .addComponent(chatScrollPane, GroupLayout.PREFERRED_SIZE, 430, GroupLayout.PREFERRED_SIZE)
                    .addContainerGap())
                .addGroup(gl_contentPane.createSequentialGroup()
                    .addGap(71)
                    .addComponent(teamOnePost, GroupLayout.PREFERRED_SIZE, 128, GroupLayout.PREFERRED_SIZE)
                    .addGap(92)
                    .addComponent(teamTwoPost, GroupLayout.PREFERRED_SIZE, 128, GroupLayout.PREFERRED_SIZE)
                    .addContainerGap())))
        );
    gl_contentPane.setVerticalGroup(
        gl_contentPane.createParallelGroup(Alignment.LEADING)
        .addGroup(gl_contentPane.createSequentialGroup()
            .addContainerGap()
            .addGroup(gl_contentPane.createParallelGroup(Alignment.LEADING, false)
                .addGroup(gl_contentPane.createSequentialGroup()
                    .addGroup(gl_contentPane.createParallelGroup(Alignment.LEADING)
                        .addComponent(teamTwoPost, GroupLayout.DEFAULT_SIZE, 52, Short.MAX_VALUE)
                        .addComponent(teamOnePost, Alignment.TRAILING, GroupLayout.DEFAULT_SIZE, GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                    .addPreferredGap(ComponentPlacement.RELATED)
                    .addGroup(gl_contentPane.createParallelGroup(Alignment.TRAILING, false)
                        .addComponent(teamOnePanel)
                        .addComponent(teamTwoPanel, GroupLayout.DEFAULT_SIZE, 344, Short.MAX_VALUE)))
                .addComponent(boardDisplay, GroupLayout.PREFERRED_SIZE, 402, GroupLayout.PREFERRED_SIZE))
            .addGap(18)
            .addGroup(gl_contentPane.createParallelGroup(Alignment.LEADING)
                .addGroup(gl_contentPane.createSequentialGroup()
                    .addComponent(scrollPane, GroupLayout.PREFERRED_SIZE, 127, GroupLayout.PREFERRED_SIZE)
                    .addPreferredGap(ComponentPlacement.RELATED)
                    .addGroup(gl_contentPane.createParallelGroup(Alignment.BASELINE)
                        .addComponent(btnSend, GroupLayout.DEFAULT_SIZE, 64, Short.MAX_VALUE)
                        .addComponent(btnClear, GroupLayout.DEFAULT_SIZE, 64, Short.MAX_VALUE)))
                .addComponent(chatScrollPane, GroupLayout.DEFAULT_SIZE, 197, Short.MAX_VALUE))
            .addContainerGap())
        );

    chatScrollPane.setViewportView(chatList);

    teamTwoPanel.setViewportView(t2PlayList);

    teamOnePanel.setViewportView(t1PlayList);

    scrollPane.setViewportView(textArea);
    contentPane.setLayout(gl_contentPane);
  }
  
  @Override
  public void react(PendingRequest req, String... results) {
    ServerRequest request = req.getRequest();
    if (request == ServerRequest.JOIN || request == ServerRequest.CSESS) {
      UUID sessionID = UUID.fromString(results[0]);
      boolean isTeamOne = Boolean.parseBoolean(results[1]);

      String rulesToParse = Arrays.stream(results).collect(Collectors.joining());
      SessionRules rules = SessionRules.parseFromString(rulesToParse);

      client.setSession(new SessionInfo(rules, sessionID, isTeamOne));
      
      resetAll();
      
      client.submitRequest(new RequestFuture(new PendingRequest(ServerRequest.UPDATE), this));
    }
    else if (request == ServerRequest.UPDATE) {
      System.out.println("-----> UPDATE RECIEVED");
      updateBoard(results[0]);
    }
    else if (request == ServerRequest.CUSER) {
      String echoName = results[0];
      UUID uuid = UUID.fromString(results[1]);
      
      client.setName(echoName);
      client.setUUID(uuid);
      System.out.println(" **** GOT NAME: "+echoName+" "+uuid);
    }
    else if (request == ServerRequest.VOTE) {
      chatList.append("----> VOTE ACCEPTED <---");
    }
    else if (request == ServerRequest.PLIST) {
      //print the list
      ArrayList<String> teamOnePlayers = new ArrayList<String>();
      ArrayList<String> teamTwoPlayers = new ArrayList<String>();
      
      System.out.println("---> PLIST RESULT: "+Arrays.toString(results));
      
      for (String string : results) {
        //there should only be at least two infos: name, isTeamOne
        //if we have UUID, then UUID should be the last piece of info
        String [] playerInfo = string.split(",");
        if (playerInfo[1].equals("true")) {
          teamOnePlayers.add(playerInfo[0]);
        }
        else {
          teamTwoPlayers.add(playerInfo[0]);
        }
      }
      
      updateTeam1Roster(teamOnePlayers);
      updateTeam2Roster(teamTwoPlayers);
    }
    else if (request == ServerRequest.QUIT) {
      resetAll();
    }
    else if (request == ServerRequest.DISC) {
      
    }
    else if (request == ServerRequest.ALL) {
      updateMessages(results[1], true, results[0]);
    }
    else if (request == ServerRequest.TEAM) {
      updateMessages(results[1], false, results[0]);
    }
    else if (request == ServerRequest.SES) {
      //TODO: how to display all sessions??? New dialog? or append to chat?
    }
  }
  
  @Override
  public void error(PendingRequest req, int errorCode) {
    chatList.append("-----> ERROR WITH REQ '"+req.toString()+"' with code "+errorCode+"<-----");
  }

  public void resetAll() {
    chatList.setText("");
    t1PlayList.setText("");
    t2PlayList.setText("");
    boardDisplay.setText("");
    repaint();
  }
  
  public void updateDislay() {
    boardDisplay.setText(display.toString());
    repaint();
  }
  
  public void updateWarningLine(String text) {
    display.setWarningLine(text);
    updateDislay();
  }

  public void updateBoard(String repr) {
    display.setBoard(repr);
    updateDislay();
  }

  public void updateTeam1Roster(List<String> teamOne) {
    clearTeam1Roster();
    
    for (String string : teamOne) {
      t1PlayList.append(string);
    }   
    
    repaint();
  }

  public void updateTeam2Roster(List<String> teamTwo) {
    clearTeam2Roster();
    
    for (String string : teamTwo) {
      t2PlayList.append(string);
    }   
    
    repaint();
  }

  public void clearTeam1Roster() {
    t1PlayList.setText("");
    repaint();
  }

  public void clearTeam2Roster() {
    t2PlayList.setText("");
    repaint();
  }

  public void updateMessages(String message, boolean toAll, String sender) {
    chatList.append("["+sender+" ("+(toAll ? "ALL" : "TEAM")+")] "+message+"\n"); 
    repaint();
  }
}
