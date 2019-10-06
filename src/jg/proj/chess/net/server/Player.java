package jg.proj.chess.net.server;

import java.util.UUID;

import io.netty.channel.Channel;

public class Player {

  private final UUID id;
  private final Channel channel;
  
  private String name;
  private Session currentSession;
    
  public Player(Channel channel){
    this.id = UUID.randomUUID();
    this.channel = channel;
  }
  
  public void setName(String name){
    this.name = name;
  }
  
  public void setSession(Session session){
    this.currentSession = session;
  }
  
  public boolean equals(Object object){
    if (object instanceof Player) {
      return ((Player) object).id.equals(id);
    }
    return false;
  }
  
  public int hashCode(){
    return id.hashCode();
  }
  
  public Session getSession(){
    return currentSession;
  }
  
  public String getName(){
    return name;
  }
  
  public Channel getChannel(){
    return channel;
  }
  
  public UUID getID(){
    return id;
  }
}
