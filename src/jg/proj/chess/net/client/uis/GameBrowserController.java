package jg.proj.chess.net.client.uis;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleLongProperty;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.RadioButton;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.control.Tooltip;
import javafx.scene.control.TableColumn.CellDataFeatures;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.VBox;
import javafx.util.Callback;
import jg.proj.chess.net.ServerRequest;
import jg.proj.chess.net.SessionRules;
import jg.proj.chess.net.SessionRules.Properties;
import jg.proj.chess.net.client.ChessClient;
import jg.proj.chess.net.client.RequestBody;
import jg.proj.chess.net.client.Reactor;
import jg.proj.chess.net.client.ResourceManager;
import jg.proj.chess.net.client.SessionInfo;

public class GameBrowserController implements Preparable{
  
  //Left side components
  @FXML
  private VBox gameBrowserBox;
  @FXML
  private TableView<SessionInfo> activeSessionsTable; 
  @FXML
  private TextField sessionUUIDInput;
  @FXML
  private Button searchSessionButton;
  @FXML
  private Button joinSessionButton;
  @FXML 
  private Button refreshButton;
  @FXML
  private HBox teamChoiceButtons;
  @FXML
  private RadioButton randTeamChoice;
  @FXML
  private RadioButton teamOneChoice;
  @FXML
  private RadioButton teamTwoChoice;
  
  //Right side components
  @FXML
  private VBox createSessionBox;
  
  //--Create session team choice buttons
  @FXML
  private Label teamPost;
  @FXML
  private RadioButton teamOne;
  @FXML
  private RadioButton teamTwo; 
  @FXML
  private RadioButton randomTeam;
  
  //--Prisoner's Dilemma choice buttons
  @FXML
  private Label prisonDilPost;
  @FXML
  private RadioButton yesPrisDil; 
  @FXML
  private RadioButton noPrisDil;
  @FXML
  private RadioButton randPrisDil;
  
  //--Invalid Votes choice buttons
  @FXML
  private Label invalidVotesPost;
  @FXML
  private RadioButton yesInvalVote; 
  @FXML
  private RadioButton noInvalVote;
  @FXML
  private RadioButton randInvalVote;
  
  //--Late join choice buttons
  @FXML
  private Label lateJoinPost;
  @FXML
  private RadioButton yesLate;
  @FXML
  private RadioButton noLate;
  @FXML
  private RadioButton randLate;
  
  //--Turn break choice components
  @FXML
  private Label turnBreaksPost;
  @FXML
  private TextField turnBreakDurationInput;
  
  //--seconds for voting input
  @FXML 
  private Label votingDurationPost;
  @FXML
  private TextField voteDurationInput;
  
  //--min player amount input
  @FXML
  private Label minPlayerAmntPost;
  @FXML
  private TextField minPlayerInput;
  
  //--create session button
  @FXML
  private Button createSessionButton;
  
    
  private final ChessClient client;
  private final Map<UUID, SessionInfo> activeSessMap;
  private final ObservableList<SessionInfo> activeSessions;
  
  //variables for flags and temporary values
  private SessionInfo selectedSessionInfo;
  
