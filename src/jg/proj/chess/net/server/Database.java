package jg.proj.chess.net.server;

import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Acts as a simple "database" of all connected Players and active sessions
 * 
 * @author Jose
 *
 */
public class Database {

  private final Map<UUID, Player> playerMap;
  private final Map<UUID, Session> sessionMap;
  
  /**
   * Constructs a Database
   */
  public Database() {
    this.playerMap = new ConcurrentHashMap<UUID, Player>();
    this.sessionMap = new ConcurrentHashMap<UUID, Session>();
  }
  
  //---Session methods---
  public Session findSession(UUID uuid){
    return sessionMap.get(uuid);
  }
  
  public void addSession(Session session){
    sessionMap.put(session.getSessionID(), session);
  }
  
  public Session removeSession(UUID id){
    return sessionMap.remove(id);
  }
  
  public Set<UUID> getAllSessionIDS(){
    return sessionMap.keySet();
  }
  
  public int getSessionCount() {
    return sessionMap.size();
  }
  //---Session methods END---
  
  //---Player methods---
  public Player findPlayer(UUID uuid){
    return playerMap.get(uuid);
  }
  
  public void addPlayer(Player player){
    playerMap.put(player.getID(), player);
  }
  
  public Player removePlayer(UUID id){
    return playerMap.remove(id);
  }
  
  public Set<UUID> getAllPlayerIDS(){
    return playerMap.keySet();
  }
  
  public int playerCount(){
    return playerMap.size();
  } 
  //---Player methods END---
  
}
