package jg.proj.chess.net.server;

/**
 * Describes the status of this session
 * @author Jose Guaro
 */
public enum SessionStatus {

  /**
   * This session has started.
   */
  RUNNING,
  
  /**
   * This session is currently 
   * accepting players. Once it reaches
   * the minimum player count for both teams, 
   * it will start playing.
   */
  ACCEPTING,
  
  /**
   * This session is currently playing.
   */
  PLAYING,
  
  /**
   * This session is currently accepting votes
   */
  VOTING,
  
  /**
   * This session is currently processing votes
   */
  PROCESSING,
  
  /**
   * This session has ended
   */
  ENDED;
}
