package jg.proj.chess.net.client;

import java.util.UUID;

import jg.proj.chess.net.ServerRequest;

public class PendingRequest {
  
  private final UUID identifier;
  private final ServerRequest baseRequest;
  private final Object [] arguments;

  public PendingRequest(ServerRequest baseRequest){
    this(baseRequest, new Object[0]);
  }
  
  public PendingRequest(ServerRequest baseRequest, Object ... arguments){
    if (baseRequest.argAmount() != arguments.length) {
      throw new IllegalArgumentException("Argument provides doesnt match requried argument amount!");
    }
    this.identifier = UUID.randomUUID();
    this.baseRequest = baseRequest;
    this.arguments = arguments;
  }
  
  @Override
  public int hashCode() {
    return identifier.hashCode();
  }
  
  @Override
  public boolean equals(Object obj) {
    if (obj instanceof PendingRequest) {
      PendingRequest other = (PendingRequest) obj;
      return other.identifier.equals(identifier);
    }
    return false;
  }
  
  public Object [] getArguments(){
    return arguments;
  }
  
  public ServerRequest getRequest(){
    return baseRequest;
  }
  
  public UUID getIdentifier() {
    return identifier;
  }
  
  public String toString(){
    return identifier+":"+baseRequest.addArguments(arguments);
  }
}
