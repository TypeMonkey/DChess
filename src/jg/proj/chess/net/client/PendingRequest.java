package jg.proj.chess.net.client;

import jg.proj.chess.net.ServerRequest;

public class PendingRequest {
  
  private final ServerRequest baseRequest;
  private final Object [] arguments;

  public PendingRequest(ServerRequest baseRequest, Object ... arguments){
    if (baseRequest.argAmount() != arguments.length) {
      throw new IllegalArgumentException("Argument provides doesnt match requried argument amount!");
    }
    this.baseRequest = baseRequest;
    this.arguments = arguments;
  }
  
  public Object [] getArguments(){
    return arguments;
  }
  
  public ServerRequest getRequest(){
    return baseRequest;
  }
  
  public String toString(){
    return baseRequest.addArguments(arguments);
  }
}
