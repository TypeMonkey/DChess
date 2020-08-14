package jg.proj.chess.net.client;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.Scanner;

import javafx.application.Application;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.layout.Pane;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;
import jg.proj.chess.net.client.ClientConfig.ConfigKey;
import jg.proj.chess.net.client.uis.GameBrowserController;
import jg.proj.chess.net.client.uis.GameEntranceController;
import jg.proj.chess.net.client.uis.GameScreenController;

/**
 * Front end for client-side operations and communications
 * @author Jose
 */
public class ChessClient extends Application{

  private String userName;
  
  
  private ClientConfig config;
  private Connector connector;
  private Stage uiStage;
  
  @Override
  public void start(Stage primaryStage) throws Exception {
    //load config file and read IP and Port values
    //the path of the config file is the first argument to the application
    config = readConfig(getParameters().getRaw().get(0));
    
    //create connector
    connector = new Connector(config.getValue(ConfigKey.IP), Integer.parseInt(config.getValue(ConfigKey.PORT)));
    
    uiStage = primaryStage;
    
    //show entrance scene
    showEntrance();
  }
  
  /**
   * Reads the provided configuration file
   * @param configPath - the path to the configuration file
   * @return a ClientConfig object
   * @throws IOException
   */
  private ClientConfig readConfig(String configPath) throws IOException{
    BufferedReader reader = new BufferedReader(new FileReader(configPath));
    
    ClientConfig config = new ClientConfig();
    
    String line = null;
    while ((line = reader.readLine()) != null) {
      String [] split = line.split("=");
      String key = split[0];
      String value = split[1];
      
      try {
        ConfigKey actKey = ConfigKey.valueOf(key);
        config.setValue(actKey, value);
      } catch (IllegalArgumentException e) {
        continue;
      }
    }
    
    reader.close();
    
    return config;
  }

  /**
   * Displays the entrance page
   * @throws IOException
   */
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
  
  /**
   * Displays the game browser page
   * @throws IOException
   */
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
  
  /**
   * Displays the game screen
   * @throws IOException
   */
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
  
  public String getUserName() {
    return userName;
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
