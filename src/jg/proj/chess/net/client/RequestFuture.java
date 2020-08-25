package jg.proj.chess.net.client;

/**
 * Represents a server request pending a response
 * @author Jose
 *
 */
public class RequestFuture{
  
  /**
   * Status of this request
   * @author Jose
   */
  public enum Status{
    /**
     * Request has been completed.
     */
    COMPLETE,
    
    /**
     * Request has been completed, but server responded with an error.
     */
    ERROR,
    
    /**
     * Request is still pending. No response from server yet.
     */
    PENDING;
  }

  protected final RequestBody request;
  protected final Reactor reactor;
  
  private volatile Status status;
  
  /**
   * Constructs a RequestFuture
   * @param request - the original request
   * @param reactor - the Reactor for this request
   */
  public RequestFuture(RequestBody request, Reactor reactor) {
    this.request = request;
    this.reactor = reactor;
    this.status = Status.PENDING;
  }
  
  /**
   * Sets the status of this request
   * @param status
   */
  public void changeStatus(Status status) {
    this.status = status;
  }
  
  /**
   * Invokes the callback code associated with 
   * this request at request completion
   * 
   * @param results - the results of this request
   */
  public void react(String ... results) {
    reactor.react(request, results);
  }
  
  /**
   * Invokes the callback code associated with
   * this request at request failure (the server has responded 
   * to the request with an error code)
   * 
   * @param errorCode - the error code
   */
  public void error(int errorCode) {
    reactor.error(request, errorCode);
  }
  
  /**
   * Retrieves the reactor for this request
   * @return the reactor for this request
   */
  public Reactor getReactor() {
    return reactor;
  }
  
  /**
   * Retrieves the status of this request
   * @return the status of this request
   */
  public Status getStatus() {
    return status;
  }
  
  /**
   * Retrieves the original request 
   * @return the original request
   */
  public RequestBody getOriginalRequest() {
    return request;
  }
  
}
