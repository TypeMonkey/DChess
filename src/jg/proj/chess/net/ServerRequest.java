package jg.proj.chess.net;

import java.util.Arrays;

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
 *       
 *       Then, the Server sends a TEAM/ALL response to all members of the A's team/session containing
 *       A's message - include A themself. 
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
  JOIN("join","~join:%s:%d", 2,"Joins a session with the provided UUID",
        ArgType.STRING, ArgType.INTEGER),
  
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
   * Returns: a string of the form: "sessionUUID:isTeamOne"
   *          where CONFIG is the argument string provided with csess
   */
  CSESS("csess", "~csess:%d:"
      + "PRISON_DILEMMA=%b:"
      + "VOTING_DURATION=%d:"
      + "MIN_TEAM_COUNT=%d:"
      + "ALLOW_INVL_VOTES=%b:"
      + "ALLOW_JOINS_GAME=%b:"
      + "BREAK_AMOUNT=%d", 6,"Creates a session",
      ArgType.INTEGER, ArgType.BOOLEAN, ArgType.INTEGER, ArgType.INTEGER, ArgType.BOOLEAN, ArgType.BOOLEAN, ArgType.INTEGER),
  
  /**
   * Requests the current status of a session
   * 
   * Argument is the session's UUID
   * 
   * Returns: status:sessionUUID:STATUS <- where STATUS is the string representation of a SessionStatus instance
   */
  STATUS("status", "~status:%s", 1, "Requests the current status of a session" ,ArgType.STRING),
  
  /**
   * Requests the most recent string representation of the current game's baord
   * 
   * Returns: the String that represents the board's most current state
   *          or BAD_REQ if not in a session
   */
  UPDATE("update","~update", 0,"Requests the most recent string representation of the current game's board"),
  
  /**
   * Changes the username of the player
   * Returns: the string "NEW_USER_NAME:UUID_OF_PLAYER"
   */
  CUSER("cuser", "~cuser:%s", 1,"Changes the username of the player", 
      ArgType.STRING),
  
  /**
   * Votes for a move
   * Returns: the exact same vote as confirmation, 
   * or INVALID_VOTE if move isn't valid of any unit in the team,
   * or NOT_VOTING team isn't voting at the moment, 
   * or BAD_REQ user isn't in a session
   */
  VOTE("vote", "~vote:%d:%c:%d:%c", 4, "Votes for a move",
      ArgType.INTEGER, ArgType.CHAR, ArgType.INTEGER, ArgType.CHAR),
  
  /**
   * Requests the name, their current team - and optionally their UUIDs - of all players in the session
   * Returns: The player list of the current session, and their UUIDs if passed with a true
   *          The string is formatted as such: user1,user1.isTeamOne:user2,user2.isTeamOne: .... etc..
   *           or if desired for UUIDs: user1,user1.isTeamOne,user1.UUID:user2,user2.isTeamOne,user2.UUID: .... etc..
   *           
   *      If client isn't in a session, then BAD_REQ is returned
   */
  PLIST("plist", "~plist:%b", 1, "Requests the name, their current team - and optionally their UUIDs - of all players in the session",
      ArgType.BOOLEAN),
  
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
  ALL("all", "~all:%s", 1, "Sends a message to all players in the session",
      ArgType.STRING),
  
  /**
   * Sends a message to all players in the sender's team
   * 
   * Returns: The exact same request: "team:MESSAGE"
   *          or BAD_REQ if sender isn't in a session, or Prisoner's Dilemma is in session
   */
  TEAM("team", "~team:%s", 1, "Sends a message to all players in the sender's team",
      ArgType.STRING),
  
  /**
   * Requests the current list of active sessions the server is hosting
   * 
   * Returns: ses:session1UUID,session1PlayerAmnt,session1PrisonersDilemma,voteDuration,invalidVoting:session2UUID,session2PlayerAmnt,session2PrisonersDilemma,voteDuration,invalidVoting
   */
  SES("ses", "~ses", 0 , "Requests the current list of active sessions the server is hosting"),
  
  /**
   * Requests a tally of all of the player's current
   * 
   * Returns: "tally:ORIGINAL_SQUARE_COORDINATE > DEST_SQUARE_COORDINATE > VOTE_AMNT : .... other votes
   * 
   * If a session has enforced Prisoner's Dilemma, an error code is sent (PRISON_DIL)
   */
  TALLY("tally", "~tally", 0, "Requests a tally of all the player's current votes");
  
  private final String requestName;
  private final String formatedString;
  private final int argAmnt;
  private final String description;
  private final ArgType [] argTypes;
  
  private ServerRequest(String requestName, String formattedString, int argAmnt, String description){
    this(requestName, formattedString, argAmnt, description, new ArgType[0]);
  }
  
  private ServerRequest(String requestName, String formattedString, int argAmnt, String description, ArgType ... types){
    this.requestName = requestName;
    this.formatedString = formattedString;
    this.argAmnt = argAmnt;
    this.description = description;
    this.argTypes = types;
    
    if (argAmnt != argTypes.length) {
      //sanity check
      throw new IllegalArgumentException("Argument types not matching type expected");
    }
  }
  
  /**
   * Returns the name of this request, without the '~'
   * @return the name of this request, without the '~'
   */
  public String getName() {
    return requestName;
  }
  
  /**
   * Returns the name of this request, with the '~'
   * @return the name of this request, with the '~'
   */
  public String getReqName() {
    return "~"+requestName;
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
  
  /**
   * Returns the expected argument types of this server request
   * @return
   */
  public ArgType[] getArgTypes() {
    return argTypes.clone();
  }
  
  public String createErrorString(int errorCode){
    return String.format(ServerResponses.BAD_REQUEST, requestName, errorCode);
  }
  
  /**
   * Adds the provided arguments to this request's formatted string.
   * @param args - the arguments
   * @return the string with added arguments, or null if the amount of arguments
   *         provided and required don't match or don't match the expected types
   */
  public String addArguments(Object ... args){
    if (args.length != argAmnt) {
      return null;
    }
    else {
      args = ArgType.convertStringArgs(ArgType.getStringRep(args), argTypes);
      if (args == null) {
        return null;
      }
      return String.format(formatedString, args);
    }
  }
}

