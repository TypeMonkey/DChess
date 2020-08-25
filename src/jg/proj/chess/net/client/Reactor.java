package jg.proj.chess.net.client;

/**
 * Allows for a concise way to house callback
 * logic/code for server requests
 * 
 * @author Jose
 */
public interface Reactor {
  
  /**
   * A convenient implementation of the Reactor interface
   * for requests that don't need a reaction
   * (such as chat messages)
   */
  public static final Reactor BLANK_REACTOR = new BlankReactor();
  
  /**
   * Invoked when the associated request was successfully
   * completed.
   * 
   * @param request - the original request
   * @param results - the results sent by the server
   */
  public void react(RequestBody request, String ... results);
  
  /**
   * Invoked when the associated request was completed
   * with a failure
   * 
   * @param request - the original request
   * @param errorCode - the error code sent by the server
   */
  public void error(RequestBody request, int errorCode);
  
  /**
   * Reactor that gives no reaction
   * @author Jose
   *
   */
  class BlankReactor implements Reactor{
    
    private BlankReactor() {}

    /**
     * Empty method. Does nothing
     */
    @Override
    public void react(RequestBody request, String ... results) {}

    /**
     * Empty method. Does nothing.
     */
    @Override
    public void error(RequestBody request, int errorCode) {}

  }
}