  private GameBrowserController(ChessClient client) {
    this.client = client;
    activeSessions = FXCollections.observableArrayList();
    activeSessMap = new HashMap<>();
  }
  
  
  /**
   * Initialized the GameBroswer UI
   * @throws IOException 
   */
  @SuppressWarnings("unchecked")
  private void init(){
    //set custom styles
    createSessionBox.setStyle("-fx-padding: 10;" + 
        "-fx-border-style: solid inside;" + 
        "-fx-border-width: 2;" +
        "-fx-border-insets: 5;" + 
        "-fx-border-radius: 3;" + 
        "-fx-border-color: grey;");
    
    gameBrowserBox.setStyle("-fx-padding: 10;" + 
        "-fx-border-style: solid inside;" + 
        "-fx-border-width: 2;" +
        "-fx-border-insets: 5;" + 
        "-fx-border-radius: 3;" + 
        "-fx-border-color: grey;");
    
    //disable team buttons
    randTeamChoice.setDisable(true);
    teamOneChoice.setDisable(true);
    teamTwoChoice.setDisable(true);

    //disable join button
    joinSessionButton.setDisable(true);
    
    //set tooltips texts
    Tooltip teamTip = new Tooltip("Sets the team you'd join once the session is created.");
    teamTip.setWrapText(true);
    teamTip.setStyle("-fx-font-size: 12");
    
    teamPost.setTooltip(teamTip);
   
    Tooltip prisonDilTip = new Tooltip("If enforced, this session will disallow "
        + "communication between players (among or between teams) "
        + "and won't display vote tallys.");
    prisonDilTip.setWrapText(true);
    prisonDilTip.setStyle("-fx-font-size: 12");
    prisonDilTip.setPrefWidth(400);
    
    prisonDilPost.setTooltip(prisonDilTip);
    
    Tooltip invalidTip = new Tooltip("If allowed, the server will not filter invalid/illegal votes" 
        + " from being counted at the end of the voting process." 
        + " This means that a team can agree on an invalid/illegal move at the end"
        + "of the voting duration, causing no actual moves to be made.");
    invalidTip.setWrapText(true);
    invalidTip.setStyle("-fx-font-size: 12");
    invalidTip.setPrefWidth(400);
    
    invalidVotesPost.setTooltip(invalidTip);
    
    Tooltip lateTip = new Tooltip("Sets whether to allow players to join mid-game, "
        + "after the minimum amount of players for both teams have been reach.");
    lateTip.setWrapText(true);
    lateTip.setStyle("-fx-font-size: 12");
    lateTip.setPrefWidth(400);
    
    lateJoinPost.setTooltip(lateTip);
    
    Tooltip turnBreak = new Tooltip("Sets the amount of seconds between each turn, as a sort of break.");
    turnBreak.setWrapText(true);
    turnBreak.setStyle("-fx-font-size: 12");
    turnBreak.setPrefWidth(400);
    
    turnBreaksPost.setTooltip(turnBreak);  
    
    //set handlers
    
    //refreshButton handle code
    refreshButton.setOnAction(new EventHandler<ActionEvent>() {
      
      @Override
      public void handle(ActionEvent event) {
        
        client.sendRequest(new RequestBody(ServerRequest.SES), new Reactor() {
          
          @Override
          public void react(RequestBody request, String... results) {
            //clear session list
            activeSessions.clear();
            activeSessMap.clear();
            
            //each string in results array represent the information of each active session
                        
            for (String ses : results) {
              String [] values = ses.split(",");
              UUID uuid = UUID.fromString(values[0]);
              int playerAmnt = Integer.parseInt(values[1]);
              boolean prisonDilemma = Boolean.parseBoolean(values[2]);
              long voteDuration = Long.parseLong(values[3]);
              boolean invalidVoting = Boolean.parseBoolean(values[4]);
              long breakAmount = Long.parseLong(values[5]);
              boolean lateGameJoin = Boolean.parseBoolean(values[6]);
              int minTeamCount = Integer.parseInt(values[7]);

              
              SessionRules rules = new SessionRules();
              rules.setProperty(Properties.PRISON_DILEMMA, prisonDilemma);
              rules.setProperty(Properties.VOTING_DURATION, voteDuration);
              rules.setProperty(Properties.ALLOW_INVL_VOTES, invalidVoting);
              rules.setProperty(Properties.BREAK_AMOUNT, breakAmount);
              rules.setProperty(Properties.ALLOW_JOINS_GAME, lateGameJoin);
              rules.setProperty(Properties.MIN_TEAM_COUNT, minTeamCount);

              
              SessionInfo info = new SessionInfo(rules, uuid, playerAmnt);
              activeSessions.add(info); 
              activeSessMap.put(uuid, info);    
              activeSessionsTable.getItems().add(info);
            }
          }
          
          @Override
          public void error(RequestBody request, int errorCode) {
            //there should be no error
          }
        });
        
      }
    });    
    
    //searchSessionButton handle code
    searchSessionButton.setOnAction(new EventHandler<ActionEvent>() {
      
      @Override
      public void handle(ActionEvent event) {
        String rawUUID = sessionUUIDInput.getText();
        try {
          UUID uuid = UUID.fromString(rawUUID);

          if (activeSessMap.containsKey(uuid)) {
            ObservableList<SessionInfo> temp = FXCollections.observableArrayList(activeSessions);          
            activeSessions.clear();
            
            activeSessions.add(activeSessMap.get(uuid));
          }  
          
        } catch (IllegalArgumentException e) {
          //bad uuid, do nothing
        }
      }
    });
    
    //join session handle
    joinSessionButton.setOnAction(new EventHandler<ActionEvent>() {

      @Override
      public void handle(ActionEvent event) {
        //use selectedSessionInfo to get UUID of desired session
        Reactor successJoin = new Reactor() {
          
          @Override
          public void react(RequestBody request, String... results) {
            try {
              String uuid = results[0];
              boolean isTeamOne = Boolean.parseBoolean(results[1]);
              
              String [] configStrings = Arrays.copyOfRange(results, 2, results.length);
              String configWhole = Arrays.stream(configStrings).collect(Collectors.joining());
              
              SessionRules sessionRules = SessionRules.parseFromString(configWhole);
              client.setCurrentSession(isTeamOne ? 1 : 2, new SessionInfo(sessionRules, UUID.fromString(uuid), 0));
              client.showGame();
            } catch (IOException e) {
              client.recordException(e);
            }
          }
          
          @Override
          public void error(RequestBody request, int errorCode) {
            client.recordException("FAILED TO JOIN SESSION: "+selectedSessionInfo.getSessionID());
            joinSessionButton.setDisable(false);
          }
        };
        
        //disable join button
        joinSessionButton.setDisable(true);
        
        //check team selection
        int teamNum = teamOneChoice.isSelected() ? 1 : (teamTwoChoice.isSelected() ? 2 : 3);
        
        //send join request
        client.sendRequest(new RequestBody(ServerRequest.JOIN, selectedSessionInfo.getSessionID().toString(), teamNum), successJoin);      
      }
      
    });
    
    //create session handler
    createSessionButton.setOnAction(new EventHandler<ActionEvent>() {

      @Override
      public void handle(ActionEvent event) {
        String voteDuration = voteDurationInput.getText().trim();
        if (voteDuration == null || voteDuration.isEmpty()) {
          voteDurationInput.setText("Duration cannot be blank!");
          voteDuration = null;
        }    
        if (!voteDuration.chars().allMatch(Character::isDigit)) {
          voteDurationInput.setText("Duration must be a number!");
          voteDuration = null;
        }
               
        String minPlayers = minPlayerInput.getText();
        if (minPlayers == null || minPlayers.isEmpty()) {
          minPlayerInput.setText("Minimum player amount cannot be blank!");
          minPlayers = null;
        }
        if (!minPlayers.chars().allMatch(Character::isDigit)) {
          minPlayerInput.setText("Minimum player amount must be a number!");
          minPlayers = null;
        }
        
        String breakDuration = turnBreakDurationInput.getText();
        if (breakDuration == null || breakDuration.isEmpty()) {
          //break durations is optional. Default is 0
          breakDuration = "0";
        }
        if(!breakDuration.chars().allMatch(Character::isDigit)){
          minPlayerInput.setText("Turn break must be a number!");
          breakDuration = null;
        }
      
        if (minPlayers != null && voteDuration != null && breakDuration != null) {
          //collect all data
          long vDuration = Long.parseLong(voteDuration);
          int mPlayers = Integer.parseInt(minPlayers);
          long bDuration = Long.parseLong(breakDuration);
          int teamID = teamOne.isSelected() ? 1 : (teamTwo.isSelected() ? 2 : 3);
          boolean prisDil = yesPrisDil.isSelected();
          boolean allowInvalid = yesInvalVote.isSelected();
          boolean allowLate = yesLate.isSelected();
          
          boolean errorFound = false;
          if (vDuration < (long) Properties.VOTING_DURATION.getDefaultValue()) {
            voteDurationInput.setText("Vote duration must be at least "+Properties.VOTING_DURATION.getDefaultValue()+" seconds!");
            errorFound = true;
          }
          if (mPlayers < (int) Properties.MIN_TEAM_COUNT.getDefaultValue()) {
            minPlayerInput.setText("Minimum player amount must be at least "+Properties.MIN_TEAM_COUNT.getDefaultValue());
            errorFound = true;
          }

          if (!errorFound) {
            RequestBody createReq = new RequestBody(ServerRequest.CSESS, 
                teamID, 
                prisDil, 
                vDuration, 
                mPlayers, 
                allowInvalid,
                allowLate,
                bDuration);

            SessionRules rules = new SessionRules();
            rules.setProperty(Properties.ALLOW_INVL_VOTES, allowInvalid);
            rules.setProperty(Properties.ALLOW_JOINS_GAME, allowLate);
            rules.setProperty(Properties.PRISON_DILEMMA, prisDil);
            rules.setProperty(Properties.VOTING_DURATION, vDuration);
            rules.setProperty(Properties.MIN_TEAM_COUNT, mPlayers);
            rules.setProperty(Properties.BREAK_AMOUNT, bDuration);

            Reactor reactor = new Reactor() {
              public void react(RequestBody request, String... results) {
                //TODO: ADD CODE             
                try {        
                  String uuid = results[0];
                  int teamID = Boolean.parseBoolean(results[1]) ? 1 : 2;
                  client.setCurrentSession(teamID, new SessionInfo(rules, UUID.fromString(uuid), 0));
                  client.showGame();
                } catch (IOException e) {
                  client.recordException(e);
                }
              }

              public void error(RequestBody request, int errorCode) {
                client.recordException("COULDN'T CREATE SESION!!!");
              }
            };

            client.sendRequest(createReq, reactor);
          }
        }
      }
      
    });
    
    //set up sessions table
    //player count column
    TableColumn<SessionInfo, Number> playerAmnt = new TableColumn<>("Player Count");
    playerAmnt.setCellValueFactory(new Callback<TableColumn.CellDataFeatures<SessionInfo,Number>, ObservableValue<Number>>() {
      
      @Override
      public ObservableValue<Number> call(CellDataFeatures<SessionInfo, Number> param) {
        return new SimpleIntegerProperty(param.getValue().getPlayerAmnt());
      }
    });
    
    //Pris. Dil. column
    TableColumn<SessionInfo, Boolean> prisonDil = new TableColumn<>("Prisoner's Dilemma Enforced?");
    prisonDil.setCellValueFactory(new Callback<TableColumn.CellDataFeatures<SessionInfo,Boolean>, ObservableValue<Boolean>>() {

      @Override
      public ObservableValue<Boolean> call(CellDataFeatures<SessionInfo, Boolean> param) {
        return new SimpleBooleanProperty((boolean) param.getValue().getRules().getProperty(Properties.PRISON_DILEMMA));
      }
    });
    
    //Vote duration column
    TableColumn<SessionInfo, Number> voteDuration = new TableColumn<>("Vote Duration (seconds)");
    voteDuration.setCellValueFactory(new Callback<TableColumn.CellDataFeatures<SessionInfo,Number>, ObservableValue<Number>>() {

      @Override
      public ObservableValue<Number> call(CellDataFeatures<SessionInfo, Number> param) {
        return new SimpleLongProperty((Long) param.getValue().getRules().getProperty(Properties.VOTING_DURATION));
      }
    });
    
    //Break duration column
    TableColumn<SessionInfo, Number> breakDuration = new TableColumn<>("Break Duration (seconds)");
    breakDuration.setCellValueFactory(new Callback<TableColumn.CellDataFeatures<SessionInfo,Number>, ObservableValue<Number>>() {

      @Override
      public ObservableValue<Number> call(CellDataFeatures<SessionInfo, Number> param) {
        return new SimpleLongProperty((long) param.getValue().getRules().getProperty(Properties.BREAK_AMOUNT));
      }
    });
    
    //Invalid votes column
    TableColumn<SessionInfo, Boolean> invalVotes = new TableColumn<>("Invalid Votes allowed?");
    invalVotes.setCellValueFactory(new Callback<TableColumn.CellDataFeatures<SessionInfo,Boolean>, ObservableValue<Boolean>>() {

      @Override
      public ObservableValue<Boolean> call(CellDataFeatures<SessionInfo, Boolean> param) {
        return new SimpleBooleanProperty((boolean) param.getValue().getRules().getProperty(Properties.ALLOW_INVL_VOTES));
      }
    });
    
    activeSessionsTable.getColumns().setAll(playerAmnt, prisonDil, voteDuration, breakDuration, invalVotes);
    
    //add listeners to radio buttons for proper deselection
    setRadioButtonDeselectors();
    
    //set default create session values
    randomTeam.fire();
    noPrisDil.fire();
    noInvalVote.fire();
    yesLate.fire();
    
    //add row selection listerer
    activeSessionsTable.getSelectionModel().selectedItemProperty().addListener(new ChangeListener<SessionInfo>() {

      @Override
      public void changed(ObservableValue<? extends SessionInfo> observable, SessionInfo oldValue,
          SessionInfo newValue) {
        selectedSessionInfo = newValue;
        
        //enable team choice buttons
        randTeamChoice.setDisable(false);
        teamOneChoice.setDisable(false);
        teamTwoChoice.setDisable(false);
        
        //set default team choice
        randTeamChoice.fire();

        //enable join button
        joinSessionButton.setDisable(false);
      }
    });

    
    //get session list from server
    refreshButton.fire();

  }
  
