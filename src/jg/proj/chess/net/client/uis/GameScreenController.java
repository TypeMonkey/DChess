package jg.proj.chess.net.client.uis;
import javafx.scene.paint.Color;
import javafx.scene.paint.ImagePattern;
import javafx.scene.paint.Paint;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;

import javafx.application.Application;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Pos;
import javafx.scene.Group;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.ContentDisplay;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.TableView;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.image.Image;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.Border;
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.RowConstraints;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.shape.Rectangle;
import javafx.scene.text.TextAlignment;
import javafx.stage.Stage;
import jg.proj.chess.net.client.ChessClient;
import jg.proj.chess.net.client.MessageListener;
import jg.proj.chess.net.client.ResourceManager;
import jg.proj.chess.net.client.SignalListener;
import jg.proj.chess.net.client.VoteTally;

public class GameScreenController implements SignalListener, MessageListener{
  
  public static final int DEFAULT_BOARD_SIZE = 8;
  
  @FXML
  private StackPane mainPane;
  
  //--topleft components
  @FXML
  private HBox topHBox;
  @FXML
  private TextField sessionUUIDDisplay;
  
  //--board components
  @FXML 
  private VBox boardVBox;
  @FXML 
  private StackPane chessBoardPane;
  @FXML
  private HBox boardBottomHBox;
  @FXML
  private Button clearVoteButton;
  @FXML 
  private Button sendVoteButton;
  @FXML 
  private Label voteNowDisplay;
  
  //--bottom left components
  @FXML
  private ListView<String> teamOneList;
  @FXML
  private ListView<String> teamTwoList;
  
  //--center right components (vote tally and chat)
  @FXML
  private Label voteTallyPost;
  @FXML
  private Label chatPost;
  @FXML
  private GridPane tallyAndCharGridPane;
  @FXML
  private TableView<VoteTally> voteTallyTable;
  @FXML
  private ListView<String> chatListDisplay;
  
  //--bottom right components
  @FXML
  private VBox chatInputVBox;
  @FXML 
  private TextArea chatInput;
  @FXML 
  private Button chatSendButton;
  @FXML
  private Button clearSendButton;
  
  //actual chess board
  private final Rectangle [][] board;
  private final ChessClient client;
  private final ResourceManager resourceManager;
  
  private GameScreenController(ChessClient client) { 
    this.client = client;
    this.resourceManager = client.getResourceManager();
    board = new Rectangle[DEFAULT_BOARD_SIZE][DEFAULT_BOARD_SIZE];
  }
  
  public void init() {    
    //set the session uuid
    //sessionUUIDDisplay.setText(client.getCurrentSession().getSessionID().toString());
    sessionUUIDDisplay.setEditable(false);
    
    //set style 
    voteTallyPost.setStyle("-fx-padding: 2;" + 
        "-fx-border-style: solid inside;" + 
        "-fx-border-width: 2;" +
        "-fx-border-insets: 5;" + 
        "-fx-border-radius: 3;" + 
        "-fx-border-color: grey;");
    
    chatPost.setStyle("-fx-padding: 2;" + 
        "-fx-border-style: solid inside;" + 
        "-fx-border-width: 2;" +
        "-fx-border-insets: 5;" + 
        "-fx-border-radius: 3;" + 
        "-fx-border-color: grey;");
    
    chatInput.setWrapText(true);
    sessionUUIDDisplay.setEditable(false);
        
    makeBoard();  
    
    /*
     * When sending "all" or "team" messages, provide BLANK Reactor instance
     * instead of writing out a manual reactor. 
     * 
     *  all,team and server signals will have their own listeners
     */
    
    //add this as listeners
    client.addMessageListener(this);
    client.addSignalListener(this);
  }
  
  @Override
  public void handleMessage(String messageType, String messageContent) {
    chatListDisplay.getItems().add("["+messageType+"]"+messageContent);
  }
  
  @Override
  public void handleSignal(int signal) {
    //TODO: Code to handle various signals
  }
  
