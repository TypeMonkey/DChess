package jg.proj.chess.net.client;

public class RequestFuture{
  
  public enum Status{
    COMPLETE,
    ERROR,
    PENDING;
  }

  protected final PendingRequest request;
  protected final Reactor reactor;
  
  private volatile Status status;
  
  public RequestFuture(PendingRequest request, Reactor reactor) {
    this.request = request;
    this.reactor = reactor;
    this.status = Status.PENDING;
  }
  
  public void changeStatus(Status status) {
    this.status = status;
  }
  
  public void react(String ... results) {
    reactor.react(request, results);
  }
  
  public void error(int errorCode) {
    reactor.error(request, errorCode);
  }
  
  public Reactor getReactor() {
    return reactor;
  }
  
  public Status getStatus() {
    return status;
  }
  
  public PendingRequest getOriginalRequest() {
    return request;
  }
  
}
