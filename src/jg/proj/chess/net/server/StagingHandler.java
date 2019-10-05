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
import jg.proj.chess.net.Player;
import jg.proj.chess.net.ServerRequests;
import jg.proj.chess.net.ServerResponses;
import jg.proj.chess.net.Session;
import jg.proj.chess.net.SessionRules;
import jg.proj.chess.net.SessionRules.Properties;

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
      if (arguments.size() == ServerRequests.CUSER.argAmount()) {
        player.setName(arguments.get(0));
        StringAndIOUtils.writeAndFlush(sender, arguments.get(0));
      }
      else {
        StringAndIOUtils.writeAndFlush(sender, String.format(ServerResponses.BAD_ARGS, 
            ServerRequests.CUSER, 
            ServerRequests.CUSER.argAmount(), 
            arguments.size()));
      }
    }
    else if (first.equals("~join")) {
      if (arguments.size() == ServerRequests.JOIN.argAmount()) {
        try {
          //parse sessionID and teamID       
          UUID sessionID = UUID.fromString(arguments.get(0));
          int teamID = Integer.parseInt(arguments.get(1));
          
          Session session = database.findSession(sessionID);
          if (session == null) {
            StringAndIOUtils.writeAndFlush(sender, String.format(ServerResponses.NO_SESSION, sessionID.toString()));
          }
          else {          
            sender.attr(teamAttribute).set(teamID == 1 ? true : teamID == 2 ? false : new Random().nextBoolean());
            sender.pipeline().removeLast();
            sender.pipeline().addLast("shandler", session);
            
            StringAndIOUtils.writeAndFlush(sender, sessionID.toString()+":"+sender.attr(teamAttribute).get()+":"+session.getRules());
          }
          
        } catch (IllegalArgumentException e) {
          StringAndIOUtils.writeAndFlush(sender, String.format(ServerResponses.BAD_ARGS, 
              ServerRequests.JOIN.toString(),
              ServerRequests.JOIN.argAmount(),
              arguments.size()));
        }
      }
      else {
        StringAndIOUtils.writeAndFlush(sender, String.format(ServerResponses.BAD_ARGS, 
            ServerRequests.JOIN.toString(),
            ServerRequests.JOIN.argAmount(),
            arguments.size()));
      }
    }
    else if (first.equals("~csess")) {
      if (arguments.size() == ServerRequests.CSESS.argAmount()) {
        boolean teamIDParsingFailed = false;
        int teamID = -1;
        try {
          teamID = Integer.parseInt(arguments.remove(0));
        } catch (NumberFormatException e) {
          teamIDParsingFailed = true;
        }
        
        if (teamIDParsingFailed) {
          StringAndIOUtils.writeAndFlush(sender, String.format(ServerResponses.BAD_ARGS, 
              ServerRequests.CSESS.toString(),
              ServerRequests.CSESS.argAmount(),
              arguments.size()));
        }
        else {
        //parse configuration arguments       
          String [][] rawStrings = new String[ServerRequests.CSESS.argAmount()][2];
          for (int i = 0; i < arguments.size(); i++) {
            rawStrings[i] = StringAndIOUtils.parseAssignment(arguments.get(i));
          }
          
          //flag to indicate successful parsing
          boolean parseSuccessful = false;
          
          //create default rules
          SessionRules rules = new SessionRules();
          for(String [] assgn : rawStrings){
            try {
              Properties property = Properties.valueOf(assgn[0]);
              Object value = null;
              switch (property) {
              case PRISON_DILEMMA:
                value = Boolean.parseBoolean(assgn[1].toLowerCase());
                break;
              case VOTING_DURATION:
                value = Long.parseLong(assgn[1]);
                break;
              case MIN_TEAM_COUNT:
                value = Integer.parseInt(assgn[1]);
                break;
              case ALLOW_INVL_VOTES:
                value = Boolean.parseBoolean(assgn[1].toLowerCase());
                break;
              case ALLOW_JOINS_GAME:
                value = Boolean.parseBoolean(assgn[1].toLowerCase());
                break;
              }
              
              //set the property
              rules.setProperty(property, value);
            } catch (IllegalArgumentException e) {
              parseSuccessful = false;
              break;
            }
          }
          
          if (parseSuccessful) {
            Session newSession = new Session(server, rules);        
            database.addSession(newSession);
            
            System.out.println(" USER: "+player.getID()+" created a session with ID: "+newSession.getSessionID());
                  
            sender.attr(teamAttribute).set(teamID == 1 ? true : teamID == 2 ? false : new Random().nextBoolean());
            StringAndIOUtils.writeAndFlush(sender, newSession.getSessionID().toString()+":"+
                                                   sender.attr(teamAttribute).get()+":"+
                                                   rules.toString());
            
            sender.pipeline().removeLast();
            sender.pipeline().addLast("shandler", newSession);

            System.out.println(" USER: "+player.getID()+" created a session with ID: "+newSession.getSessionID()+" and has been moved");
            
            server.runSession(newSession);
          }
          else {
            StringAndIOUtils.writeAndFlush(sender, String.format(ServerResponses.BAD_ARGS, 
                ServerRequests.CSESS.toString(),
                ServerRequests.CSESS.argAmount(),
                arguments.size()));
          }
        }       
      }
      else {
        StringAndIOUtils.writeAndFlush(sender, String.format(ServerResponses.BAD_ARGS, 
            ServerRequests.CSESS.toString(),
            ServerRequests.CSESS.argAmount(),
            arguments.size()));
      }
    }
    else {
      StringAndIOUtils.writeAndFlush(sender, String.format(ServerResponses.BAD_REQ, "unknown reqest for staging"));
    }
  }  
}
