package jg.proj.chess.net;

public interface ServerResponses {
  
  //Server Error Responses
  public static final String BAD_REQUEST = "%s:ERROR:%d";
  
  public static final int UNKNOWN = -1; //unknown requests
  public static final int NO_SESS = -2; //no session 
  public static final int NO_ACCPT = -3; //session not accepting players
  public static final int BAD_VOTE = -4; //Vote is invalid
  public static final int NO_VOTE = -5; //voting for player's team isn't allowed at the moment
  public static final int WRONG_ARGS = -6; //given an insufficient amount of args, or args don't match
  public static final int NOT_IN_SESS = -7; //player not in session  
  public static final int NO_JOIN = -8; //player can't join session
  public static final int PRISON_DIL = -9; //all and team msgs are forbidden due to prison dillemma
  
  //Server Signal messages
  public static final String SIGNAL = "signal:%d";
  
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
  
  public static final int TURN_END = 19; //a team's turn has ended
  
  public static final int VOTE_RECIEVED = 20; //Someone from the player's team has voted. So, the vote tally has been updated
  
  //first string argument -> user-name , second string argument -> message
  public static final String ALL_MSG = "all:%s:%s";
  
  public static final String TEAM_MSG = "team:%s:%s";
  
  //string argument -> message
  public static final String SERVER_MSG = "serv:%s"; 
}
