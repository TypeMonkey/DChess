package jg.proj.chess.net.server;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.NoSuchElementException;
import java.util.PriorityQueue;
import java.util.Random;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import io.netty.channel.Channel;
import io.netty.channel.ChannelHandler.Sharable;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.channel.group.ChannelGroup;
import io.netty.channel.group.DefaultChannelGroup;
import io.netty.util.AttributeKey;
import io.netty.util.concurrent.GlobalEventExecutor;
import jg.proj.chess.core.Board;
import jg.proj.chess.core.DefaultBoardPreparer;
import jg.proj.chess.core.Square;
import jg.proj.chess.core.TeamInformation;
import jg.proj.chess.core.units.InvalidMove;
import jg.proj.chess.core.units.Unit;
import jg.proj.chess.core.units.Unit.UnitType;
import jg.proj.chess.net.ServerRequest;
import jg.proj.chess.net.ServerResponses;
import jg.proj.chess.net.SessionRules;
import jg.proj.chess.net.Vote;
import jg.proj.chess.net.SessionRules.Properties;
import jg.proj.chess.utils.StringAndIOUtils;

/**
 * Represents a game session.
 * 
 * @author Jose
 */
@Sharable
public class Session extends SimpleChannelInboundHandler<String> implements Runnable{
  private static final int DEFAULT_BOARD_SIZE = 8;
  private final GameServer server;
  
  /**
   * The unique id that identifies this session
   */
  private final UUID sessionID;  
  
  /**
   * The game logic for this session
   */
  private final Board board;
  private final TeamInformation teams;
  
  /**
   * The teams of this session
   */
  private final ChannelGroup teamOne;
  private final ChannelGroup teamTwo;
  
  /**
   * List to add votes to. Cleared at every vote end
   */
  private final List<Vote> votes;
  
  /**
   * The rules of this session
   */
  private final SessionRules rules;
  
  /**
   * Round count
   */
  private volatile int currentRound;  
  
  /**
   * Whether the session is active (hasn't ended)
   */
  private volatile boolean running;
  
  /**
   * Whether it's currently team one's turn
   */
  private volatile boolean teamOneTurn;
  
  /**
   * Whether this session is currently accepting votes
   * from either team
   */
  private volatile boolean currentlyVoting;
  
  /**
   * Whether this session has started
   */
  private volatile boolean hasStarted;
  
  /**
   * Whether this session is currently accepting players
   */
  private volatile boolean acceptingPlayers;
  
  public Session(GameServer server, SessionRules rules){
    this.server = server;
    this.rules = rules;
    this.sessionID = UUID.randomUUID();
    
    votes = new CopyOnWriteArrayList<>();
    
    board = new Board(DEFAULT_BOARD_SIZE, DEFAULT_BOARD_SIZE);
    teams = board.initialize(new DefaultBoardPreparer());
    currentRound = 1;
    
    teamOne = new DefaultChannelGroup(GlobalEventExecutor.INSTANCE);
    teamTwo = new DefaultChannelGroup(GlobalEventExecutor.INSTANCE);
    
  }
  
  public boolean equals(Object obj) {
    if (obj instanceof Session) {
      Session other = (Session) obj;
      return other.sessionID.equals(sessionID);
    }
    return false;
  }
  
  public int hashCode() {
    return sessionID.hashCode();
  }
  
  private void msgTeamOne(String message){    
    //message team 1 first
    for(Channel team1Player : teamOne){
      StringAndIOUtils.writeAndFlush(team1Player, message);
    }
  }
  
  private void msgTeamTwo(String message){    
    //message team 1 first
    for(Channel team2Player : teamTwo){
      StringAndIOUtils.writeAndFlush(team2Player, message);
    }
  }
  
  private void msgEveryone(String message){    
    //message team 1 first
    msgTeamOne(message);
    
    //message team 2 first
    msgTeamTwo(message);
  }
  
  private void sendSignalAll(int signal) {
   sendSignallTeam1(signal);
   sendSignallTeam2(signal);
  }
  
