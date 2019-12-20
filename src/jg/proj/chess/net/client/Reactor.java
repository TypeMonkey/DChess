package jg.proj.chess.net.client;

public interface Reactor {
  
  public static final Reactor BLANK_REACTOR = new BlankReactor();
  
  public void react(PendingRequest request, String ... results);
  
  public void error(PendingRequest request, int errorCode);
  
  /**
   * Reactor that gives no reaction
   * @author Jose
   *
   */
  class BlankReactor implements Reactor{
    
    private BlankReactor() {}

    @Override
    public void react(PendingRequest request, String ... results) {}

    @Override
    public void error(PendingRequest request, int errorCode) {}

  }
}
