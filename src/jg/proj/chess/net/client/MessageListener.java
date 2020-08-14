package jg.proj.chess.net.client;

/**
 * Interface for handling messages.
 * 
 * Messages are from the server that do not exactly
 * correspond with a request. Meaning, clients can receive messages
 * without having made a single request to the server.
 * 
 * Messages include "team" and "all" chats
 * 
 * @author Jose Guaro
 *
 */
public interface MessageListener {

  /**
   * Handles a received message
   * @param messageType - the type of message
   * @param messageContent - the actual message's content
   */
  public void handleMessage(String messageType, String messageContent);
  
}
