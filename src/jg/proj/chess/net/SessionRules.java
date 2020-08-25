package jg.proj.chess.net;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import jg.proj.chess.net.SessionRules.Properties;
import jg.proj.chess.utils.StringAndIOUtils;

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
    PRISON_DILEMMA(false),
    
    /**
     * A long property that sets the amount of seconds alloted for voting
     * for team
     */
    VOTING_DURATION(15L),
    
    /**
     * An int property that sets the minimum amount of players each team must have
     * before the session starts
     */
    MIN_TEAM_COUNT(1),
    
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
    ALLOW_INVL_VOTES(false),
    
    /**
     * A boolean property that allows for players to join even after
     * the session has started.
     * 
     * If true, players are allowed to join mid-game. Else, joining 
     * is disallowed after the game has started
     */
    ALLOW_JOINS_GAME(true),
    
    /**
     * A long property that sets the amount of seconds to wait before
     * each turn, as a 'break'/'pause' in between sessions
     */
    BREAK_AMOUNT(0L);
    
    private final Object defaultValue;
    private Properties(Object defaultValue) {
      this.defaultValue = defaultValue;
    }
    
    public Object getDefaultValue() {
      return defaultValue;
    }
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
    str += Properties.BREAK_AMOUNT+"="+properties.get(Properties.BREAK_AMOUNT)+":";
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
    defaultMap.put(Properties.PRISON_DILEMMA, Properties.PRISON_DILEMMA.defaultValue);
    defaultMap.put(Properties.VOTING_DURATION, Properties.VOTING_DURATION.defaultValue);
    defaultMap.put(Properties.MIN_TEAM_COUNT, Properties.MIN_TEAM_COUNT.defaultValue);
    defaultMap.put(Properties.ALLOW_INVL_VOTES, Properties.ALLOW_INVL_VOTES.defaultValue);
    defaultMap.put(Properties.ALLOW_JOINS_GAME, Properties.ALLOW_JOINS_GAME.defaultValue);
    defaultMap.put(Properties.BREAK_AMOUNT, Properties.BREAK_AMOUNT.defaultValue);
    
    return defaultMap;
  }
  
  /**
   * Parses from a string a SessionRules object
   * Note: The string to parse must be in the format:
   *     PROPERTY1=VALUE1:PROPERTY2=VALUE2: .... etc.
   * 
   * @param toparse
   * @return a SessionRules object, or null if string was incorrectly formatted
   */
  public static SessionRules parseFromString(String toparse){
    System.out.println("TARGET: "+toparse);
    SessionRules sessionRules = new SessionRules();
    ArrayList<String> arguments = new ArrayList<String>(Arrays.asList(toparse.split(":")));
    
    String [][] rawStrings = new String[ServerRequest.CSESS.argAmount() - 1][2];
    for (int i = 0; i < arguments.size(); i++) {
      System.out.println("--> "+arguments.get(i));
      rawStrings[i] = StringAndIOUtils.parseAssignment(arguments.get(i));
      if (rawStrings[i] == null) {
        sessionRules = null;
        break;
      }
    }
    System.out.println("==========");
    
    if (sessionRules != null) {
      //create default rules
      SessionRules rules = new SessionRules();
      for(String [] assgn : rawStrings){
        System.out.println("CYCLING!!! "+Arrays.toString(assgn));
        try {
          Properties property = Properties.valueOf(assgn[0]);
          Object value = null;
          switch (property) {
          case PRISON_DILEMMA:
            value = Boolean.parseBoolean(assgn[1].toLowerCase());
            break;
          case VOTING_DURATION:
            value = Long.parseLong(assgn[1]);
            break;
          case MIN_TEAM_COUNT:
            value = Integer.parseInt(assgn[1]);
            break;
          case ALLOW_INVL_VOTES:
            value = Boolean.parseBoolean(assgn[1].toLowerCase());
            break;
          case ALLOW_JOINS_GAME:
            value = Boolean.parseBoolean(assgn[1].toLowerCase());
            break;
          case BREAK_AMOUNT:
            value = Long.parseLong(assgn[1]);
            break;
          }

          //set the property
          rules.setProperty(property, value);
        } catch (IllegalArgumentException e) {
          sessionRules = null;
          break;
        }
      }
      
      System.out.println("~~~~~~~~~~~~~~~");
      return rules;
    }  
    
    System.out.println("done!!!!!! "+sessionRules);
    return sessionRules;
  }
}
