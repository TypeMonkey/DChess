package jg.proj.chess.net.client.uis;
import javafx.scene.paint.Color;
import javafx.scene.paint.ImagePattern;

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
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.TableView;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.image.Image;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.RowConstraints;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.shape.Rectangle;
import javafx.stage.Stage;
import jg.proj.chess.net.client.ChessClient;

public class GameScreenController{
  
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
  private TableView<String> voteTallyTable;
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
  
  private final ChessClient client;
  
  private GameScreenController(ChessClient client) { 
    this.client = client;
  }
  
  public void init() {
    sessionUUIDDisplay.setText("HELLO!!!!!");
    
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
  }
  
  private void makeBoard() {   
    VBox rowFixer = new VBox(0);
    
    Rectangle [][] spots = new Rectangle[8][8];
    
    for(int r = 0; r<8; r++) {
      HBox colFixer = new HBox(0);
      for(int c = 0; c<8; c++) {
        Rectangle square = new Rectangle(55, 55);
        square.setStroke(Color.BLACK);
        square.setFill(Color.TRANSPARENT);
        
        spots[r][c] = square;
        
        colFixer.getChildren().add(square);
      }
      rowFixer.getChildren().add(colFixer);
    }
    
    Group group = new Group(rowFixer);
    
    chessBoardPane.setAlignment(Pos.CENTER);    
    chessBoardPane.getChildren().add(group);
    
    //set chess pieces manually
    //black pieces
    try {
      spots[0][0].setFill(new ImagePattern(new Image(new File("chesspieces/rookBlack.png").toURI().toURL().toString())));
      spots[0][1].setFill(new ImagePattern(new Image(new File("chesspieces/horseBlack.png").toURI().toURL().toString())));
      spots[0][2].setFill(new ImagePattern(new Image(new File("chesspieces/bishopBlack.png").toURI().toURL().toString())));
      spots[0][3].setFill(new ImagePattern(new Image(new File("chesspieces/queenBlack.png").toURI().toURL().toString())));
      spots[0][4].setFill(new ImagePattern(new Image(new File("chesspieces/kingBlack.png").toURI().toURL().toString())));
      spots[0][5].setFill(new ImagePattern(new Image(new File("chesspieces/bishopBlack.png").toURI().toURL().toString())));
      spots[0][6].setFill(new ImagePattern(new Image(new File("chesspieces/horseBlack.png").toURI().toURL().toString())));
      spots[0][7].setFill(new ImagePattern(new Image(new File("chesspieces/rookBlack.png").toURI().toURL().toString())));

      for(int c = 0; c < 8; c++) {
        spots[1][c].setFill(new ImagePattern(new Image(new File("chesspieces/pawnBlack.png").toURI().toURL().toString())));
      }
      
      //white pieces
      spots[7][0].setFill(new ImagePattern(new Image(new File("chesspieces/rookWhite.png").toURI().toURL().toString())));
      spots[7][1].setFill(new ImagePattern(new Image(new File("chesspieces/horseWhite.png").toURI().toURL().toString())));
      spots[7][2].setFill(new ImagePattern(new Image(new File("chesspieces/bishopWhite.png").toURI().toURL().toString())));
      spots[7][3].setFill(new ImagePattern(new Image(new File("chesspieces/queenWhite.png").toURI().toURL().toString())));
      spots[7][4].setFill(new ImagePattern(new Image(new File("chesspieces/kingWhite.png").toURI().toURL().toString())));
      spots[7][5].setFill(new ImagePattern(new Image(new File("chesspieces/bishopWhite.png").toURI().toURL().toString())));
      spots[7][6].setFill(new ImagePattern(new Image(new File("chesspieces/horseWhite.png").toURI().toURL().toString())));
      spots[7][7].setFill(new ImagePattern(new Image(new File("chesspieces/rookWhite.png").toURI().toURL().toString())));

      for(int c = 0; c < 8; c++) {
        spots[6][c].setFill(new ImagePattern(new Image(new File("chesspieces/pawnWhite.png").toURI().toURL().toString())));
      }

    } catch (MalformedURLException e) {
      // TODO Auto-generated catch block
      e.printStackTrace();
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
