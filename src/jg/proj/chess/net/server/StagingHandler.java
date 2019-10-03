package jg.proj.chess.net.server;

import java.util.Arrays;
import java.util.Random;
import java.util.UUID;
import java.util.stream.Collectors;

import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.util.AttributeKey;
import jg.proj.chess.net.Player;
import jg.proj.chess.net.Session;

public class StagingHandler extends SimpleChannelInboundHandler<String> {
  
  public static final String FAILED_JOIN_CREATE = "-1:FALSE\n\r";
  public static final String INVALID_ARG_AMNT = "W_ARG\n\r";
  
  private final GameServer server;
  private final Database database;
  
  public StagingHandler(GameServer server) {
    this.server = server;
    this.database = server.getDatabase();
  }
  
  @Override
  public void handlerAdded(ChannelHandlerContext ctx) throws Exception {
    Channel sender = ctx.channel();         
    System.out.println(" staged "+sender.remoteAddress());
  }
  
  @Override
  protected void channelRead0(ChannelHandlerContext ctx, String msg) throws Exception {
    Channel sender = ctx.channel();
    Player player = (Player) sender.attr(AttributeKey.valueOf("player")).get();
    
    //initial message should be:
    // "USERNAME: JOIN_COMMAND" or "USERNAME: CSESS_COMMAND"
    
    //check is message is a "join" or "csess" command. If so, change handler to be a Session
    // join command: "join:SESSION_ID" or "join:SESSION_ID:TEAM1?"  -> "SESSION_ID:TEAM1"
    // csess command: "csess" or "csess:TEAM1" -> "SESSION_ID:TEAM1"
    
    AttributeKey<Boolean> teamAttribute = AttributeKey.valueOf("teamone");
    
    String [] split = msg.split(":");
        
    System.out.println(" FROM PLAYER "+sender.remoteAddress()+" | "+Arrays.toString(split)+" | original: "+msg.trim());
    
    if (split.length >= 2) {
      //set user name
      String userName = split[0];
      player.setName(userName);
      System.out.println("    ->NEW USERNAME: "+userName+" | "+sender.remoteAddress());
      
      //decide on command
      String command = split[1];
      if (command.equals("join")) {
        if (split.length >= 3) {
          UUID sessionID = UUID.fromString(split[2]);
          Session desiredSession = database.findSession(sessionID);
                
          System.out.println(" USER: "+userName+" attempting to join "+sessionID+" || all games "+database.getAllSessionIDS()+" | "+(desiredSession == null));
          if (desiredSession != null) {
            if (split.length == 3) {
              sender.attr(teamAttribute).set(new Random().nextBoolean());
            }
            else if (split.length == 4) {
              boolean joinTeamOne = Boolean.parseBoolean(split[3].trim().toLowerCase());
              sender.attr(teamAttribute).set(joinTeamOne);        
            }
            sender.write(sessionID.toString()+":"+sender.attr(teamAttribute).get());
            
            sender.pipeline().removeLast();
            sender.pipeline().addLast("shandler", desiredSession);
            
            System.out.println("  USER: "+userName+" has been successfully moved");
          }
          else {
            sendErrorAndClose(FAILED_JOIN_CREATE, sender, player);
          }
        }
        else {
          sendErrorAndClose(INVALID_ARG_AMNT, sender, player);
        }
      }
      else if (command.equals("csess")) {
        Session newSession = new Session(server);
        database.addSession(newSession);
             
        System.out.println(" USER: "+userName+" created a session with ID: "+newSession.getSessionID());
        
        if (split.length == 2) {
          sender.attr(teamAttribute).set(new Random().nextBoolean());
        }
        else if (split.length == 3) {
          boolean joinTeamOne = Boolean.parseBoolean(split[2].trim().toLowerCase());
          sender.attr(teamAttribute).set(joinTeamOne);
        }
              
        sender.write(newSession.getSessionID().toString()+":"+sender.attr(teamAttribute).get());
        
        sender.pipeline().removeLast();
        sender.pipeline().addLast("shandler", newSession);

        System.out.println(" USER: "+userName+" created a session with ID: "+newSession.getSessionID()+" and has been moved");
        
        server.runSession(newSession);
      }
      else {
        sendErrorAndClose(FAILED_JOIN_CREATE, sender, player);
      }
    }
    else {
      sendErrorAndClose(FAILED_JOIN_CREATE, sender, player);
    }
  }
  
  private void sendErrorAndClose(String message, Channel sender, Player player){
    database.removePlayer(player.getID());
    sender.writeAndFlush(FAILED_JOIN_CREATE);
    sender.close();
  }
  
}
