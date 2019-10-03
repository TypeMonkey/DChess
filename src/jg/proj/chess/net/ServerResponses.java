package jg.proj.chess.net;

public interface ServerResponses {
  
  public static final String NO_SESSION = "Session '%s' could't be found!";
  
  public static final String BAD_REQ = "Bad Request: %s";
  
  public static final String INVALID_VOTE = "Bad vote: %c%d>%c%d";
  
  public static final String NOT_VOTING = "Not voting right now: %c%d>%c%d";
  
  public static final String BAD_ARGS = "The request %s needs %d , but recieved only %d"; 
}