  private void sendSignallTeam1(int signal) {
    msgTeamOne(String.format(ServerResponses.SIGNAL_MSG, signal));
  }
  
  private void sendSignallTeam2(int signal) {
    msgTeamTwo(String.format(ServerResponses.SIGNAL_MSG, signal));
  }
  
  @Override
  public void run() {
    if (!running) {
      running = true;

      //Wait until both teams reach the minimum size
      final int minTeamSize = (int) rules.getProperty(Properties.MIN_TEAM_COUNT);
      if (teamOne.size() < minTeamSize || teamTwo.size() < minTeamSize) {
        msgEveryone(String.format(ServerResponses.SERVER_MSG, "----> WAITING FOR MORE PLAYERS <----"));
        acceptingPlayers = true;
        while (teamOne.size() < minTeamSize || teamTwo.size() < minTeamSize) {
          if (teamOne.size() == 0 && teamTwo.size() == 0) {
            //session has been abandoned.
            running = false;
            break;
          }
        }
        acceptingPlayers = false;
      }
      
      hasStarted = true;
      //alert all players that the game has started
      sendSignalAll(ServerResponses.GAME_START);

      //play the game
      boolean hasWon = false;

      teamOneTurn = true;
      while (!hasWon && running) {
        
        /*
         * Check if either team has no players. The team with no players loose 
         * automatically by desertion
         */
        if (teamOne.isEmpty()) {
          sendSignalAll(ServerResponses.TEAM1_DESS);
          hasWon = true;
        }
        else if(teamTwo.isEmpty()){
          sendSignalAll(ServerResponses.TEAM2_DESS);
          hasWon = true;
        }
        else {
          /*
           * starts a makeshift countdown window for voting
           */
          long timeNow = System.currentTimeMillis();
          long projectedEnd = timeNow + TimeUnit.MILLISECONDS.convert(((long) rules.getProperty(Properties.VOTING_DURATION)), TimeUnit.SECONDS);

          //maps a player to their vote
          ConcurrentHashMap<Player, Vote> voterMap = new ConcurrentHashMap<>();

          //voting time window
          final ChannelGroup currentTeam = teamOneTurn ? teamOne : teamTwo;       
          final int currentTeamID = teamOneTurn ? 1 : 2;
          
          System.out.println("---CURRENT TURN: "+currentTeamID+" | "+teamOneTurn);
          if (teamOneTurn) {
            sendSignallTeam1(ServerResponses.VOTE_START);
          }
          else {
            sendSignallTeam2(ServerResponses.VOTE_START);
          }
          System.out.println(" ---warned them----");
          
          currentlyVoting = true;
          while (System.currentTimeMillis() < projectedEnd) {
            /*
             * Keep iterating through the vote list while the 
             * voting window is still active
             */
            for(Vote vote : votes) {
             // System.out.println(" ---> sorting vote: "+vote);
              
              if (currentTeam.contains(vote.getVoter().getChannel())) {
                //make sure to only be sorting votes from the current team
                
                Square origin = board.querySquare(vote.getFileOrigin(), vote.getRankOrigin());
                Square destination = board.querySquare(vote.getFileDest(), vote.getRankDest());
                Unit targetUnit = origin.getUnit();
                
                //only consider vote if it's a valid vote, or if the rules allow for no filtering of bad votes              
                if ( (targetUnit != null && targetUnit.getTeamID() == currentTeamID && targetUnit.possibleDestinations().contains(destination)) || 
                     (rules.getProperty(Properties.ALLOW_INVL_VOTES).equals(Boolean.TRUE)) ) {             
                  voterMap.put(vote.getVoter(), vote);
                }
                else {
                  System.out.println("   NOT A VALID DESTINATION SQUARE");
                  StringAndIOUtils.writeAndFlush(vote.getVoter().getChannel(), 
                      String.format(ServerResponses.BAD_REQUEST, ServerRequest.VOTE.getName(), ServerResponses.BAD_VOTE));
                }        
              
              }
            }
          }
          
          //clear out previous votes and signal vote end
          currentlyVoting = false;
          votes.clear();
          if (teamOneTurn) {
            sendSignallTeam1(ServerResponses.VOTE_END);
          }
          else {
            sendSignallTeam2(ServerResponses.VOTE_END);
          }
          msgEveryone(String.format(ServerResponses.SERVER_MSG, "---> Processing votes! <---"));

          //decide on move based on plurality
          if (!voterMap.isEmpty()) {
            PriorityQueue<VoteCounter> voteQueue = new PriorityQueue<VoteCounter>((x,y) -> y.votes - x.votes);
            
            HashMap<Vote, Integer> voteCounter = new HashMap<>();
            for(Vote voteEntry : voterMap.values()) {
                voteCounter.put(voteEntry, voteCounter.containsKey(voteEntry) ? voteCounter.get(voteEntry) + 1 : 1);             
            }
            
            voteCounter.entrySet().stream().map(x -> new VoteCounter(x.getKey(), x.getValue())).collect(Collectors.toCollection(() -> voteQueue));
            
            msgEveryone(String.format(ServerResponses.SERVER_MSG, "---> Votes processed! <---"));
            
            VoteCounter mostPopular = voteQueue.poll();
            System.out.println(" MOST POPULAR: "+mostPopular.vote.getVoter().getName()+" | "+mostPopular.vote);
            if (mostPopular != null) {
              Square square = board.querySquare(mostPopular.vote.getFileOrigin(), mostPopular.vote.getRankOrigin());
              Square destination = board.querySquare(mostPopular.vote.getFileDest(), mostPopular.vote.getRankDest());

              if (!voteQueue.isEmpty() && voteQueue.peek().votes == mostPopular.votes) {
                //there's a tie in vote
                sendSignalAll( currentTeamID == 1 ? ServerResponses.TEAM1_TIED : ServerResponses.TEAM2_TIED);
                System.out.println(" ----> TEAM "+currentTeamID+" is tied on a move. NO MOVE FROM THEM! <----");
              }
              else if(square.getUnit() == null){
                sendSignalAll( currentTeamID == 1 ? ServerResponses.TEAM1_NO_UNIT : ServerResponses.TEAM2_NO_UNIT);
                System.out.println(" ----> TEAM "+currentTeamID+" has voted to move a none existant unit. NO MOVE MADE! <---- ");
              }
              else if (square.getUnit().getTeamID() != currentTeamID) {
                sendSignalAll( currentTeamID == 1 ? ServerResponses.TEAM1_OTHER_UNIT : ServerResponses.TEAM2_OTHER_UNIT);
                System.out.println(" ----> TEAM "+currentTeamID+" has voted to move a unit THAT'S NOT THEIRS!. NO MOVE MADE! <---- ");
              }
              else {
                try {
                  square.getUnit().moveTo(destination);
                  //send "res" message
                  msgEveryone(String.format(ServerResponses.RESULT_MSG, square.getFile(), square.getRank(), destination.getFile(), destination.getRank()));                 
                } catch (InvalidMove e) {
                  sendSignalAll( currentTeamID == 1 ? ServerResponses.TEAM1_IDIOT_VOTE : ServerResponses.TEAM2_IDIOT_VOTE);
                  System.out.println("----> TEAM "+currentTeamID+" has voted on an invalid move. NO MOVE FROM THEM! <----");
                }
                
              }
              
            }
          }
          else if(teamOneTurn){
            sendSignalAll(ServerResponses.TEAM1_NO_VOTE);
            System.out.println("[SERVER] No vote has been made by Team 1. No movement will be made for this turn....");
          }
          else {
            sendSignalAll(ServerResponses.TEAM2_NO_VOTE);
            System.out.println("[SERVER] No vote has been made by Team 2. No movement will be made for this turn....");
          }

          if (teams.getTeamOne().get(UnitType.KING).get(0).getCurrentSquare() == null) {
            hasWon = true;
            sendSignalAll(ServerResponses.TEAM1_WON);
            System.out.println("[SERVER] TEAM ONE WON!");
          }
          else if (teams.getTeamOne().get(UnitType.KING).get(0).getCurrentSquare() == null) {
            hasWon = true;
            sendSignalAll(ServerResponses.TEAM2_WON);
            System.out.println("[SERVER] TEAM TWO WON!");
          }         
        }
        //switch turns
        teamOneTurn = teamOneTurn ? false : true;
        currentRound++;
      }

      //clear both teams of members
      teamOne.clear();
      teamTwo.clear();
      
      running = false;  
    }
    
    //remove session from database
    System.out.println("---REMOVED SESSION: "+sessionID);
    server.getDatabase().removeSession(sessionID);
  }
  
