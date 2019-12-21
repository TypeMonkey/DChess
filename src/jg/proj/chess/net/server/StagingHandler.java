package jg.proj.chess.net.server;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Random;
import java.util.UUID;
import java.util.stream.Collectors;

import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.util.AttributeKey;
import jg.proj.chess.net.StringAndIOUtils;
import jg.proj.chess.net.server.SessionRules.Properties;
import jg.proj.chess.net.ServerRequest;
import jg.proj.chess.net.ServerResponses;

/**
 * ChannelHandler meant for Players that have yet to join or create a Session
 * @author Jose
 *
 */
public class StagingHandler extends SimpleChannelInboundHandler<String> {
 
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
      if (arguments.size() == ServerRequest.CUSER.argAmount()) {
        player.setName(arguments.get(0));
        StringAndIOUtils.writeAndFlush(sender, ServerRequest.CUSER.getName()+":"+arguments.get(0)+":"+player.getID().toString());
      }
      else {
        StringAndIOUtils.writeAndFlush(sender, ServerRequest.CUSER.createErrorString(ServerResponses.WRONG_ARGS));
      }
    }
    else if (first.equals("~join")) {
      if (arguments.size() == ServerRequest.JOIN.argAmount()) {
        try {
          //parse sessionID and teamID       
          UUID sessionID = UUID.fromString(arguments.get(0));
          int teamID = Integer.parseInt(arguments.get(1));
          
          Session session = database.findSession(sessionID);
          if (session == null) {
            System.out.println("----FROM: "+player.getName()+" attempt to join SESSION: |"+sessionID+"| , AVAILS: "+database.getAllSessionIDS());
            StringAndIOUtils.writeAndFlush(sender, ServerRequest.JOIN.createErrorString(ServerResponses.NO_SESS));
          }
          else if (session.getRules().getProperty(Properties.ALLOW_JOINS_GAME).equals(Boolean.FALSE) ||
                   !session.isAcceptingPlayers()) {
            StringAndIOUtils.writeAndFlush(sender, ServerRequest.JOIN.createErrorString(ServerResponses.NO_JOIN));
          }
          else {          
            sender.attr(teamAttribute).set(teamID == 1 ? true : teamID == 2 ? false : new Random().nextBoolean());
            sender.pipeline().removeLast();
            sender.pipeline().addLast("shandler", session);
            
            StringAndIOUtils.writeAndFlush(sender, ServerRequest.JOIN.getName()+":"+sessionID.toString()+":"+sender.attr(teamAttribute).get()+":"+session.getRules());
          }
          
        } catch (IllegalArgumentException e) {
          StringAndIOUtils.writeAndFlush(sender, ServerRequest.JOIN.createErrorString(ServerResponses.WRONG_ARGS));
        }
      }
      else {
        StringAndIOUtils.writeAndFlush(sender, ServerRequest.JOIN.createErrorString(ServerResponses.WRONG_ARGS));
      }
    }
    else if (first.equals("~csess")) {
      System.out.println("---IN CSESS");
      if (arguments.size() == ServerRequest.CSESS.argAmount()) {
        boolean teamIDParsingFailed = false;
        int teamID = -1;
        try {
          teamID = Integer.parseInt(arguments.remove(0));
        } catch (NumberFormatException e) {
          teamIDParsingFailed = true;
        }
        
        if (teamIDParsingFailed) {
          System.out.println("---FAILED TO PARSE ID");
          StringAndIOUtils.writeAndFlush(sender, ServerRequest.CSESS.createErrorString(ServerResponses.WRONG_ARGS));
        }
        else {    
          System.out.println("----parsing----");
          SessionRules rules = SessionRules.parseFromString(arguments.stream().collect(Collectors.joining(":")));
          System.out.println("----parsing done!!!----");

          if (rules != null) {            
            Session newSession = new Session(server, rules);        
            database.addSession(newSession);
            
            System.out.println("---SESSION CREATED AND ADDED");

            System.out.println(" USER: "+player.getID()+" created a session with ID: "+newSession.getSessionID());

            sender.attr(teamAttribute).set(teamID == 1 ? true : teamID == 2 ? false : new Random().nextBoolean());
            StringAndIOUtils.writeAndFlush(sender, ServerRequest.CSESS.getName()+":"+newSession.getSessionID().toString()+":"+
                sender.attr(teamAttribute).get()+":"+
                rules.toString());

            sender.pipeline().removeLast();
            sender.pipeline().addLast("shandler", newSession);

            System.out.println(" USER: "+player.getID()+" created a session with ID: "+newSession.getSessionID()+" and has been moved");

            server.runSession(newSession);
          }
          else {
            System.out.println("---FAILED TO PARSE RULES");
            StringAndIOUtils.writeAndFlush(sender, ServerRequest.CSESS.createErrorString(ServerResponses.WRONG_ARGS));
          }
        }       
      }
      else {
        System.out.println("mismatch aargs! "+arguments.size());
        StringAndIOUtils.writeAndFlush(sender, ServerRequest.CSESS.createErrorString(ServerResponses.WRONG_ARGS));
      }
    }
    else if (first.equals("~ses")) {
      if (arguments.size() == ServerRequest.SES.argAmount()) {        
        String whole = "";
        for(UUID uuid : database.getAllSessionIDS()) {
          Session session = database.findSession(uuid);
          if (session != null && session.isAcceptingPlayers()) {
            whole += uuid.toString()+","+session.totalPlayers()+","+session.getRules().getProperty(Properties.PRISON_DILEMMA)+":";
          }
        }
        
        StringAndIOUtils.writeAndFlush(sender, ServerRequest.SES.getName()+":"+whole);
      }
      else {
        StringAndIOUtils.writeAndFlush(sender, ServerRequest.SES.createErrorString(ServerResponses.WRONG_ARGS));
      }
    }
    else if (first.equals("~disc")) {
      System.out.println(" ---->>>> "+player.getName()+" has DISCONNECTED (staging)!!!!");
      
      server.getDatabase().removePlayer(player.getID());
      sender.pipeline().remove(this);
      sender.close();
    }
    else {
      String isolatedCommandName = first.substring(1).toUpperCase();
      /*
       * check if command prefix is an actual valid command in the first place
       */
      try {
        ServerRequest request = ServerRequest.valueOf(isolatedCommandName);
        StringAndIOUtils.writeAndFlush(sender, String.format(ServerResponses.BAD_REQUEST, request.getName(), ServerResponses.NOT_IN_SESS));
      } catch (IllegalArgumentException e) {
        // Not a valid command....
        StringAndIOUtils.writeAndFlush(sender, String.format(ServerResponses.BAD_REQUEST, "!"+first, ServerResponses.UNKNOWN));
      }
    }
  }  
}
