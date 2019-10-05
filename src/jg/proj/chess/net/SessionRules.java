package jg.proj.chess.net;

import java.util.HashMap;
import java.util.Map;

public final class SessionRules {

  /**
   * Properties that Game Sessions will value
   * @author Jose
   *
   */
  public enum Properties{
    /**
     * A boolean property that disallows communication between teams
     * and among team members
     * 
     * If true, prisoner's dilemma is enforced. Else, communication is allowed
     */
    PRISON_DILEMMA,
    
    /**
     * A long property that sets the amount of seconds alloted for voting
     * for team
     */
    VOTING_DURATION,
    
    /**
     * An int property that sets the minimum amount of players each team must have
     * before the session starts
     */
    MIN_TEAM_COUNT,
    
    /**
     * A boolean property that allows for invalid move votes to be casted
     * 
     * If true, invalid votes can be casted. This means that Players casting an invalid vote
     * (i.e: trying to move a unit to an invalid square,
     *       moving to move a unit that's not in their respective team,
     *       voting on an origin square that has no units
     * 
     * Else, invalid votes are filtered prior to counting a final result.
     */
    ALLOW_INVL_VOTES,
    
    /**
     * A boolean property that allows for players to join even after
     * the session has started.
     * 
     * If true, players are allowed to join mid-game. Else, joining 
     * is disallowed after the game has started
     */
    ALLOW_JOINS_GAME;
  }
  
  private final Map<Properties, Object> properties;
  
  public SessionRules(){
    this.properties = getDefaultPropertiesMap();
  }
  
  public void setProperty(Properties property, Object value){
    properties.put(property, value);
  }
  
  public Object getProperty(Properties property){
    return properties.get(property);
  }
  
  public String toString(){
    String str = "";
    str += Properties.PRISON_DILEMMA+"="+properties.get(Properties.PRISON_DILEMMA)+":";
    str += Properties.VOTING_DURATION+"="+properties.get(Properties.VOTING_DURATION)+":";
    str += Properties.MIN_TEAM_COUNT+"="+properties.get(Properties.MIN_TEAM_COUNT)+":";
    str += Properties.ALLOW_INVL_VOTES+"="+properties.get(Properties.ALLOW_INVL_VOTES)+":";
    str += Properties.ALLOW_JOINS_GAME+"="+properties.get(Properties.ALLOW_JOINS_GAME)+":";
    return str;
  }
  
  /**
   * Returns the default rules for a Session
   * @return the properties map with the default values
   */
  private static Map<Properties, Object> getDefaultPropertiesMap(){
    HashMap<Properties, Object> defaultMap = new HashMap<>();
    
    /*
     * Load default values
     */
    defaultMap.put(Properties.PRISON_DILEMMA, true);
    defaultMap.put(Properties.VOTING_DURATION, 15000);
    defaultMap.put(Properties.MIN_TEAM_COUNT, 1);
    defaultMap.put(Properties.ALLOW_INVL_VOTES, false);
    defaultMap.put(Properties.ALLOW_JOINS_GAME, true);
    
    return defaultMap;
  }
  
  
}
