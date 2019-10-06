package jg.proj.chess.net.client;

import java.util.concurrent.CompletableFuture;

public class RequestFuture extends CompletableFuture<Object []>{
  
  private final PendingRequest request;
    
  public RequestFuture(PendingRequest request) {
    this.request = request;
  }

  public PendingRequest getOriginalRequest(){
    return request;
  }
}
