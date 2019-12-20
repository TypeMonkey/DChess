package jg.proj.chess.net;

/**
 * A collection of supported requests 
 * that can be processed by a DChess server.
 * 
 * Note: All responses are prepended with their request name (even when an error occurs)
 *       ex: If a csess request was sent, the server will respond with "csess:RESULTS_SUBSTRING".
 *           If a join request was sent with an invalid UUID, the server will send back: "join:ERROR:ERROR_MESSAGE"
 *           
 *       This note doesn't apply to QUIT.
 *       
 * Note: The server will send to clients server responses that it never requested.
 *       These requests are called "Voluntary".
 *       
 *       Such requests are TEAM and ALL. They are sent when a new message is sent by a 
 *       team member or someone in session , respectively.
 *       
 *       Say Player A is connected to a session and is in Team 1. They then send a TEAM/ALL request.
 *       Naturally, the server responds with the same TEAM/ALL request to Player A to acknowledge that 
 *       the Player A's message was received. 
 *       
 *       Then, the Server sends a TEAM/ALL response to all members of the A's team/session containing
 *       A's message - include A themself. This means that A receives their own message twice. It's important to filter
 *       appropriately for this.
 * @author Jose
 */
public enum ServerRequest{
  
  /**
   * Joins a session with the provided UUID and team ID
   * 
   * if the team ID if 1 or 2, then the player will join the respective team ID
   * else, the player will join team 1 or 2 randomly
   * 
   * Returns: a string of the form: "sessionUUID:isTeamOne:CONFIG"
   *          where CONFIG is the configuration of the session as dictated
   *          by the original csess request that created it,
   *          or NO_SESSION if a session of the given UUID couldn't be found
   */
  JOIN("join","~join:%s:%d", 2,"Joins a session with the provided UUID"),
  
  /**
   * Creates a session
   * 
   * Look into jg.proj.chess.net.Properties as each
   * argument is specified by each enum.
   * 
   * teamID (the first integer argument) should 1 or 2 is player wants to be in Team 1 ("white")
   * or Team 2 ("black") respectively. If teamID isn't either 1 or 2, their
   * team designation will be randomly decided
   * 
   * Returns: a string of the form: "sessionUUID:isTeamOne:CONFIG"
   *          where CONFIG is the argument string provided with csess
   */
  CSESS("csess", "~csess:%d:"
      + "PRISON_DILEMMA=%b:"
      + "VOTING_DURATION=%d:"
      + "MIN_TEAM_COUNT=%d:"
      + "ALLOW_INVL_VOTES=%b:"
      + "ALLOW_JOINS_GAME=%b", 6,"Creates a session"),
  
  /**
   * Requests the most recent string representation of the current game's state
   * 
   * Returns: the String that represents the board's most current state
   *          or BAD_REQ if not in a session
   */
  UPDATE("update","~update", 0,"Requests the most recent string representation of the current game's board"),
  
  /**
   * Changes the username of the player
   * Returns: the string "NEW_USER_NAME:UUID_OF_PLAYER"
   */
  CUSER("cuser", "~cuser:%s", 1,"Changes the username of the player"),
  
  /**
   * Votes for a move
   * Returns: the exact same vote as confirmation, 
   * or INVALID_VOTE if move isn't valid of any unit in the team,
   * or NOT_VOTING team isn't voting at the moment, 
   * or BAD_REQ user isn't in a session
   */
  VOTE("vote", "~vote:%c%d>%c%d", 4, "Votes for a move"),
  
  /**
   * Requests the name, their current team - and optionally their UUIDs - of all players in the session
   * Returns: The player list of the current session, and their UUIDs if passed with a true
   *          The string is formatted as such: user1,user1.isTeamOne:user2,user2.isTeamOne: .... etc..
   *           or if desired for UUIDs: user1,user1.isTeamOne,user1.UUID:user2,user2.isTeamOne,user2.UUID: .... etc..
   *           
   *      If client isn't in a session, then BAD_REQ is returned
   */
  PLIST("plist", "~plist:%b", 1, "Requests the name, their current team - and optionally their UUIDs - of all players in the session"),
  
  /**
   * Quits the current session
   * Returns: The same request, or error if the player isn't in a session
   */
  QUIT("quit", "~quit", 0 ,"Quits the current session"),
  
  /**
   * Disconnects from the server
   * Returns: nothing. List "quit", assume that once this request is sent, the player is disconnected from the server
   */
  DISC("disc", "~disc", 0 , "Disconnects from the server"),
  
  /**
   * Sends a message to all players in the session
   * 
   * Returns: The exact same request: "all:MESSAGE" 
   *          or BAD_REQ if sender isn't in a session, or Prisoner's Dilemma is in session
   */
  ALL("all", "~all:%s", 1, "Sends a message to all players in the session"),
  
  /**
   * Sends a message to all players in the sender's team
   * 
   * Returns: The exact same request: "team:MESSAGE"
   *          or BAD_REQ if sender isn't in a session, or Prisoner's Dilemma is in session
   */
  TEAM("team", "~team:%s", 1, "Sends a message to all players in the sender's team"),
  
  /**
   * Requests the current list of active sessions the server is hosting
   * 
   * Returns: ses:session1UUID,session1PlayerAmnt,session1PrisonersDilemma:session2UUID,session2PlayerAmnt,session2PrisonersDilemma
   */
  SES("ses", "~ses", 0 , "Requests the current list of active sessions the server is hosting");
  
  private final String requestName;
  private final String formatedString;
  private final int argAmnt;
  private final String description;
  
  private ServerRequest(String requestName, String formattedString, int argAmnt, String description){
    this.requestName = requestName;
    this.formatedString = formattedString;
    this.argAmnt = argAmnt;
    this.description = description;
  }
  
  /**
   * Returns the name of this request, without the '~'
   * @return the name of this request, without the '~'
   */
  public String getName() {
    return requestName;
  }
  
  /**
   * Returns the formatted string for this request
   * @return the formatted string for this request
   */
  public String getFormatString(){
    return formatedString;
  }
  
  /**
   * Returns the description of this request
   * @return the textual description of this request
   */
  public String getDescription(){
    return description;
  }
  
  /**
   * Returns the expected amount of arguments for this request
   * @return the expected amount of arguments for this request
   */
  public int argAmount() {
    return argAmnt;
  }
  
  public String createErrorString(int errorCode){
    return String.format(ServerResponses.BAD_REQUEST, requestName, errorCode);
  }
  
  /**
   * Adds the provided arguments to this request's formatted string.
   * @param args - the arguments
   * @return the string with added arguments, or null if the amount of arguments
   *         provided and required don't match
   */
  public String addArguments(Object ... args){
    if (args.length != argAmnt) {
      return null;
    }
    return String.format(formatedString, args);
  }
}

