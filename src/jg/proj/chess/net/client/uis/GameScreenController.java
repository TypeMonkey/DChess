package jg.proj.chess.net.client.uis;
import javafx.scene.paint.Color;
import javafx.scene.paint.ImagePattern;
import javafx.scene.paint.Paint;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;

import javafx.application.Application;
import javafx.event.Event;
import javafx.event.EventHandler;
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
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.Border;
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Region;
import javafx.scene.layout.RowConstraints;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.shape.Rectangle;
import javafx.scene.text.TextAlignment;
import javafx.stage.Stage;
import jg.proj.chess.core.Board;
import jg.proj.chess.core.DefaultBoardPreparer;
import jg.proj.chess.core.Square;
import jg.proj.chess.core.units.InvalidMove;
import jg.proj.chess.net.ServerResponses;
import jg.proj.chess.net.client.ChessClient;
import jg.proj.chess.net.client.MessageListener;
import jg.proj.chess.net.client.ResourceManager;
import jg.proj.chess.net.client.SignalListener;
import jg.proj.chess.net.client.VoteTally;
import jg.proj.chess.net.client.uis.components.GraphicalSquare;
import jg.proj.chess.net.client.uis.components.VoteChoice;

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
  private final GraphicalSquare [][] visibleBoard;
  private final ChessClient client;
  private final ResourceManager resourceManager;
  
  //logic for chess board and game session
  private final Board board;
  private final VoteChoice voteChoice;
  
  private GameScreenController(ChessClient client) { 
    this.client = client;
    this.resourceManager = client.getResourceManager();
    voteChoice = new VoteChoice();
    visibleBoard = new GraphicalSquare[DEFAULT_BOARD_SIZE][DEFAULT_BOARD_SIZE];
    board = new Board(DEFAULT_BOARD_SIZE, DEFAULT_BOARD_SIZE);
  }
  
  public void init() {    
    //prepare logical board
    board.initialize(new DefaultBoardPreparer());
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
  public void handleMessage(String messageType, String ... messageContent) {
    if (messageType.equals(ServerResponses.RESULT)) {
      //Apply unit move to both logical board and graphical card
      
      //parse coordinates
      //NOTE: ranks start at 1.
      final char fromChar = messageContent[0].charAt(0);
      final int fromInt = Integer.parseInt(messageContent[1]);
      final char destChar = messageContent[2].charAt(0);
      final int destInt = Integer.parseInt(messageContent[3]);
      
      //update board
      
      //move units on the logical board
      Square fromSquare = board.querySquare(fromChar, fromInt);
      Square destSquare = board.querySquare(destChar, destInt);
      
      try {
        fromSquare.getUnit().moveTo(destSquare);
      } catch (InvalidMove e) {
        /*
         * This shouldn't happen as even if a session
         * allows for invalid votes, the server doesn't
         * send RESULT messages if an invalid vote reaches consensus
         */
        client.recordException(e);
      }
      
      //move unit on the graphical board
      Rectangle graphFromSquare = visibleBoard[fromInt - 1][fromChar - 'A'].getShape();
      Rectangle graphToSquare = visibleBoard[destInt - 1][destChar - 'A'].getShape();
      
      Paint unit = graphFromSquare.getFill();
      graphFromSquare.setFill(Color.TRANSPARENT);
      graphToSquare.setFill(unit);
    }
    else {
      chatListDisplay.getItems().add("["+messageType+"]"+messageContent);
    }
  }
  
  @Override
  public void handleSignal(int signal) {
    //TODO: Code to handle various signals
  }
  
  public void showLegalMoves( ) {
    
  }
  
  private void makeBoard() {      
    VBox rowFixer = new VBox();
    Group group = new Group(rowFixer);

        
    HBox colMarkerRow = new HBox(1);
    
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
      
      final Rectangle square = new Rectangle(46, 46);
      square.setFill(Color.TRANSPARENT);
      square.setStroke(Color.TRANSPARENT);
      square.setStrokeWidth(2);
      
      final StackPane stackPane = new StackPane();
      stackPane.setAlignment(Pos.CENTER);
      
      stackPane.getChildren().addAll(square, label);
      
      
      System.out.println("---column mark: "+rMarker);
      
      
      colMarkerRow.getChildren().add(stackPane);
    }
    rowFixer.getChildren().add(colMarkerRow);
    
    boolean greyOut = true;
    for(int r = 0; r<8; r++) {
      HBox colFixer = new HBox(1);
      
      final Label label = new Label(String.valueOf(r));
      label.setStyle("-fx-font-size: 30");
      label.setTextFill(Color.BLACK);
      label.setTextAlignment(TextAlignment.CENTER);
      label.setContentDisplay(ContentDisplay.CENTER);
      label.setAlignment(Pos.CENTER);
      label.setPrefSize(25, 25);
      
      final Rectangle colSquare = new Rectangle(46, 46);
      colSquare.setFill(Color.TRANSPARENT);
      colSquare.setStroke(Color.TRANSPARENT);
      colSquare.setStrokeWidth(2);
      
      final StackPane stackPane = new StackPane();
      stackPane.setAlignment(Pos.CENTER);
      
      stackPane.getChildren().addAll(colSquare, label);
      colFixer.getChildren().add(stackPane);
      
      for(int c = 0; c<8; c++) {
        Rectangle square = new Rectangle(46, 46);
        square.setStroke(Color.BLACK);
        square.setFill(greyOut ? Color.GREY : Color.TRANSPARENT);
        square.setStrokeWidth(2);
        
        StackPane squarePane = new StackPane();
        squarePane.setAlignment(Pos.CENTER);
        squarePane.setPrefSize(46, 46);
        squarePane.setMinSize(Region.USE_PREF_SIZE, Region.USE_PREF_SIZE);
        squarePane.setMaxSize(Region.USE_PREF_SIZE, Region.USE_PREF_SIZE);
        
        
        squarePane.getChildren().add(square);
        
        square.setOnMouseClicked( new EventHandler<Event>() {
          @Override
          public void handle(Event event) {
            System.out.println("---SQUARE CLICKED!! board greying!");
            
            //mark this sqaure's outline as green
            square.setStroke(Color.GREEN);
          }         
        });
        
        
        visibleBoard[r][c] = new GraphicalSquare(squarePane, square);
        
        colFixer.getChildren().add(squarePane);
      }
      
      greyOut = !greyOut;
      
      rowFixer.getChildren().add(colFixer);
    }    
    
    chessBoardPane.setAlignment(Pos.CENTER);    
    chessBoardPane.getChildren().add(group);
    
    /*
    //set chess pieces manually    
    try {
      //white pieces
      visibleBoard[0][0].setFill(new ImagePattern(new Image(resourceManager.getResourceAsStream("rookWhite"))));
      visibleBoard[0][1].setFill(new ImagePattern(new Image(resourceManager.getResourceAsStream("knightWhite"))));
      visibleBoard[0][2].setFill(new ImagePattern(new Image(resourceManager.getResourceAsStream("bishopWhite"))));
      visibleBoard[0][3].setFill(new ImagePattern(new Image(resourceManager.getResourceAsStream("queenWhite"))));
      visibleBoard[0][4].setFill(new ImagePattern(new Image(resourceManager.getResourceAsStream("kingWhite"))));
      visibleBoard[0][5].setFill(new ImagePattern(new Image(resourceManager.getResourceAsStream("rookWhite"))));
      visibleBoard[0][6].setFill(new ImagePattern(new Image(resourceManager.getResourceAsStream("knightWhite"))));
      visibleBoard[0][7].setFill(new ImagePattern(new Image(resourceManager.getResourceAsStream("bishopWhite"))));

      for(int c = 0; c < 8; c++) {
        visibleBoard[1][c].setFill(new ImagePattern(new Image(resourceManager.getResourceAsStream("pawnWhite"))));
      }
      
      //black pieces
      visibleBoard[7][0].setFill(new ImagePattern(new Image(resourceManager.getResourceAsStream("rookBlack"))));
      visibleBoard[7][1].setFill(new ImagePattern(new Image(resourceManager.getResourceAsStream("knightBlack"))));
      visibleBoard[7][2].setFill(new ImagePattern(new Image(resourceManager.getResourceAsStream("bishopBlack"))));
      visibleBoard[7][3].setFill(new ImagePattern(new Image(resourceManager.getResourceAsStream("queenBlack"))));
      visibleBoard[7][4].setFill(new ImagePattern(new Image(resourceManager.getResourceAsStream("kingBlack"))));
      visibleBoard[7][5].setFill(new ImagePattern(new Image(resourceManager.getResourceAsStream("rookBlack"))));
      visibleBoard[7][6].setFill(new ImagePattern(new Image(resourceManager.getResourceAsStream("knightBlack"))));
      visibleBoard[7][7].setFill(new ImagePattern(new Image(resourceManager.getResourceAsStream("bishopBlack"))));

      for(int c = 0; c < 8; c++) {
        visibleBoard[6][c].setFill(new ImagePattern(new Image(resourceManager.getResourceAsStream("pawnBlack"))));
      }

    } catch (IOException e) {
     client.recordException(e);
    }
    */
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