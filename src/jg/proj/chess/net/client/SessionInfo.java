package jg.proj.chess.net.client;

import java.util.UUID;

import jg.proj.chess.net.SessionRules;

public class SessionInfo {
  
  private final SessionRules rules;
  private final UUID sessionID;
  private final int playerAmnt;
  
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
  
  public int getPlayerAmnt() {
    return playerAmnt;
  }

  public SessionRules getRules() {
    return rules;
  }

  public UUID getSessionID() {
    return sessionID;
  }
}
