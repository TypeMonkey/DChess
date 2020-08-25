package jg.proj.chess.net.client;

import java.util.UUID;

import jg.proj.chess.net.SessionRules;

/**
 * Holds information about a session's status and rules
 * @author Jose
 */
public class SessionInfo {
  
  private final SessionRules rules;
  private final UUID sessionID;
  private final int playerAmnt;
  
  /**
   * Constructs a SessionInfo
   * @param rules - the SessionRules governing the session
   * @param sessionID - the unique UUID that identifies the session
   * @param playerAmnt - the amount of players in this session at the time of this object's creation
   */
  public SessionInfo(SessionRules rules, UUID sessionID, int playerAmnt) {
    this.rules = rules;
    this.sessionID = sessionID;
    this.playerAmnt = playerAmnt;
  }
  
  @Override
  public int hashCode() {
    return sessionID.hashCode();
  }
  
  @Override
  public boolean equals(Object obj) {
    if (obj instanceof SessionInfo) {
      SessionInfo other = (SessionInfo) obj;
      return other.sessionID.equals(sessionID);
    }
    return false;
  }
  
  /**
   * Retrieves the amount of players in this session
   * 
   * Note: The value returned may not accurately reflect
   *       the actual, current amount of players in a session.
   *       This value is what the amount of players were at the creation of
   *       this SessionInfo object.
   * @return the amount of players in this session
   */
  public int getPlayerAmnt() {
    return playerAmnt;
  }

  /**
   * Retrieves the rules of that govern this session
   * @return the rules of that govern this session
   */
  public SessionRules getRules() {
    return rules;
  }

  /**
   * Retrieves the UUID that identifies this session
   * @return the UUID that identifies this session
   */
  public UUID getSessionID() {
    return sessionID;
  }
}
