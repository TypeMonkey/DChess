package jg.proj.chess.net.client;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.Arrays;
import java.util.HashSet;

import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import javafx.application.Application;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.scene.layout.Pane;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;
import jg.proj.chess.core.units.Unit.UnitType;
import jg.proj.chess.net.client.ClientConfig.ConfigKey;
import jg.proj.chess.net.client.ResourceManager.ResourceInfo;
import jg.proj.chess.net.client.uis.GameBrowserController;
import jg.proj.chess.net.client.uis.GameEntranceController;
import jg.proj.chess.net.client.uis.GameScreenController;

/**
 * Front end for client-side operations and communications
 * @author Jose
 */
public class ChessClient extends Application{

  //session information
  private String userName;
  private SessionInfo currentSession;
  private int teamID;
  
  //network and other game objects
  private ClientConfig config;
  private Connector connector;
  private ResourceManager resourceManager;
  private EventLoopGroup workerPool;
  
  //main UI object
  private Stage uiStage;
  
  @Override
  public void start(Stage primaryStage) throws Exception {
    uiStage = primaryStage;
    
    uiStage.setTitle("DChess v1");
    
    workerPool = new NioEventLoopGroup();

    //load config file and read IP and Port values
    //the path of the config file is the first argument to the application
    config = readConfig(getParameters().getRaw().get(0));
    
    //create connector
    connector = new Connector(this, 
                              workerPool, 
                              config.getValue(ConfigKey.IP), 
                              Integer.parseInt(config.getValue(ConfigKey.PORT)));   
    connector.connect();
    
    //load game resources
    loadResources();
    
    //set app icon
    uiStage.getIcons().add(new Image(resourceManager.getResourceAsStream("appIcon")));
    
    //show entrance scene
    showEntrance();
  }
  
  private void loadResources() throws IOException {
    HashSet<ResourceInfo> generalResources = new HashSet<>();
    
    //add icon as a resource too
    generalResources.add(new ResourceInfo("appIcon", "icon.png"));
    
    //get all unit images path
    for (UnitType unitType : UnitType.values()) {
      String whiteImageName = unitType.name().toLowerCase()+"White";
      String blackImageName = unitType.name().toLowerCase()+"Black";
      
      generalResources.add(new ResourceInfo(whiteImageName, "chesspieces/"+whiteImageName+".png"));
      generalResources.add(new ResourceInfo(blackImageName, "chesspieces/"+blackImageName+".png"));
    }
    
    //load ui files
    generalResources.add(new ResourceInfo("screenUI", "xmls/GameScreen.fxml"));
    generalResources.add(new ResourceInfo("entranceUI", "xmls/GameEntrance.fxml"));
    generalResources.add(new ResourceInfo("browserUI", "xmls/GameBrowser.fxml"));
    
    resourceManager = new ResourceManager(generalResources);
        
    //load all resources 
    resourceManager.loadAllResources();
    
    //pre-load all UI screens
    GameEntranceController.createUI(resourceManager, this);
    GameBrowserController.createUI(resourceManager, this);
    GameScreenController.createUI(resourceManager, this);
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
      String key = split[0].toUpperCase();
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
    final Pane pane = GameEntranceController.getPane();
    final StackPane stackPane = new StackPane(pane);
    stackPane.setAlignment(Pos.CENTER);
    
    final Scene scene = new Scene(stackPane);
    System.out.println("----CHANGING TO ENTRANCE");
    uiStage.setScene(scene);
    
    //call prepare()
    GameEntranceController.getController().prepare();
            
    //now show our UI
    uiStage.show();   
  }
  
  /**
   * Displays the game browser page
   * @throws IOException
   */
  public void showBrowser() throws IOException{
    final Pane pane = GameBrowserController.getPane();
    final StackPane stackPane = new StackPane(pane);
    stackPane.setAlignment(Pos.CENTER);
    
    final Scene scene = new Scene(stackPane);
    System.out.println("----CHANGING TO BROWSER");
    uiStage.setScene(scene);
    
    //set screen title to include username
    uiStage.setTitle(uiStage.getTitle()+" - "+userName);
    
    //call prepare()
    GameBrowserController.getController().prepare();
    
    //now show our UI
    uiStage.show();
  }
  
  /**
   * Displays the game screen
   * @throws IOException
   */
  public void showGame() throws IOException{
    final Pane pane = GameScreenController.getPane();
    final StackPane stackPane = new StackPane(pane);
    stackPane.setAlignment(Pos.CENTER);
    
    final Scene scene = new Scene(stackPane);
    System.out.println("----CHANGING TO SCREEN");
    uiStage.setScene(scene);
    
    //call prepare()
    GameScreenController.getController().prepare();
    
    //now show our UI
    uiStage.show();
  }
  
  public void setUserName(String userName) {
    this.userName = userName;
  }
  
  public void setCurrentSession(int teamID, SessionInfo currentSession) {
    this.teamID = teamID;
    this.currentSession = currentSession;
  }
  
  public String getUserName() {
    return userName;
  }
  
  public SessionInfo getCurrentSession() {
    return currentSession;
  }
  
  public int getCurrentTeam() {
    return teamID;
  }
  
  public ResourceManager getResourceManager() {
    return resourceManager;
  }
  
  public void addSignalListener(SignalListener listener) {
    connector.addSignalListener(listener);
  }
  
  public void addMessageListener(MessageListener listener) {
   connector.addMessageListener(listener);
  }
  
  public void sendRequest(RequestBody request, Reactor reactor) {
    System.out.println("---REQUEST SUBMITTED: "+request+" | "+Arrays.toString(request.getArguments()));   
    connector.sendRequest(request, reactor);  
  }
  
  /**
   * Shuts down the worker pool and Connector
   */
  @Override
  public void stop() throws InterruptedException {
    connector.shutdown();
    workerPool.shutdownGracefully().sync();
  }
  
  public synchronized void recordException(Exception exception) {
    exception.printStackTrace();
  }
  
  public synchronized void recordException(String message) {
    System.err.println(message);
  }

  public static void main(String[] args) {
    launch("config.txt");  
  }
}
