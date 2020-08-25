package jg.proj.chess.net;

public interface ServerResponses {
  
  //Server Error Responses
  public static final String BAD_REQUEST = "%s:ERROR:%d";
  
  public static final int UNKNOWN = -1; //unknown requests
  public static final int NO_SESS = -2; //no session 
  public static final int NO_ACCPT = -3; //session not accepting players
  public static final int BAD_VOTE = -4; //Vote is invalid
  public static final int NO_VOTE = -5; //voting for player's team isn't allowed at the moment
  public static final int WRONG_ARGS = -6; //given an insufficient amount of args, or args don't match expected type
  public static final int NOT_IN_SESS = -7; //player not in session  
  public static final int NO_JOIN = -8; //player can't join session
  public static final int PRISON_DIL = -9; //all and team msgs are forbidden due to prison dillemma
  public static final int NO_DESCISION = -10; //player's team hasn't decided on a move
  public static final int NO_TALLY = -11; //a tally cannot be formed as no votes have been recieved
  public static final int IN_SESS = -12; //certain requests cannot be made while the user is in a session
  public static final int BAD_ARGS = -13; //the arguments to the request are illegal/invalid
  
  //Server Signal messages
  public static final String SIGNAL = "signal";
  public static final String SIGNAL_MSG = SIGNAL+":%d";
  
  public static final int GAME_START = 0; //game has started
  
  public static final int VOTE_START = 1; //start of voting for player's team
  public static final int VOTE_END = 2; //end of voting for player's team
  
  public static final int TEAM1_WON = 3; //team 1 won!
  public static final int TEAM2_WON = 4; //team 2 won!
  
  public static final int TEAM1_DESS = 5; //team 1 has deserted during game!
  public static final int TEAM2_DESS = 6; //team 1 has deserted during game!
  
  public static final int TEAM1_TIED = 7; //team 1 is tied on a vote
  public static final int TEAM2_TIED = 8; //team 2 is tied on a vote
  
  public static final int TEAM1_NO_UNIT = 9; //team 1 has voted to move a non-existant unit
  public static final int TEAM2_NO_UNIT = 10; //team 2 has voted to move a non-existant unit
  
  public static final int TEAM1_OTHER_UNIT = 11; //team 1 has voted to move a unit that's not theirs
  public static final int TEAM2_OTHER_UNIT = 12; //team 2 has voted to move a unit that's not theirs

  //This signal is only for sessions with no filtering of bad votes
  public static final int TEAM1_IDIOT_VOTE = 13; //team 1 has voted on a move that's invalid
  public static final int TEAM2_IDIOT_VOTE = 14; //team 2 has voted on a move that's invalid
  
  public static final int TEAM1_NO_VOTE = 15; //no one from team 1 has voted
  public static final int TEAM2_NO_VOTE = 16; //no one from team 2 has voted
  
  public static final int PLAYER_JOINED = 17; //player has joined the session
  public static final int PLAYER_LEFT = 18; //player has left the session
    
  public static final int VOTE_RECIEVED = 20; //Someone from the player's team has voted. So, the vote tally has been updated
  
  public static final int BREAK_START = 21; //The game has started the break window in between turns. Take a rest
  public static final int BREAK_END = 22; //The game has ended the break window in between turns. Get up!

  
  //first string argument -> user-name , second string argument -> message
  public static final String ALL = "all";
  public static final String ALL_MSG = ALL+":%s:%s";
  
  public static final String TEAM = "team";
  public static final String TEAM_MSG = TEAM+":%s:%s";
  
  //string argument -> message
  public static final String SERV = "serv"; 
  public static final String SERVER_MSG = SERV+":%s"; 
  
  //Sent when a vote is decided. The arguments correspond to decided unit movement on the board
  public static final String RESULT = "result";
  public static final String RESULT_MSG = RESULT+":%d:%c:%d:%c";
  
  //sent every second during the voting window
  public static final String TIME = "time";
  public static final String TIME_MSG = TIME+":%d"; //where the integer argument is the amount of seconds left
  
  //sent every second during the break
  public static final String BREAK = "break";
  public static final String BREAK_MSG = BREAK+":%d";  //where the integer argument is the amount of seconds left
}
