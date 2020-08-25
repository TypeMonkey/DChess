package jg.proj.chess.net.server;

import java.io.IOException;
import java.util.Arrays;
import java.util.NoSuchElementException;
import java.util.Random;
import java.util.UUID;
import java.util.stream.Collectors;

import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.util.AttributeKey;
import jg.proj.chess.utils.StringAndIOUtils;
import jg.proj.chess.net.ServerRequest;
import jg.proj.chess.net.ServerResponses;
import jg.proj.chess.net.SessionRules;
import jg.proj.chess.net.SessionStatus;
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
    
    String [] split = msg.split(":"); //split message
    final String requestIdentifier = split[0];  //used by the client to identify requests
    ServerRequest request = null;
    try {
      //the substring operation is to remove the '~' in front of a request
      request = ServerRequest.valueOf(split[1].substring(1).toUpperCase());
    } catch (IllegalArgumentException e) {
      //leave as is...
    }
    
    /*
     * A requests has the format: IDEN:REQ_NAME:arguments
     * So, extract all elements from index 2 to split.length - 1 as those are the actual arguments
     * 
     * If split.length <= 2, then that means the request has insufficient arguments
     */
    final String [] arguments = split.length > 2 ? Arrays.copyOfRange(split, 2, split.length) : new String[0];
    
    System.out.println(" FROM PLAYER "+sender.remoteAddress()+" | "+request+" | original: "+msg.trim());

    if (request != null) {
      if (request.argAmount() == arguments.length) {
        AttributeKey<Boolean> teamAttribute = AttributeKey.valueOf("teamone");
        
        int errorCode = 0; //0 means no error was encountered
        
        /*
         * Null response means that the request warrants no response from the server.
         * 
         * This is useful for requests such as QUIT or DISC that may cause for
         * player disconnections. Attempting to write a response may cause IO errors
         */
        String response = null; 
        
        switch (request) {
          case CUSER:
          { 
            player.setName(arguments[0]);
            response = player.getName()+":"+player.getID();
            break;
          }
          case JOIN:
          {
            UUID sessionUUID = UUID.fromString(arguments[0]);
            int teamID = Integer.parseInt(arguments[1]);
            
            //find the session
            Session session = database.findSession(sessionUUID);
            if (session == null) {
              System.out.println(" -> No session found of ID '"+sessionUUID+"' , req by "+player.getName());
              errorCode = ServerResponses.NO_SESS;
            }
            else if(session.getRules().getProperty(Properties.ALLOW_JOINS_GAME).equals(Boolean.FALSE) ||
                    session.getStatus() != SessionStatus.ACCEPTING){
              //session doesn't allow late joins or isn't currently accepting players -> send error
              errorCode = ServerResponses.NO_JOIN;
            }
            else {
              //player can join session
              sender.attr(teamAttribute).set(teamID == 1 ? true : teamID == 2 ? false : new Random().nextBoolean());
              sender.pipeline().removeLast();
              sender.pipeline().addLast("shandler", session);
              
              response = sessionUUID.toString()+":"+sender.attr(teamAttribute).get()+":"+session.getRules();             
            }
            break;
          }
          case CSESS:
          {
            int teamID = Integer.parseInt(arguments[0]);
            String [] rulesSubArray = Arrays.copyOfRange(arguments, 1, arguments.length);
            String rulesString = Arrays.stream(rulesSubArray).collect(Collectors.joining(":"));
            
            //now parse the rules
            SessionRules sessionRules = SessionRules.parseFromString(rulesString);
            if (sessionRules != null) {
              if ( (long) sessionRules.getProperty(Properties.VOTING_DURATION) <  (long) Properties.VOTING_DURATION.getDefaultValue() ||
                   (int) sessionRules.getProperty(Properties.MIN_TEAM_COUNT) <  (int) Properties.MIN_TEAM_COUNT.getDefaultValue()) {
                System.out.println("---BAD ARGS FOR CSESS: "+sessionRules);
                errorCode = ServerResponses.BAD_ARGS;
              }
              else {
                //no error found in parsing. Continue on
                
                //create session and add it to the database
                Session session = new Session(server, sessionRules);
                database.addSession(session);
                
                sender.attr(teamAttribute).set(teamID == 1 ? true : teamID == 2 ? false : new Random().nextBoolean());            

                sender.pipeline().removeLast();
                sender.pipeline().addLast("shandler", session);
                server.runSession(session);
                
                response = session.getSessionID() + ":" + sender.attr(teamAttribute).get();
              }            
            }
            else {
              errorCode = ServerResponses.WRONG_ARGS;
            }
            
            break;
          }
          case SES:
          {
            if (database.getSessionCount() == 0) {
              //if there are no sessions running, respond with error NO_SES
              errorCode = ServerResponses.NO_SESS;
            }
            else {
              String whole = "";
              for(UUID uuid : database.getAllSessionIDS()) {
                Session session = database.findSession(uuid);
                if (session != null && session.getStatus() == SessionStatus.ACCEPTING) {
                  whole += uuid.toString()+","+
                           session.totalPlayers()+","+
                           session.getRules().getProperty(Properties.PRISON_DILEMMA)+","+
                           session.getRules().getProperty(Properties.VOTING_DURATION)+","+
                           session.getRules().getProperty(Properties.ALLOW_INVL_VOTES)+","+
                           session.getRules().getProperty(Properties.BREAK_AMOUNT)+","+
                           session.getRules().getProperty(Properties.ALLOW_JOINS_GAME)+","+
                           session.getRules().getProperty(Properties.MIN_TEAM_COUNT)+":";
                }
              }
              
              response = whole.substring(0, whole.length() - 1); //remove the last colon    
            }
            break;
          }
          case DISC: 
          {
            System.out.println(" ---->>>> "+player.getName()+" has DISCONNECTED (staging)!!!!");
            
            server.getDatabase().removePlayer(player.getID());
            sender.pipeline().remove(this);
            sender.close();
            
            response = null;
            break;
          }
          case STATUS:
          {
            UUID sessionID = UUID.fromString(arguments[0]);
            Session targetSession = database.findSession(sessionID);
            if (targetSession != null) {
              response = sessionID.toString()+":"+targetSession.getStatus().toString();
            }
            else {
              errorCode = ServerResponses.NO_SESS;
            }
            break;
          }
          default:
          {
            //the rest of the server requests are only available while in a session
            errorCode = ServerResponses.NOT_IN_SESS;
            break;
          }
        }
        
        if (errorCode < 0) {
          //means error was encountered
          StringAndIOUtils.writeAndFlush(sender, 
               requestIdentifier+":"+String.format(ServerResponses.BAD_REQUEST, request.getName(), errorCode));
        }
        else if (response != null) {
          //no error was encountered. Result string has been created
          StringAndIOUtils.writeAndFlush(sender, requestIdentifier+":"+request.getName()+":"+response);
        }   
        
      }
      else {
        //invalid amount of args provided. Send error
        StringAndIOUtils.writeAndFlush(sender, 
            requestIdentifier+":"+String.format(ServerResponses.BAD_REQUEST, request.getName(), ServerResponses.WRONG_ARGS));
      }
    }
    else {
      //no such request exists
      String errorResponse = String.format(ServerResponses.BAD_REQUEST, requestIdentifier, ServerResponses.UNKNOWN);
      System.out.println("--CLIENT: "+player.getName()+" sent an unknow request: "+split[1]);
      StringAndIOUtils.writeAndFlush(sender, errorResponse);
    }
  }  
}
