package jg.proj.chess.net.client;

import java.io.IOException;

import javafx.application.Application;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.layout.Pane;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;
import jg.proj.chess.net.ServerRequest;
import jg.proj.chess.net.client.uis.GameBrowserController;
import jg.proj.chess.net.client.uis.GameEntranceController;
import jg.proj.chess.net.client.uis.GameScreenController;

/**
 * Front end for client-side operations and communications
 * @author Jose
 */
public class ChessClient extends Application{

  private String userName;
  
  
  private Stage uiStage;
  
  @Override
  public void start(Stage primaryStage) throws Exception {
    uiStage = primaryStage;
  }
  
  public void showEntrance() throws IOException {
    //show entrance scene
    final Pane pane = GameEntranceController.createUI("xmls/GameEntrance.fxml", this);
    final StackPane stackPane = new StackPane(pane);
    stackPane.setAlignment(Pos.CENTER);
    
    final Scene scene = new Scene(stackPane);
    System.out.println("----CHANGING TO ENTRANCE");
    uiStage.setScene(scene);
        
    //now show our UI
    uiStage.show();
  }
  
  public void showBrowser() throws IOException{
    final Pane pane = GameBrowserController.createUI("xmls/GameBrowser.fxml", this);
    final StackPane stackPane = new StackPane(pane);
    stackPane.setAlignment(Pos.CENTER);
    
    final Scene scene = new Scene(stackPane);
    System.out.println("----CHANGING TO BROWSER");
    uiStage.setScene(scene);
    
    //now show our UI
    uiStage.show();
  }
  
  public void showGame() throws IOException{
    final Pane pane = GameScreenController.createUI("xmls/GameScreen.fxml", this);
    final StackPane stackPane = new StackPane(pane);
    stackPane.setAlignment(Pos.CENTER);
    
    final Scene scene = new Scene(stackPane);
    System.out.println("----CHANGING TO SCREEN");
    uiStage.setScene(scene);
    
    //now show our UI
    uiStage.show();
  }
  
  public void setUserName(String userName) {
    this.userName = userName;
  }
  
  public void sendRequest(PendingRequest request, Reactor reactor) {
    System.out.println("---REQUEST SUBMITTED: "+request);
  }
  
  public void recordException(Exception exception) {
    exception.printStackTrace();
  }
  
  public void recordException(String message) {
    System.err.println(message);
  }

  public static void main(String[] args) {
    launch(args);  
  }
}