  @Override
  public void handlerAdded(ChannelHandlerContext ctx) throws Exception {
    //tell everyone in the session that someone joined
    Channel playerChannel = ctx.channel();
    Player player = (Player) playerChannel.attr(AttributeKey.valueOf("player")).get();
    player.setSession(this);    
        
    boolean isTeamOne = (boolean) playerChannel.attr(AttributeKey.valueOf("teamone")).get();
    if (isTeamOne) {
      System.out.println(" USER: "+player.getName()+" added to Team One of session "+sessionID);
      teamOne.add(playerChannel);
    }
    else {
      System.out.println(" USER: "+player.getName()+" added to Team Two of session "+sessionID);
      teamTwo.add(playerChannel);
    }
    
    sendSignalAll(ServerResponses.PLAYER_JOINED);
    if (currentlyVoting) {
      if (teamOneTurn && isTeamOne) {      
        StringAndIOUtils.writeAndFlush(playerChannel, String.format(ServerResponses.SIGNAL_MSG, ServerResponses.VOTE_START));
        System.out.println("-----> "+player.getName()+"'s team IS CURRENTLY VOTING <-----");
      }
      else if (!teamOneTurn && !isTeamOne) {
        StringAndIOUtils.writeAndFlush(playerChannel, String.format(ServerResponses.SIGNAL_MSG, ServerResponses.VOTE_START));    
        System.out.println("-----> "+player.getName()+"'s team IS CURRENTLY VOTING <-----");
      }
    }
    
    System.out.println("---sent newbie welcom---");
  }
  
