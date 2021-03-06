package jg.proj.chess.net.client.uis;
import javafx.scene.paint.Color;
import javafx.scene.paint.Paint;

import java.io.File;
import java.io.IOException;
import java.time.Duration;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.value.ObservableValue;
import javafx.event.ActionEvent;
import javafx.event.Event;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Pos;
import javafx.scene.Group;
import javafx.scene.control.Button;
import javafx.scene.control.ContentDisplay;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.RadioButton;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.control.Tooltip;
import javafx.scene.control.TableColumn.CellDataFeatures;
import javafx.scene.image.Image;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.Border;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.shape.Rectangle;
import javafx.scene.text.Font;
import javafx.scene.text.Text;
import javafx.scene.text.TextAlignment;
import javafx.util.Callback;
import jg.proj.chess.core.Board;
import jg.proj.chess.core.DefaultBoardPreparer;
import jg.proj.chess.core.Square;
import jg.proj.chess.core.units.InvalidMove;
import jg.proj.chess.net.ServerRequest;
import jg.proj.chess.net.ServerResponses;
import jg.proj.chess.net.SessionStatus;
import jg.proj.chess.net.client.ChessClient;
import jg.proj.chess.net.client.MessageListener;
import jg.proj.chess.net.client.RequestBody;
import jg.proj.chess.net.client.Reactor;
import jg.proj.chess.net.client.ResourceManager;
import jg.proj.chess.net.client.SignalListener;
import jg.proj.chess.net.client.VoteTally;
import jg.proj.chess.net.client.uis.components.GraphicalSquare;
import jg.proj.chess.net.client.uis.components.VoteChoice;

public class GameScreenController implements SignalListener, MessageListener, Preparable{
  
  public static final int DEFAULT_BOARD_SIZE = 8;
  public static final double CHAT_LIST_WRAP_WIDTH = 157;
  
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
  @FXML 
  private Label bottomStatusLabel;
  
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
  private ListView<Text> chatListDisplay;
  @FXML 
  private Button quitSessionButton;
  
  //--bottom right components
  @FXML
  private VBox chatInputVBox;
  @FXML 
  private TextArea chatInput;
  @FXML 
  private Button chatSendButton;
  @FXML
  private Button chatClearButton;
  @FXML
  private RadioButton teamChatButton;
  @FXML
  private RadioButton allChatButton;
  
  //actual chess board
  private final GraphicalSquare [][] visibleBoard;
  private final ChessClient client;
  private final ResourceManager resourceManager;
  
  //logic for chess board and game session
  private final Board board;
  private final VoteChoice voteChoice;
  private final Reactor plistReactor;
  
  //flags to keep track of game state
  private volatile boolean isPlayersTeamVoting;
  
  private GameScreenController(ChessClient client) { 
    this.client = client;
    this.resourceManager = client.getResourceManager();
    voteChoice = new VoteChoice();
    visibleBoard = new GraphicalSquare[DEFAULT_BOARD_SIZE][DEFAULT_BOARD_SIZE];
    board = new Board(DEFAULT_BOARD_SIZE, DEFAULT_BOARD_SIZE);
    
    plistReactor = new Reactor() {      
      @Override
      public void react(RequestBody request, String... results) {
        //clear both team lists
        teamOneList.getItems().clear();
        teamTwoList.getItems().clear();
        
        System.out.println("----PLIST: "+Arrays.toString(results));
        
        //iterate through user strings
        for (String userEntry : results) {
          String [] values = userEntry.split(",");
          String name = values[0];
          boolean isTeamOne = Boolean.parseBoolean(values[1]);
          String uuid = values[2];

          if (isTeamOne) {
            teamOneList.getItems().add(name+"@"+uuid);
          }
          else {
            teamTwoList.getItems().add(name+"@"+uuid);
          }
        }
      }      
      @Override
      public void error(RequestBody request, int errorCode) {
        
      }
    };
  }
  
