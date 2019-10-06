package jg.proj.chess.net.client;

import java.util.UUID;

import jg.proj.chess.net.server.SessionRules;

public class SessionInfo {
  
  private final SessionRules rules;
  private final UUID sessionID;
  private final boolean isTeamOne;
  
  public SessionInfo(SessionRules rules, UUID sessionID, boolean isTeamOne) {
    this.rules = rules;
    this.sessionID = sessionID;
    this.isTeamOne = isTeamOne;
  }

  public SessionRules getRules() {
    return rules;
  }

  public UUID getSessionID() {
    return sessionID;
  }

  public boolean isTeamOne() {
    return isTeamOne;
  }
}
