package jg.proj.chess.net.client.uis;

import java.io.File;
import java.io.IOException;

import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.event.EventType;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.Pane;
import javafx.scene.layout.StackPane;
import jg.proj.chess.net.ServerRequest;
import jg.proj.chess.net.client.ChessClient;
import jg.proj.chess.net.client.PendingRequest;
import jg.proj.chess.net.client.Reactor;
import jg.proj.chess.net.client.ResourceManager;

public class GameEntranceController implements Displayable{

  @FXML
  private StackPane mainStackPane;
  @FXML
  private Pane entrancePane;
  @FXML
  private Label democChessPost;
  @FXML
  private Label enterUserNamePost;
  @FXML
  private TextField userNameInput;
  @FXML
  private Button userNameEnterButton;
  @FXML 
  private Label creditsPost;
  
  //--FLAG VARIABLES
  private boolean invalidUsername;
  //--END OF FLAG VARIABLES
  
  private final ChessClient client;
  
  private GameEntranceController(ChessClient client){
    this.client = client;
  }
  
  private void init() {
    userNameEnterButton.setOnAction(new EventHandler<ActionEvent>() {
      public void handle(ActionEvent event) {
        String userName = userNameInput.getText();
        if (userName == null || userName.isEmpty() || userName.trim().isEmpty()) {
          userNameInput.setText("USERNAME CANNOT BE BLANK!!!");
          invalidUsername = true;
        }
        else {
          client.setUserName(userName);
          PendingRequest changeName = new PendingRequest(ServerRequest.CUSER, userName);
          client.sendRequest(changeName, new Reactor() {
            
            @Override
            public void react(PendingRequest request, String... results) {
              //change to game browser
              try {
                client.showBrowser();
              } catch (IOException e) {
                client.recordException(e);
              }
            }
            
            @Override
            public void error(PendingRequest request, int errorCode) {
              userNameInput.setText("SERVER ERROR TO CUSER: "+errorCode);
              invalidUsername = true;
            }
          });     
        }
      }
    });
    
    userNameInput.focusedProperty().addListener(new ChangeListener<Boolean>() {

      @Override
      public void changed(ObservableValue<? extends Boolean> observable, Boolean oldValue, Boolean newValue) {
        if (newValue && invalidUsername) {
          //textfield is in focus
          userNameInput.setText("");
          invalidUsername = false;
        }
      }
    });
    
    userNameInput.setOnAction(new EventHandler<ActionEvent>() {

      @Override
      public void handle(ActionEvent event) {
        userNameEnterButton.fire();
      }
    });
  }
  
  @Override
  public void prepare() {
   
  }
  
  private static GameEntranceController entranceController;
  private static Pane pane;
  
  public static Pane createUI(ResourceManager resourceManager, ChessClient client) throws IOException {
    if (pane == null) {
      entranceController = new GameEntranceController(client);    
      
      FXMLLoader uiLoader = new FXMLLoader();
      uiLoader.setController(entranceController);
      pane = uiLoader.load(resourceManager.getResourceAsStream("entranceUI"));
      
      entranceController.init();
      
      return pane;
    }
    return pane;
  }
  
  public static GameEntranceController getController() {
    return entranceController;
  }
  
  public static Pane getPane() {
    return pane;
  }
}
