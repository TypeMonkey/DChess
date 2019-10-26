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
import jg.proj.chess.net.client.uis.CSessDialog.CSessForm;

import javax.swing.GroupLayout;
import javax.swing.GroupLayout.Alignment;
import javax.swing.JTextPane;
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
import java.util.Arrays;

import javax.swing.SwingConstants;
import javax.swing.UIManager;
import javax.swing.border.BevelBorder;
import javax.swing.JButton;
import javax.swing.JEditorPane;
import javax.swing.JMenuBar;
import javax.swing.JMenu;
import javax.swing.JMenuItem;


public class MainFrame extends JFrame {
  
  private final GameClient client;

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
  private final JMenuItem connectPortal = new JMenuItem("Connect to A Session");


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
    
    setTitle("DChess Client 1.0");
    setResizable(false);
    setLocationRelativeTo(null);
    addWindowListener(new WindowAdapter() {
      @Override
      public void windowClosing(WindowEvent e) {
        client.submitRequest(new PendingRequest(ServerRequest.QUIT, new Object[0]));
        super.windowClosing(e);
      }
    });
    setBounds(100, 100, 1031, 718);
    
    setJMenuBar(menuBar);
    
    menuBar.add(connectMenuItem);
    
    connectMenuItem.add(connectPortal);
    setContentPane(contentPane);
    
    initComponents();
    initLayout();
  }
  
  private void initComponents() {
    contentPane.setBorder(new EmptyBorder(5, 5, 5, 5));
    
    boardDisplay.setEditable(false);
       
    teamOnePost.setHorizontalAlignment(SwingConstants.CENTER);
    teamOnePost.setFont(new Font("Segoe UI", Font.PLAIN, 25));
    
    teamTwoPost.setHorizontalAlignment(SwingConstants.CENTER);
    teamTwoPost.setFont(new Font("Segoe UI", Font.PLAIN, 25));
    
    teamOnePanel.setBorder(new BevelBorder(BevelBorder.LOWERED, null, null, null, null));    
    teamTwoPanel.setBorder(new BevelBorder(BevelBorder.LOWERED, null, null, null, null));
    
    btnSend.setFont(new Font("Segoe UI", Font.BOLD, 18)); 
    btnSend.addActionListener(new ActionListener() {
      
      @Override
      public void actionPerformed(ActionEvent e) {
        client.parseInput(textArea.getText());
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
    
    connectPortal.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent e) {
        CSessDialog dialog = new CSessDialog();
        dialog.setModal(true);
        
        dialog.setVisible(true);
        
        CSessForm form = dialog.blockUntilFinished();
        if (form != null) {
          client.submitRequest(form.asCsessRequest());
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
  
  public void updateDislay(String text) {
    boardDisplay.setText(text);
    boardDisplay.setFont(new Font("Consolas", Font.PLAIN, 20));
  }
  
  public void updateBoard(String repr) {
    String string = repr.replace("~", "<br>");
    string = string.replace(" ", "&nbsp;");
            
    updateDislay("<html>"+string+"</html>");
  }
}
