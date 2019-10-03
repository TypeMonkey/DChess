package jg.proj.chess.net;


public enum ServerRequests{
  
  /**
   * Joins a session with the provided UUID and team ID
   * 
   * if the team ID if 1 or 2, then the player will join the respective team ID
   * else, the player will join team 1 or 2 randomly
   * 
   * Returns: a string of the form: "sessionUUID:isTeamOne"
   *          or NO_SESSION if a session of the given UUID couldn't be found
   */
  JOIN("~join:%s:%d", 2,"Joins a session with the provided UUID"),
  
  /**
   * Creates a session
   * 
   * The first integer argument is the teamID of the player whose creating the session
   * if the team ID if 1 or 2, then the player will join the respective team ID
   * else, the player will join team 1 or 2 randomly
   * 
   * The second argument is a long and represents the amount of seconds alloted
   * for voting per team. If this argument is <= 0, the session will use the default 
   * voting time of 15 seconds
   * 
   * Returns: a string of the form: "sessionUUID:isTeamOne"
   */
  CSESS("~csess:%d:%d", 2,"Creates a session"),
  
  /**
   * Requests the most recent string representation of the current game's board
   * Returns: the String that represents the board's most current state
   *          or BAD_REQ if not in a session
   */
  UPDATE("~update", 0,"Requests the most recent string representation of the current game's board"),
  
  /**
   * Changes the username of the player
   * Returns: the new username as confirmation
   */
  CUSER("~cuser:%s", 1,"Changes the username of the player"),
  
  /**
   * Votes for a move
   * Returns: the exact same vote as confirmation, 
   * or INVALID_VOTE if move isn't valid of any unit in the team,
   * or NOT_VOTING team isn't voting at the moment, 
   * or BAD_REQ user isn't in a session
   */
  VOTE("~vote:%c%d>%c%d", 4, "Votes for a move"),
  
  /**
   * Requests the name, their current team - and optionally their UUIDs - of all players in the session
   * Returns: The player list of the current session, and their UUIDs if passed with a true
   *          The string is formatted as such: user1,user1.isTeamOne:user2,user2.isTeamOne: .... etc..
   *           or if desired for UUIDs: user1,user1.isTeamOne,user1.UUID:user2,user2.isTeamOne,user2.UUID: .... etc..
   *           
   *      If client isn't in a session, then BAD_REQ is returned
   */
  PLIST("~plist:%b", 1, "Requests the name, their current team - and optionally their UUIDs - of all players in the session"),
  
  /**
   * Quits the current session
   * Returns: nothing. Assume that once this request is sent, the player is disconnected from the server
   */
  QUIT("~quit", 0 ,"Quits the current session");
  
  private final String formatedString;
  private final int argAmnt;
  private final String description;
  
  private ServerRequests(String formattedString, int argAmnt, String description){
    this.formatedString = formattedString;
    this.argAmnt = argAmnt;
    this.description = description;
  }
  
  public String getFormatString(){
    return formatedString;
  }
  
  public String getDescription(){
    return description;
  }
  
  public int argAmount() {
    return argAmnt;
  }
  
  public String addArguments(Object ... args){
    if (args.length != argAmnt) {
      return null;
    }
    return String.format(formatedString, args);
  }
    
  /*
  public static interface ServerRequests {
    
    public static final String UPDATE = "~update";
    public static final String CUSER = "~cuser:%s";
    public static final String VOTE = "~vote:%c%d>%c%d";
    public static final String PLIST = "~plist";
    public static final String QUIT = "~quit";
    
  }
  */
}

