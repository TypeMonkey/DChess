package jg.proj.chess.net.client.uis;

/**
 * Used by UI controllers that need to execute
 * certain logic prior to display
 * @author Jose
 *
 */
public interface Preparable {
  
  /**
   * Prepares the controller prior to displaying
   */
  public void prepare();
  
}
