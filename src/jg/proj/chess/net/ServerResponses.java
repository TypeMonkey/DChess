package jg.proj.chess.net;

public interface ServerResponses {
  //Server responses to Requests
  public static final String BAD_REQ = "%s:ERROR:%s";
  
  public static final String UNKNOWN_REQ = String.format(BAD_REQ, "!", "%s");
  
  public static final String NO_SESSION = "Session '%s' could't be found!";
  
  public static final String NOT_ACCEPT = "Session '%s' is no longer accepting players!";
    
  public static final String INVALID_VOTE = "Bad vote: %c%d>%c%d";
    
  public static final String NOT_VOTING = "Not voting right now: %c%d>%c%d";
  
  public static final String BAD_ARGS = "The request %s needs %d , but recieved only %d"; 
  
  //Sent to clients that send a server request only valid while in a game session
  //ex: ally chat, quit, or all chat
  public static final String NOT_IN_SESSION = "Not in a session";
  
  //Formatted strings that can be parsed by clients  
  public static final String GEN_MESS = "gen:%s";  
  
  //first string argument -> user-name , second string argument -> message
  public static final String ALL_MESS = "all:%s:%s";
  
  public static final String TEAM_MESS = "team:%s:%s";
  
  //voting server messages
  
  public static final String VOTE_NOW = "vote_now";
  
  public static final String VOTE_DONE = "vote_ended";
}