  @SuppressWarnings("unchecked")
  public void init() {       
    //voteNowDisplay should be blank
    voteNowDisplay.setText("");
    
    //bottomStatusLabel should be blank
    bottomStatusLabel.setText("");
    bottomStatusLabel.setFont(Font.font("Segoe UI", 14));
    bottomStatusLabel.setStyle(
        "-fx-padding: 10;" + 
        "-fx-border-style: solid inside;" + 
        "-fx-border-width: 2;" +
        "-fx-border-insets: 5;" + 
        "-fx-border-radius: 3;" + 
        "-fx-border-color: grey;");
    
    //Set sessionUUID tooltip
    Tooltip sessUUIDInfo = new Tooltip("This is the unique ID for your session that "
                                     + "you can give to other players to join your game.");
    sessUUIDInfo.setWrapText(true);
    sessUUIDInfo.setStyle("-fx-font-size: 12");
    sessUUIDInfo.setPrefWidth(400);
    
    sessionUUIDDisplay.setTooltip(sessUUIDInfo);
    
    //set the session uuid
    sessionUUIDDisplay.setEditable(false);
    
    //set chat to sent to team by default
    teamChatButton.setOnAction(new EventHandler<ActionEvent>() {
      @Override
      public void handle(ActionEvent event) {
        allChatButton.setSelected(false);
      }
    });
    allChatButton.setOnAction(new EventHandler<ActionEvent>() {
      @Override
      public void handle(ActionEvent event) {
        teamChatButton.setSelected(false);
      }
    });
    
    //add handles for send and clear vote buttons
    sendVoteButton.setOnAction(new EventHandler<ActionEvent>() {

      @Override
      public void handle(ActionEvent event) {
        //make send request 
        Square fromSquare = voteChoice.getFromChoice();
        Square toSquare = voteChoice.getToChoice();
        RequestBody voteRequest = new RequestBody(ServerRequest.VOTE, 
            fromSquare.getFile(), 
            fromSquare.getRank(), 
            toSquare.getFile(), 
            toSquare.getRank());
        
        Reactor reactor = new Reactor() {        
          @Override
          public void react(RequestBody request, String... results) {
            String mess = "[SERVER] GOT YOUR VOTE: "+Arrays.stream(results).collect(Collectors.joining(""));
            Text text = new Text(mess);
            text.setWrappingWidth(chatListDisplay.getPrefWidth());
            chatListDisplay.getItems().add(text);
            voteNowDisplay.setText("VOTE RECIEVED!");
          }
          
          @Override
          public void error(RequestBody request, int errorCode) {
            voteNowDisplay.setText("BAD VOTE! Retry!");
          }
        };
        
        client.sendRequest(voteRequest, reactor);
      }
    });
    
    clearVoteButton.setOnAction(new EventHandler<ActionEvent>() {
      @Override
      public void handle(ActionEvent event) {

        Square fromSquare = voteChoice.getFromChoice();
        Square toSquare = voteChoice.getToChoice();
        
        System.out.println("********CLEARING VOTE SELECTION! "+" | "+fromSquare+" > "+toSquare);
        
        GraphicalSquare fromGSquare = visibleBoard[fromSquare.getFile() - 1][fromSquare.getRank() - 'A'];
        GraphicalSquare toGSquare = visibleBoard[toSquare.getFile() - 1][toSquare.getRank() - 'A'];
        
        fromGSquare.getOutline().setStroke(Color.BLACK);
        fromGSquare.getOutline().setFill(fromGSquare.getOrigialSquareColor());
        
        toGSquare.getOutline().setStroke(Color.BLACK);
        toGSquare.getOutline().setFill(toGSquare.getOrigialSquareColor());
        
        //reset the highlight of all the fromSquare's possible desinations
        Set<Square> highlighed = fromSquare.getUnit() == null ? new HashSet<>() : fromSquare.getUnit().possibleDestinations();
        
        
        System.out.println("----> RESET ALL: "+highlighed);
        
        for (Square square : highlighed) {
          GraphicalSquare targetGSquare = visibleBoard[square.getFile() - 1][square.getRank() - 'A'];
          targetGSquare.getOutline().setStroke(Color.BLACK);
          targetGSquare.getOutline().setFill(targetGSquare.getOrigialSquareColor());
        }
        
        //clear the actual votes
        voteChoice.setFromChoice(null);
        voteChoice.setToChoice(null);
        
        //disable send and clear vote buttons
        sendVoteButton.setDisable(true);
        clearVoteButton.setDisable(true);
      }
    });
    
    //handle for quitting session
    quitSessionButton.setOnAction(new EventHandler<ActionEvent>() {
      @Override
      public void handle(ActionEvent event) {
        // TODO Auto-generated method stub       
        client.sendRequest(new RequestBody(ServerRequest.QUIT), Reactor.BLANK_REACTOR);
        
        try {
          client.showBrowser();
        } catch (IOException e) {
          client.recordException(e);
        }
      }     
    });
    
    
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
    
    //add code for send chat button
    chatSendButton.setOnAction(new EventHandler<ActionEvent>() {
      @Override
      public void handle(ActionEvent event) {
        String message = chatInput.getText() == null ? "" : chatInput.getText();
        
        client.sendRequest(new RequestBody(teamChatButton.isSelected() ? ServerRequest.TEAM : ServerRequest.ALL, message), Reactor.BLANK_REACTOR);
        
        //now clear the chatInput textarea
        chatInput.clear();
      }
    });
    
    //add code for chatInput
    chatInput.setWrapText(true);
    chatInput.setOnKeyPressed(new EventHandler<KeyEvent>() {
      @Override
      public void handle(KeyEvent arg0) {
        if (arg0.getCode() == KeyCode.ENTER) {
          chatSendButton.fire();
          arg0.consume();
        }
        else if (arg0.getCode() == KeyCode.ALT) {
          if (teamChatButton.isSelected()) {
            teamChatButton.setSelected(false);
            allChatButton.setSelected(true);
          }
          else {
            allChatButton.setSelected(false);
            teamChatButton.setSelected(true);
          }
        }
      }
    });
    
    //add code for clear chat
    chatClearButton.setOnAction(new EventHandler<ActionEvent>() {
      @Override
      public void handle(ActionEvent event) {
        chatInput.clear();
      }
    }); 
        
    //set tallyVote columns
    TableColumn<VoteTally, String> voteColumn = new TableColumn<>("Vote");
    voteColumn.setCellValueFactory(new Callback<TableColumn.CellDataFeatures<VoteTally,String>, ObservableValue<String>>() {
      
      @Override
      public ObservableValue<String> call(CellDataFeatures<VoteTally, String> param) {
        VoteTally tally = param.getValue();     
        String fromSq = tally.getOldFile()+String.valueOf(tally.getOldRank());   
        String toSq = tally.getNewFile()+String.valueOf(tally.getNewRank());
        return new SimpleStringProperty(fromSq+"->"+toSq);
      }
    });
    
    //set vote count column
    TableColumn<VoteTally, Number> voteCountCol = new TableColumn<>("Vote Count");
    voteCountCol.setCellValueFactory(new Callback<TableColumn.CellDataFeatures<VoteTally,Number>, ObservableValue<Number>>() {

      @Override
      public ObservableValue<Number> call(CellDataFeatures<VoteTally, Number> param) {
        return new SimpleIntegerProperty(param.getValue().getVoteCount());
      }
    });
    
    voteTallyTable.getColumns().clear();
    voteTallyTable.getColumns().addAll(voteColumn, voteCountCol);
        
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
      
      //clear board first
      clearVoteButton.fire();
      
      //parse coordinates
      //NOTE: ranks start at 1.
      final int fromFile = Integer.parseInt(messageContent[0]);
      final char fromRank = messageContent[1].charAt(0);
      final int destFile = Integer.parseInt(messageContent[2]);
      final char destRank = messageContent[3].charAt(0);
      
      //update board
      
      //move units on the logical board
      Square fromSquare = board.querySquare(fromFile, fromRank);
      Square destSquare = board.querySquare(destFile, destRank);
      
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
      Paint unitFromSquare = visibleBoard[fromFile - 1][fromRank - 'A'].getPicture();
      
      //swap from and to images 
      visibleBoard[fromFile - 1][fromRank - 'A'].setPicture(Color.TRANSPARENT);
      visibleBoard[destFile - 1][destRank - 'A'].setPicture(unitFromSquare);
            
      //clear votes
      voteTallyTable.getItems().clear();
    }
    else if (messageType.equals(ServerResponses.BREAK)) {
      long secondsLeft = Long.parseLong(messageContent[0]);
      
      long minutesCalc = secondsLeft / 60;
      String minutes = minutesCalc >= 0 && minutesCalc <= 9 ? "0"+minutesCalc : String.valueOf(minutesCalc);
      long secondsCalc = secondsLeft % 60;
      String seconds = secondsCalc >= 0 && secondsCalc <= 9 ? "0"+secondsCalc : String.valueOf(secondsCalc);
      
      voteNowDisplay.setText("BREAK TIME LEFT: "+minutes+":"+seconds);
      voteNowDisplay.setTextFill(Color.CORNFLOWERBLUE);
    }
    else if (messageType.equals(ServerResponses.TIME)) {
      long secondsLeft = Long.parseLong(messageContent[0]);
      
      long minutesCalc = secondsLeft / 60;
      String minutes = minutesCalc >= 0 && minutesCalc <= 9 ? "0"+minutesCalc : String.valueOf(minutesCalc);
      long secondsCalc = secondsLeft % 60;
      String seconds = secondsCalc >= 0 && secondsCalc <= 9 ? "0"+secondsCalc : String.valueOf(secondsCalc);
      
      int votingTeamID = isPlayersTeamVoting ? client.getCurrentTeam() : (client.getCurrentTeam() == 1 ? 2 : 1);
      voteNowDisplay.setText("TEAM "+votingTeamID+" VOTE TIME: "+minutes+":"+seconds);
      voteNowDisplay.setTextFill(isPlayersTeamVoting ? Color.GREENYELLOW : Color.RED);
    }
    else if (messageType.equals(ServerResponses.SERV)) {
      updateChatList("[SERVER] "+Arrays.stream(messageContent).collect(Collectors.joining()), Color.PURPLE);
    }
    else {
      updateChatList("["+messageType+"] "+messageContent[0]+": "+
                     Arrays.stream(Arrays.copyOfRange(messageContent, 1, messageContent.length)).collect(Collectors.joining()));
    }
  }
  
  @Override
  public void handleSignal(int signal) {
    
    switch (signal) {
    case ServerResponses.GAME_START: 
    {
      //get player list
      RequestBody plistRequest = new RequestBody(ServerRequest.PLIST, true);
      client.sendRequest(plistRequest, plistReactor);
      bottomStatusLabel.setText(">>> GAME STARTED <<<");
      bottomStatusLabel.setTextFill(Color.PURPLE);
      System.out.println("----GAME STARTED SIGNAL RECEIVED: "+client.getUserName());
      break;
    }
    case ServerResponses.BREAK_START:
    {
      bottomStatusLabel.setText("Take a break! Cooldown! Look outside! Re-strategize!");
      bottomStatusLabel.setTextFill(Color.GREENYELLOW);
      break;
    }
    case ServerResponses.BREAK_END:
    {
      bottomStatusLabel.setText("GET UP! CALL THE BANNERS!");
      bottomStatusLabel.setTextFill(Color.RED);
      break;
    }
    case ServerResponses.VOTE_START:
    {
      //update voteNowDisplay
      isPlayersTeamVoting = true;
      bottomStatusLabel.setText("Your team is voting! Place a vote now!");
      bottomStatusLabel.setTextFill(Color.GREENYELLOW);
      break;
    }
    case ServerResponses.VOTE_END:
    {
      //update voteNowDisplay
      isPlayersTeamVoting = false;
      bottomStatusLabel.setText("Your team is done voting! Board updated!");
      bottomStatusLabel.setTextFill(Color.RED);
      break; 
    }
    case ServerResponses.TEAM1_WON:
    {
      String outcomeText = client.getCurrentTeam() == 1 ? "YOUR TEAM WON!" :
                                                          "YOUR TEAM LOST";
      Color outcomeFont = client.getCurrentTeam() == 1 ? Color.CORNFLOWERBLUE : Color.RED;
      //update voteNowDisplay and chat to show victory
      bottomStatusLabel.setText(outcomeText);
      bottomStatusLabel.setTextFill(outcomeFont);
      break;
    }
    case ServerResponses.TEAM2_WON:
    {
      String outcomeText = client.getCurrentTeam() == 2 ? "YOUR TEAM WON!" :
                                                          "YOUR TEAM LOST";
      Color outcomeFont = client.getCurrentTeam() == 2 ? Color.CORNFLOWERBLUE : Color.RED;
      //update voteNowDisplay and chat to show victory
      bottomStatusLabel.setText(outcomeText);
      bottomStatusLabel.setTextFill(outcomeFont); 
      break;
    }
    case ServerResponses.TEAM1_DESS:
    {
      //If client is Team 1, then they've already quit the screen.
      //No need to update team 1's UI then
      if (client.getCurrentTeam() == 2) {
        //update status to show defeat
        bottomStatusLabel.setText("Team 1 desserted the battle! Your team won!");
        bottomStatusLabel.setTextFill(Color.CORNFLOWERBLUE);
      }
      break;
    }
    case ServerResponses.TEAM2_DESS:
    {
      //If client is Team 2, then they've already quit the screen.
      //No need to update team 2's UI then
      if (client.getCurrentTeam() == 1) {
        //update status to show defeat
        bottomStatusLabel.setText("Team 2 desserted the battle! Your team won!");
        bottomStatusLabel.setTextFill(Color.CORNFLOWERBLUE);
      }
      break;
    }
    case ServerResponses.TEAM1_TIED:
    {
      //update voteNowDisplay and chat to show tied status
      String message = client.getCurrentTeam() == 1 ? 
                       "Your time is tied on a move! No move is made!" : 
                       "Team 1 is tied on a move! No move is made!";
      Color outcomeFont = client.getCurrentTeam() == 2 ? Color.CORNFLOWERBLUE : Color.RED;

      bottomStatusLabel.setText(message);
      bottomStatusLabel.setTextFill(outcomeFont);
      break;
    }
    case ServerResponses.TEAM2_TIED:
    {
      //update voteNowDisplay and chat to show tied status
      String message = client.getCurrentTeam() == 2 ? 
          "Your time is tied on a move! No move is made!" : 
          "Team 2 is tied on a move! No move is made!";
      Color outcomeFont = client.getCurrentTeam() == 1 ? Color.CORNFLOWERBLUE : Color.RED;

      bottomStatusLabel.setText(message);
      bottomStatusLabel.setTextFill(outcomeFont);
      break;
    }
    case ServerResponses.TEAM1_OTHER_UNIT:
    {
      String message = client.getCurrentTeam() == 1 ? 
                 "Your team voted to move a unit that's not theirs. No move made!" : 
                 "Team One voted to move one of your team's units. No move made!";
      Color messColor = client.getCurrentTeam() == 1 ? Color.RED : Color.CORNFLOWERBLUE;
      bottomStatusLabel.setText(message);
      bottomStatusLabel.setTextFill(messColor);
      break;
    }
    case ServerResponses.TEAM2_OTHER_UNIT:
    {  
      String message = client.getCurrentTeam() == 2 ? 
                "Your team voted to move a unit that's not theirs. No move made!" : 
                "Team Two voted to move one of your team's units. No move made!";
      Color messColor = client.getCurrentTeam() == 2 ? Color.RED : Color.CORNFLOWERBLUE;
      bottomStatusLabel.setText(message);
      bottomStatusLabel.setTextFill(messColor);
      break;
    }
    case ServerResponses.TEAM1_IDIOT_VOTE:
    {      
      //update chatlist
      String message = client.getCurrentTeam() == 1 ? 
                "Your team voted on an illegal move. No move made!" : 
                "Team One voted on an illegal move. No move made!";
      Color messColor = client.getCurrentTeam() == 1 ? Color.RED : Color.CORNFLOWERBLUE;
      bottomStatusLabel.setText(message);
      bottomStatusLabel.setTextFill(messColor);
      break;
    }
    case ServerResponses.TEAM2_IDIOT_VOTE:
    {   
      //update chatlist
      String message = client.getCurrentTeam() == 2 ? 
                "Your team voted on an illegal move. No move made!" : 
                "Team Two voted on an illegal move. No move made!";
      Color messColor = client.getCurrentTeam() == 2 ? Color.RED : Color.CORNFLOWERBLUE;
      bottomStatusLabel.setText(message);
      bottomStatusLabel.setTextFill(messColor);
      break;
    }
    case ServerResponses.TEAM1_NO_VOTE:
    {    
      //update chatlist
      String message = client.getCurrentTeam() == 1 ? 
                "Your team sent no votes at all! No move made!" : 
                "Team One sent no votes at all! No move made!";
      Color messColor = client.getCurrentTeam() == 1 ? Color.RED : Color.CORNFLOWERBLUE;
      bottomStatusLabel.setText(message);
      bottomStatusLabel.setTextFill(messColor);
      break;
    }
    case ServerResponses.TEAM2_NO_VOTE:
    {  
      //update chatlist
      String message = client.getCurrentTeam() == 2 ? 
                "Your team sent no votes at all! No move made!" : 
                "Team One sent no votes at all! No move made!";
      Color messColor = client.getCurrentTeam() == 2 ? Color.RED : Color.CORNFLOWERBLUE;
      bottomStatusLabel.setText(message);
      bottomStatusLabel.setTextFill(messColor);
      break;
    } 
    case ServerResponses.PLAYER_JOINED:
    {
      //get player list
      RequestBody plistRequest = new RequestBody(ServerRequest.PLIST, true);
      client.sendRequest(plistRequest, plistReactor);
      break;
    }
    case ServerResponses.PLAYER_LEFT:
    {
      //get player list
      RequestBody plistRequest = new RequestBody(ServerRequest.PLIST, true);
      client.sendRequest(plistRequest, plistReactor);
      break;
    }
    case ServerResponses.VOTE_RECIEVED:
    {
      //request TALLY 
      Reactor tallyReactor = new Reactor() {
        
        //make sure to mark off 
        
        @Override
        public void react(RequestBody request, String... results) {
          // TODO Auto-generated method stub
          voteTallyTable.getItems().clear();
          for (String vote : results) {
            String [] split = vote.split(">");
            
            String fromSquare = split[0];
            int fromFile = Integer.parseInt(String.valueOf(fromSquare.charAt(0)));
            char fromRank = fromSquare.charAt(1);
            
            String toSquare = split[1];
            int toFile = Integer.parseInt(String.valueOf(toSquare.charAt(0)));
            char toRank = toSquare.charAt(1);
            
            int voteCnt = Integer.parseInt(split[2]);
            
            voteTallyTable.getItems().add(new VoteTally(fromFile, fromRank, toFile, toRank, voteCnt));
          }
        }
        
        @Override
        public void error(RequestBody request, int errorCode) {
          client.recordException("TALLY REQUEST FAILED: "+errorCode);
        }
      };
      
      client.sendRequest(new RequestBody(ServerRequest.TALLY), tallyReactor);
      break;
    }
    }
  }
  
  private void updateChatList(String message) {
    updateChatList(message, Color.BLACK);
  }
  
  private void updateChatList(String message, Color fontColor) {
    Text text = new Text(message);
    text.setStroke(fontColor);
    text.setWrappingWidth(CHAT_LIST_WRAP_WIDTH);
    chatListDisplay.getItems().add(text);
    chatListDisplay.scrollTo(text);
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
    
    boolean colGreyOut = true;
    for(int r = 0; r<8; r++) {
      HBox colFixer = new HBox(3);
      
      final Label label = new Label(String.valueOf(r + 1));
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
      
      boolean rowGreyOut = !colGreyOut;
      for(int c = 0; c<8; c++) {
        Rectangle outlineSquare = new Rectangle(46, 46);
        outlineSquare.setStroke(Color.BLACK);            
        final Color squareColor = rowGreyOut ? Color.GREY : Color.TRANSPARENT;        
        outlineSquare.setFill(squareColor);
        rowGreyOut = !rowGreyOut;       
        outlineSquare.setStrokeWidth(3);
        
        Rectangle frameSquare = new Rectangle(46, 46);
        frameSquare.setStroke(Color.TRANSPARENT);
        frameSquare.setFill(Color.TRANSPARENT);
        
        StackPane squarePane = new StackPane();
        squarePane.setAlignment(Pos.CENTER);
        squarePane.setPrefSize(46, 46);
        squarePane.setMinSize(Region.USE_PREF_SIZE, Region.USE_PREF_SIZE);
        squarePane.setMaxSize(Region.USE_PREF_SIZE, Region.USE_PREF_SIZE);
             
        squarePane.getChildren().addAll(outlineSquare, frameSquare);
        
        final int squareFile = r + 1;
        final char squareRank = (char) (c + 'A');
        squarePane.setOnMouseClicked( new EventHandler<Event>() {
          @Override
          public void handle(Event event) {
            System.out.println("---SQUARE CLICKED!! board greying!");

            if (voteChoice.isVoteComplete()) {
              /*
               * New vote is being made. Reset the board
               */
              clearVoteButton.fire();
            }
            
            if (voteChoice.getFromChoice() == null) {
              Square pickedSquare = board.querySquare(squareFile, squareRank);
              System.out.println("---first choice!!!"+pickedSquare+" | "+pickedSquare.getUnit().getType());
              voteChoice.setFromChoice(pickedSquare);
              //highlight this square as green
              outlineSquare.setStroke(Color.GREEN);
              
              Set<Square> possibleChoices = pickedSquare.getUnit() != null ? 
                                            pickedSquare.getUnit().possibleDestinations() : 
                                            new HashSet<Square>();
              
              for (Square choiceSquare : possibleChoices) {
                GraphicalSquare choiceGSquare = visibleBoard[choiceSquare.getFile() - 1][choiceSquare.getRank() - 'A'];
                choiceGSquare.getOutline().setFill(Color.LIGHTGREEN);
                choiceGSquare.getOutline().setStroke(Color.GREEN);
              }
            }
            else if (voteChoice.getToChoice() == null) {
              voteChoice.setToChoice(board.querySquare(squareFile, squareRank));
              //highlight this square as blue
              outlineSquare.setStroke(Color.BLUE);
              //then, enable send vote and clear vote buttons
              sendVoteButton.setDisable(false);
              clearVoteButton.setDisable(false);
            }
            
          }         
        });
        
        
        visibleBoard[r][c] = new GraphicalSquare(squarePane, outlineSquare, frameSquare, squareColor);
        
        colFixer.getChildren().add(squarePane);
      }
      
      colGreyOut = !colGreyOut;
      
      rowFixer.getChildren().add(colFixer);
    }    
    
    chessBoardPane.setAlignment(Pos.CENTER);    
    chessBoardPane.getChildren().add(group);
    
    
    //set chess pieces manually    
    try {
      //white piece
      visibleBoard[0][0].setPicture(new Image(resourceManager.getResourceAsStream("rookWhite")));
      visibleBoard[0][1].setPicture(new Image(resourceManager.getResourceAsStream("knightWhite")));
      visibleBoard[0][2].setPicture(new Image(resourceManager.getResourceAsStream("bishopWhite")));
      visibleBoard[0][3].setPicture(new Image(resourceManager.getResourceAsStream("queenWhite")));
      visibleBoard[0][4].setPicture(new Image(resourceManager.getResourceAsStream("kingWhite")));
      visibleBoard[0][5].setPicture(new Image(resourceManager.getResourceAsStream("bishopWhite")));
      visibleBoard[0][6].setPicture(new Image(resourceManager.getResourceAsStream("knightWhite")));
      visibleBoard[0][7].setPicture(new Image(resourceManager.getResourceAsStream("rookWhite")));

      for(int c = 0; c < 8; c++) {
        visibleBoard[1][c].setPicture(new Image(resourceManager.getResourceAsStream("pawnWhite")));
      }
      
      //black pieces
      visibleBoard[7][0].setPicture(new Image(resourceManager.getResourceAsStream("rookBlack")));
      visibleBoard[7][1].setPicture(new Image(resourceManager.getResourceAsStream("knightBlack")));
      visibleBoard[7][2].setPicture(new Image(resourceManager.getResourceAsStream("bishopBlack")));
      visibleBoard[7][3].setPicture(new Image(resourceManager.getResourceAsStream("queenBlack")));
      visibleBoard[7][4].setPicture(new Image(resourceManager.getResourceAsStream("kingBlack")));
      visibleBoard[7][5].setPicture(new Image(resourceManager.getResourceAsStream("bishopBlack")));
      visibleBoard[7][6].setPicture(new Image(resourceManager.getResourceAsStream("knightBlack")));
      visibleBoard[7][7].setPicture(new Image(resourceManager.getResourceAsStream("rookBlack")));

      for(int c = 0; c < 8; c++) {
        visibleBoard[6][c].setPicture(new Image(resourceManager.getResourceAsStream("pawnBlack")));
      }

    } catch (IOException e) {
     client.recordException(e);
    }
    
  }
  
  @Override
  public void prepare() {
    sessionUUIDDisplay.setText(client.getCurrentSession().getSessionID().toString());
    teamChatButton.fire();
    
    //disable send and clear vote buttons by default
    sendVoteButton.setDisable(true);
    clearVoteButton.setDisable(true);
    
    //prepare logical board
    board.initialize(new DefaultBoardPreparer());
    
    //makes the board
    makeBoard();
    
    //send plist request
    client.sendRequest(new RequestBody(ServerRequest.PLIST, true), plistReactor);
    
    //get session status
    Reactor statusReactor = new Reactor() {      
      @Override
      public void react(RequestBody request, String... results) {
        String status = results[1];
        SessionStatus sessionStatus = SessionStatus.valueOf(status);
        if (sessionStatus == SessionStatus.PLAYING) {
          voteNowDisplay.setText(">>> GAME STARTED <<<");
          voteNowDisplay.setTextFill(Color.PURPLE);
        }
        else {
          System.out.println("---GAME STATUS from STATUS REQ: "+sessionStatus);
        }
      }
      
      @Override
      public void error(RequestBody request, int errorCode) {
        client.recordException("ERROR: couldn't find session to get status");
      }
    };
    
    System.out.println("---CURRENT SESSION UUID: "+client.getCurrentSession().getSessionID());
    RequestBody request = new RequestBody(ServerRequest.STATUS, client.getCurrentSession().getSessionID().toString());
    
    client.sendRequest(request, statusReactor); 
    
  }
  
  /**
   * Place all dev code here
   */
  private void uiTest() {
    //DEV_CODE: 
    for(int i = 0 ; i < 100; i++) {
      updateChatList("HELLO WORLD!!! "+i+" hello world hello world hello world", 
                      (i % 2) == 0 ? Color.GREENYELLOW : Color.RED);
    }
  }

  private static GameScreenController screenController;
  private static Pane pane;
  
  public static Pane createUI(ResourceManager resourceManager, ChessClient client) throws IOException {
    if (pane == null) {
      screenController = new GameScreenController(client);    
      
      FXMLLoader uiLoader = new FXMLLoader();
      uiLoader.setController(screenController);
      pane = uiLoader.load(resourceManager.getResourceAsStream("screenUI"));
      
      screenController.init();
      
      //DEV_CODE: delete soon
      //screenController.uiTest();
      
      return pane;
    }
    return pane;
  }
  
  public static GameScreenController getController() {
    return screenController;
  }
  
  public static Pane getPane() {
    return pane;
  }
}
