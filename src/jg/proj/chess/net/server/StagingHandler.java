package jg.proj.chess.net.server;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.NoSuchElementException;
import java.util.Random;
import java.util.UUID;
import java.util.stream.Collectors;

import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.util.AttributeKey;
import jg.proj.chess.net.IOUtils;
import jg.proj.chess.net.Player;
import jg.proj.chess.net.ServerRequests;
import jg.proj.chess.net.ServerResponses;
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
  public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
    //if an IO error is caught, just remove the player from the server
    if (cause instanceof IOException) {
      Player player = (Player) ctx.channel().attr(AttributeKey.valueOf("player")).get();
      database.removePlayer(player.getID());
      
      if (player.getSession() != null) {
        Session currentSession = player.getSession();
        try {
          ctx.channel().pipeline().remove(player.getSession());
          System.out.println("---Removed player from session "+currentSession.getSessionID()+" | "+player.getName());
        } catch (NoSuchElementException  e) {
          System.out.println("---Error while removing player from session "+currentSession.getSessionID()+" | "+player.getName());
        }
      }
    }
  }
  
  @Override
  protected void channelRead0(ChannelHandlerContext ctx, String msg) throws Exception {
    Channel sender = ctx.channel();
    Player player = (Player) sender.attr(AttributeKey.valueOf("player")).get();
    //check is message is a "join" or "csess" command. If so, change handler to be a Session
    
    AttributeKey<Boolean> teamAttribute = AttributeKey.valueOf("teamone");
                
    ArrayList<String> arguments = new ArrayList<String>(Arrays.asList(msg.split(":")));
    System.out.println(" FROM PLAYER "+sender.remoteAddress()+" | "+arguments+" | original: "+msg.trim());

    String first = arguments.remove(0);
    if (first.equals("~cuser")) {
      if (arguments.size() == ServerRequests.CUSER.argAmount()) {
        player.setName(arguments.get(0));
        IOUtils.writeAndFlush(sender, arguments.get(0));
      }
      else {
        IOUtils.writeAndFlush(sender, String.format(ServerResponses.BAD_ARGS, 
            ServerRequests.CUSER, 
            ServerRequests.CUSER.argAmount(), 
            arguments.size()));
      }
    }
    else if (first.equals("~join")) {
      if (arguments.size() == ServerRequests.JOIN.argAmount()) {
        UUID sessionID = UUID.fromString(arguments.get(0));
        int teamID = Integer.parseInt(arguments.get(1));
               
        Session session = database.findSession(sessionID);
        if (session == null) {
          IOUtils.writeAndFlush(sender, String.format(ServerResponses.NO_SESSION, sessionID.toString()));
        }
        else {          
          sender.attr(teamAttribute).set(teamID == 1 ? true : teamID == 2 ? false : new Random().nextBoolean());
          sender.pipeline().removeLast();
          sender.pipeline().addLast("shandler", session);
          
          IOUtils.writeAndFlush(sender, sessionID.toString()+":"+sender.attr(teamAttribute).get());
        }
      }
      else {
        IOUtils.writeAndFlush(sender, String.format(ServerResponses.BAD_ARGS, 
            ServerRequests.JOIN.toString(),
            ServerRequests.JOIN.argAmount(),
            arguments.size()));
      }
    }
    else if (first.equals("~csess")) {
      if (arguments.size() == ServerRequests.CSESS.argAmount()) {
        int teamID = Integer.parseInt(arguments.get(0));
        long secondsForVote = Long.parseLong(arguments.get(1));
        
        Session newSession = new Session(server, secondsForVote <= 0 ? 15000 : secondsForVote);        
        database.addSession(newSession);
        
        System.out.println(" USER: "+player.getID()+" created a session with ID: "+newSession.getSessionID());
              
        sender.attr(teamAttribute).set(teamID == 1 ? true : teamID == 2 ? false : new Random().nextBoolean());
        IOUtils.writeAndFlush(sender, newSession.getSessionID().toString()+":"+sender.attr(teamAttribute).get());
        
        sender.pipeline().removeLast();
        sender.pipeline().addLast("shandler", newSession);

        System.out.println(" USER: "+player.getID()+" created a session with ID: "+newSession.getSessionID()+" and has been moved");
        
        server.runSession(newSession);
      }
      else {
        IOUtils.writeAndFlush(sender, String.format(ServerResponses.BAD_ARGS, 
            ServerRequests.CSESS.toString(),
            ServerRequests.CSESS.argAmount(),
            arguments.size()));
      }
    }
    else {
      IOUtils.writeAndFlush(sender, String.format(ServerResponses.BAD_REQ, "unknown reqest for staging"));
    }
  }  
}