  private void setRadioButtonDeselectors() {
    //team choice deselectors
    randomTeam.setOnAction(new EventHandler<ActionEvent>() {

      @Override
      public void handle(ActionEvent event) {
        teamOneChoice.setSelected(false);
        teamTwoChoice.setSelected(false);
      }
    });
    
    teamOneChoice.setOnAction(new EventHandler<ActionEvent>() {

      @Override
      public void handle(ActionEvent event) {
        randTeamChoice.setSelected(false);
        teamTwoChoice.setSelected(false);
      }
    });
    
    teamTwoChoice.setOnAction(new EventHandler<ActionEvent>() {

      @Override
      public void handle(ActionEvent event) {
        teamOneChoice.setSelected(false);
        randTeamChoice.setSelected(false);
      }
    });
    
    //create session - team choice
    teamOne.setOnAction(new EventHandler<ActionEvent>() {

      @Override
      public void handle(ActionEvent event) {
        teamTwo.setSelected(false);
        randomTeam.setSelected(false);
      }
    });
    
    teamTwo.setOnAction(new EventHandler<ActionEvent>() {

      @Override
      public void handle(ActionEvent event) {
        teamOne.setSelected(false);
        randomTeam.setSelected(false);
      }
    });
    
    randomTeam.setOnAction(new EventHandler<ActionEvent>() {

      @Override
      public void handle(ActionEvent event) {
        teamTwo.setSelected(false);
        teamOne.setSelected(false);
      }
    });
    
    //create session - pris. dil.
    yesPrisDil.setOnAction(new EventHandler<ActionEvent>() {

      @Override
      public void handle(ActionEvent event) {
        noPrisDil.setSelected(false);
        randPrisDil.setSelected(false);
      }
    });
    
    noPrisDil.setOnAction(new EventHandler<ActionEvent>() {

      @Override
      public void handle(ActionEvent event) {
        yesPrisDil.setSelected(false);
        randPrisDil.setSelected(false);
      }
    });
    
    randPrisDil.setOnAction(new EventHandler<ActionEvent>() {

      @Override
      public void handle(ActionEvent event) {
        noPrisDil.setSelected(false);
        yesPrisDil.setSelected(false);
      }
    });
    
    //create session - invalid votes
    yesInvalVote.setOnAction(new EventHandler<ActionEvent>() {

      @Override
      public void handle(ActionEvent event) {
        noInvalVote.setSelected(false);
        randInvalVote.setSelected(false);
      }
    });
    
    noInvalVote.setOnAction(new EventHandler<ActionEvent>() {

      @Override
      public void handle(ActionEvent event) {
        yesInvalVote.setSelected(false);
        randInvalVote.setSelected(false);
      }
    });
    
    randInvalVote.setOnAction(new EventHandler<ActionEvent>() {

      @Override
      public void handle(ActionEvent event) {
        yesInvalVote.setSelected(false);
        noInvalVote.setSelected(false);
      }
    });
    
    //create session - late join
    yesLate.setOnAction(new EventHandler<ActionEvent>() {

      @Override
      public void handle(ActionEvent event) {
        noLate.setSelected(false);
        randLate.setSelected(false);
      }
    });
    
    noLate.setOnAction(new EventHandler<ActionEvent>() {

      @Override
      public void handle(ActionEvent event) {
        yesLate.setSelected(false);
        randLate.setSelected(false);
      }
    });
    
    randLate.setOnAction(new EventHandler<ActionEvent>() {

      @Override
      public void handle(ActionEvent event) {
        noLate.setSelected(false);
        yesLate.setSelected(false);
      }
    });
  }
  
  @Override
  public void prepare() {
   
  }
  
  /**
   * Place all dev code here
   */
  private void uiTest() {
    //DEV_CODE: 
    System.out.println("---dummy sessions");
    SessionInfo info = new SessionInfo(new SessionRules(), UUID.randomUUID(), 15);
    activeSessions.add(info);
    activeSessMap.put(info.getSessionID(), info);
    
    activeSessionsTable.getItems().add(info);
  }
  
  private static GameBrowserController browserController;
  private static Pane pane;
  
  public static Pane createUI(ResourceManager resourceManager, ChessClient client) throws IOException {
    if (pane == null) {
      browserController = new GameBrowserController(client);    
      
      FXMLLoader uiLoader = new FXMLLoader();
      uiLoader.setController(browserController);
      pane = uiLoader.load(resourceManager.getResourceAsStream("browserUI"));
      
      browserController.init();
      
      //DEV_CODE: delete soon
      // browserController.uiTest();
      
      return pane;
    }
    return pane;
  }
  
  public static GameBrowserController getController() {
    return browserController;
  }
  
  public static Pane getPane() {
    return pane;
  }
}