  private void makeBoard() {   
    VBox rowFixer = new VBox(0);
        
    HBox colMarkerRow = new HBox(0);
    
    //this is the square at the top left corner of the board
    final Rectangle dummyCornerSquare = new Rectangle(50, 50);
    dummyCornerSquare.setFill(Color.TRANSPARENT);
    dummyCornerSquare.setStroke(Color.TRANSPARENT);
    colMarkerRow.getChildren().add(dummyCornerSquare);
    
    //add column marks 'A' <=> 'H'
    for(char rMarker = 'A'; rMarker <= 'H'; rMarker++) {      
      
      final Label label = new Label(String.valueOf(rMarker));
      label.setStyle("-fx-font-size: 30");
      label.setTextFill(Color.BLACK);
      label.setTextAlignment(TextAlignment.CENTER);
      label.setContentDisplay(ContentDisplay.CENTER);
      label.setAlignment(Pos.CENTER);
      label.setPrefSize(25, 25);
      
      final Rectangle square = new Rectangle(50, 50);
      square.setFill(Color.TRANSPARENT);
      square.setStroke(Color.TRANSPARENT);
      
      final StackPane stackPane = new StackPane();
      stackPane.setAlignment(Pos.CENTER);
      
      stackPane.getChildren().addAll(square, label);
      
      
      System.out.println("---column mark: "+rMarker);
      
      
      colMarkerRow.getChildren().add(stackPane);
    }
    rowFixer.getChildren().add(colMarkerRow);

    
    for(int r = 0; r<8; r++) {
      HBox colFixer = new HBox(0);
      
      final Label label = new Label(String.valueOf(r));
      label.setStyle("-fx-font-size: 30");
      label.setTextFill(Color.BLACK);
      label.setTextAlignment(TextAlignment.CENTER);
      label.setContentDisplay(ContentDisplay.CENTER);
      label.setAlignment(Pos.CENTER);
      label.setPrefSize(25, 25);
      
      final Rectangle colSquare = new Rectangle(50, 50);
      colSquare.setFill(Color.TRANSPARENT);
      colSquare.setStroke(Color.TRANSPARENT);
      
      final StackPane stackPane = new StackPane();
      stackPane.setAlignment(Pos.CENTER);
      
      stackPane.getChildren().addAll(colSquare, label);
      colFixer.getChildren().add(stackPane);
      
      for(int c = 0; c<8; c++) {
        Rectangle square = new Rectangle(50, 50);
        square.setStroke(Color.BLACK);
        square.setFill(Color.TRANSPARENT);
        
        board[r][c] = square;
        
        colFixer.getChildren().add(square);
      }
      rowFixer.getChildren().add(colFixer);
    }
    
    Group group = new Group(rowFixer);
    
    chessBoardPane.setAlignment(Pos.CENTER);    
    chessBoardPane.getChildren().add(group);
    
    //set chess pieces manually    
    try {
      //white pieces
      board[0][0].setFill(new ImagePattern(new Image(resourceManager.getResourceAsStream("rookWhite"))));
      board[0][1].setFill(new ImagePattern(new Image(resourceManager.getResourceAsStream("knightWhite"))));
      board[0][2].setFill(new ImagePattern(new Image(resourceManager.getResourceAsStream("bishopWhite"))));
      board[0][3].setFill(new ImagePattern(new Image(resourceManager.getResourceAsStream("queenWhite"))));
      board[0][4].setFill(new ImagePattern(new Image(resourceManager.getResourceAsStream("kingWhite"))));
      board[0][5].setFill(new ImagePattern(new Image(resourceManager.getResourceAsStream("rookWhite"))));
      board[0][6].setFill(new ImagePattern(new Image(resourceManager.getResourceAsStream("knightWhite"))));
      board[0][7].setFill(new ImagePattern(new Image(resourceManager.getResourceAsStream("bishopWhite"))));

      for(int c = 0; c < 8; c++) {
        board[1][c].setFill(new ImagePattern(new Image(resourceManager.getResourceAsStream("pawnWhite"))));
      }
      
      //black pieces
      board[7][0].setFill(new ImagePattern(new Image(resourceManager.getResourceAsStream("rookBlack"))));
      board[7][1].setFill(new ImagePattern(new Image(resourceManager.getResourceAsStream("knightBlack"))));
      board[7][2].setFill(new ImagePattern(new Image(resourceManager.getResourceAsStream("bishopBlack"))));
      board[7][3].setFill(new ImagePattern(new Image(resourceManager.getResourceAsStream("queenBlack"))));
      board[7][4].setFill(new ImagePattern(new Image(resourceManager.getResourceAsStream("kingBlack"))));
      board[7][5].setFill(new ImagePattern(new Image(resourceManager.getResourceAsStream("rookBlack"))));
      board[7][6].setFill(new ImagePattern(new Image(resourceManager.getResourceAsStream("knightBlack"))));
      board[7][7].setFill(new ImagePattern(new Image(resourceManager.getResourceAsStream("bishopBlack"))));

      for(int c = 0; c < 8; c++) {
        board[6][c].setFill(new ImagePattern(new Image(resourceManager.getResourceAsStream("pawnBlack"))));
      }

    } catch (IOException e) {
     client.recordException(e);
    }
  }

  private static GameScreenController screenController;
  private static Pane pane;
  
  public static Pane createUI(String fxmlLocation, ChessClient client) throws IOException {
    if (pane == null) {
      screenController = new GameScreenController(client);    
      
      FXMLLoader uiLoader = new FXMLLoader();
      uiLoader.setController(screenController);
      uiLoader.setLocation(new File(fxmlLocation).toURI().toURL());
      pane = uiLoader.load();
      
      screenController.init();
      
      return pane;
    }
    return pane;
  }
  
  public static GameScreenController getController() {
    return screenController;
  }
}