  @Override
  public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
    //if an IO error is caught, just remove the player from the server
    if (cause instanceof IOException) {
      Player player = (Player) ctx.channel().attr(AttributeKey.valueOf("player")).get();
      server.getDatabase().removePlayer(player.getID());
      
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
  public void handlerRemoved(ChannelHandlerContext ctx) throws Exception {
    //tell everyone in the session that someone left
    Channel playerChannel = ctx.channel();
    Player player = (Player) playerChannel.attr(AttributeKey.valueOf("player")).get();
    player.setSession(null);
    
    boolean isTeamOne = (boolean) playerChannel.attr(AttributeKey.valueOf("teamone")).get();
    if (isTeamOne) {
      teamOne.remove(playerChannel);
    }
    else {
      teamTwo.remove(playerChannel);
    }
        
    if (server.getDatabase().findPlayer(player.getID()) != null) {
      //player dropped off unexpectedly. No quit request sent
      sendSignalAll(ServerResponses.PLAYER_LEFT);
      msgEveryone(String.format(ServerResponses.SERVER_MSG, player.getName()+" has left the session!"));
      System.out.println("[SERVER] "+player.getName()+" has left the session!");
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
    
    System.out.println(" FROM PLAYER (SESS) "+sender.remoteAddress()+" | "+request+" | original: "+msg.trim());

    if (request != null) {
      if (request.argAmount() == arguments.length) {
        AttributeKey<Boolean> teamAttribute = AttributeKey.valueOf("teamone");
        
        int errorCode = 0; //0 means no error was encountered
        String response = null; 
        
        switch (request) {
          case CUSER:
          { 
            player.setName(arguments[0]);
            response = player.getName()+":"+player.getID();
            break;
          }
          case SES:
          {
            if (server.getDatabase().getSessionCount() == 0) {
              //if there are no sessions running, respond with error NO_SES
              errorCode = ServerResponses.NO_SESS;
            }
            else {
              String whole = "";
              for(UUID uuid : server.getDatabase().getAllSessionIDS()) {
                Session session = server.getDatabase().findSession(uuid);
                if (session != null && session.isAcceptingPlayers()) {
                  whole += uuid.toString()+","+
                           session.totalPlayers()+","+
                           session.getRules().getProperty(Properties.PRISON_DILEMMA)+","+
                           session.getRules().getProperty(Properties.VOTING_DURATION)+","+
                           session.getRules().getProperty(Properties.ALLOW_INVL_VOTES)+":";
                }
              }
              response = whole;    
            }
            break;
          }
          case DISC: 
          {
            System.out.println(" ---->>>> "+player.getName()+" has DISCONNECTED (staging)!!!!");
            
            //remove them from their team
            if (teamOne.contains(sender)) {
              //remove sender from team one
              System.out.println(" ---->>>> "+player.getName()+" (TEAM ONE) has LEFT!");
              teamOne.remove(sender);
            }
            else {
              //remove sender from team two
              System.out.println("[SERVER] "+player.getName()+" (TEAM TWO) has LEFT!");
              teamTwo.remove(sender);
            }
            
            sendSignalAll(ServerResponses.PLAYER_LEFT);
            
            server.getDatabase().removePlayer(player.getID());
            sender.pipeline().remove(this);
            sender.close();
            
            response = "bye";
            break;
          }
          case QUIT:
          {
            if (teamOne.contains(sender)) {
              //remove sender from team one
              System.out.println(" ---->>>> "+player.getName()+" (TEAM ONE) has LEFT!");
              teamOne.remove(sender);
            }
            else {
              //remove sender from team two
              System.out.println("[SERVER] "+player.getName()+" (TEAM TWO) has LEFT!");
              teamTwo.remove(sender);
            }
            
            sendSignalAll(ServerResponses.PLAYER_LEFT);
            
            sender.pipeline().addLast("handler", new StagingHandler(server));    
            StringAndIOUtils.writeAndFlush(sender, ServerRequest.QUIT.getName());
            sender.pipeline().remove(this);
          
            response = "bye";
            break;
          }
          case TALLY:
          {
            if (rules.getProperty(Properties.PRISON_DILEMMA).equals(Boolean.TRUE)) {
              errorCode = ServerResponses.PRISON_DIL;
            }
            else {
              //Count votes
              HashMap<Vote, Integer> voteCount = new HashMap<>();
              ChannelGroup playerTeam = teamOne.contains(sender) ? teamOne : teamTwo;
              
              for (Vote vote : votes) {
                if (playerTeam.contains(vote.getVoter().getChannel())) {
                  //only count the player's team votes
                  voteCount.put(vote, voteCount.containsKey(vote) ? voteCount.get(vote) + 1 : 1);
                }
              }
              
              if (!voteCount.isEmpty()) {
                //now, concat all votes
                response = voteCount.entrySet().stream().map(x -> x.getKey().toString() + ">" + x.getValue()).collect(Collectors.joining(":"));               
              }
              else {
                errorCode = ServerResponses.NO_TALLY;
              }               
            }
            break;
          }
          case VOTE:
          {
            if (currentlyVoting) {
              final int currentVotingTeam = teamOneTurn ? 1 : 2;
              final int senderTeam = sender.attr(teamAttribute).get() ? 1 : 2;
              
              if (currentVotingTeam == senderTeam) {
                /*
                 * If session is currently voting and it's team one turn
                 * and the sender of this vote is in team one
                 */
                int fromFile = Integer.parseInt(arguments[0]);
                char fromRank = arguments[1].charAt(0);
                int destFile = Integer.parseInt(arguments[2]);
                char destRank = arguments[3].charAt(0);
                
                Vote vote = new Vote(fromFile, fromRank, destFile, destRank, player);
                votes.add(vote);
                response = vote.toString();
                
                if (senderTeam == 1) {
                  sendSignallTeam1(ServerResponses.VOTE_RECIEVED);
                }
                else {
                  sendSignallTeam2(ServerResponses.VOTE_RECIEVED);
                }
              }           
              else {
                errorCode = ServerResponses.NO_VOTE;
              }
            }
            else {
              errorCode = ServerResponses.NO_VOTE;
            }
            break;
          }
          case PLIST: 
          {
            boolean includeUUID = Boolean.parseBoolean(arguments[0].toLowerCase());
            AttributeKey<Player> playerKey = AttributeKey.valueOf("player");
            
            //get team one first, then team two
            String mess = "";
            for (Channel channel : teamOne) {
              Player attachedPlayer = channel.attr(playerKey).get();
              mess += attachedPlayer.getName()+",true"+(includeUUID ? ","+attachedPlayer.getID() : "");
              mess += ":";
            }
            
            for (Channel channel : teamTwo) {
              Player attachedPlayer = channel.attr(playerKey).get();
              mess += attachedPlayer.getName()+",false"+(includeUUID ? ","+attachedPlayer.getID() : "");
              mess += ":";
            }
            
            //remove trailing semicolon
            if (!mess.isEmpty()) {
              mess = mess.substring(0, mess.length() - 1);
            }
            
            response = mess;
          
            break;
          }
          case UPDATE:
          {
            response = board.parsableToString();
            break;
          }
          case ALL:
          {
            if (rules.getProperty(Properties.PRISON_DILEMMA).equals(Boolean.TRUE)) {
              errorCode = ServerResponses.PRISON_DIL;
            }
            else {
              String message = Arrays.stream(arguments).collect(Collectors.joining());
              response = player.getName()+":"+message;  
              
              //message everyone
              msgEveryone(message);
            }
            break;
          }
          case TEAM:
          {
            if (rules.getProperty(Properties.PRISON_DILEMMA).equals(Boolean.TRUE)) {
              errorCode = ServerResponses.PRISON_DIL;
            }
            else {
              String message = Arrays.stream(arguments).collect(Collectors.joining());
              response = player.getName()+":"+message;      
              
              if (sender.attr(teamAttribute).get()) {
                //send to team one
                msgTeamOne(message);
              }
              else {
                //send to team two
                msgTeamTwo(message);
              }
            }
            break;
          }
          default:
          {
            //the rest of the server requests are only available while NOT in a session
            errorCode = ServerResponses.IN_SESS;
            break;
          }
        }
        
        if (errorCode < 0) {
          //means error was encountered
          StringAndIOUtils.writeAndFlush(sender, 
               requestIdentifier+":"+request.createErrorString(errorCode));
        }
        else {
          //no error was encountered. Result string has been created
          StringAndIOUtils.writeAndFlush(sender, requestIdentifier+":"+request.getName()+":"+response);
        }
        
      }
      else {
        //invalid amount of args provided. Send error
        StringAndIOUtils.writeAndFlush(sender, 
            requestIdentifier+":"+request.createErrorString(ServerResponses.WRONG_ARGS));
      }
    }
    else {
      //no such request exists
      String errorResponse = String.format(ServerResponses.BAD_REQUEST, requestIdentifier, ServerResponses.UNKNOWN);
      System.out.println("--CLIENT: "+player.getName()+" sent an unknow request: "+split[1]);
      StringAndIOUtils.writeAndFlush(sender, errorResponse);
    }
  
  }
    
  public boolean isRunning(){
    return running;
  }
  
  public boolean isAcceptingPlayers() {
    return acceptingPlayers;
  }
  
  public boolean hasStarted() {
    return hasStarted;
  }
  
  protected Board getBoard(){
    return board;
  }
  
  public int getRounds(){
    return currentRound;
  }
  
  public UUID getSessionID(){
    return sessionID;
  }
  
  public int totalPlayers(){
    return teamOne.size() + teamTwo.size();
  }
  
  public int teamOneSize() {
    return teamOne.size();
  }
  
  public int teamTwoSize() {
    return teamTwo.size();
  }
  
  public SessionRules getRules(){
    return rules;
  }
}
