package jg.proj.chess.net.server;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.PriorityQueue;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
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
import jg.proj.chess.core.units.Unit.UnitType;
import jg.proj.chess.net.ServerRequest;
import jg.proj.chess.net.ServerResponses;
import jg.proj.chess.net.StringAndIOUtils;
import jg.proj.chess.net.Vote;
import jg.proj.chess.net.server.SessionRules.Properties;

/**
 * Represents a game session.
 * 
 * @author Jose
 */
@Sharable
public class Session extends SimpleChannelInboundHandler<String> implements Runnable{
  private final GameServer server;
  
  private final UUID sessionID;  
  private final Board board;
  private final TeamInformation teams;
  
  private final ChannelGroup teamOne;
  private final ChannelGroup teamTwo;
  
  private final ArrayBlockingQueue<Vote> voteQueue;
  
  private final SessionRules rules;
  
  private volatile int currentRound;  
  private volatile boolean running;
  private volatile boolean teamOneTurn;
  private volatile boolean currentlyVoting;
  
  private volatile boolean acceptingPlayers;
  
  public Session(GameServer server, SessionRules rules){
    this.server = server;
    this.rules = rules;
    this.sessionID = UUID.randomUUID();
    
    voteQueue = new ArrayBlockingQueue<>(10);
    
    board = new Board(8, 8);
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
  
  @Override
  public void run() {
    if (!running) {
      running = true;

      //Wait until both teams reach the minimum size
      int minTeamSize = (int) rules.getProperty(Properties.MIN_TEAM_COUNT);
      if (teamOne.size() < minTeamSize && teamTwo.size() < minTeamSize) {
        messageEveryone("----> WAITING FOR MORE PLAYERS <----");
        acceptingPlayers = true;
        while (teamOne.size() < minTeamSize && teamTwo.size() < minTeamSize);
        acceptingPlayers = false;
      }
      
      //alert all players that the game has started
      messageEveryone("----> GAME STARTED! <----");

      //play the game
      boolean hasWon = false;

      teamOneTurn = true;
      while (!hasWon) {
        
        /*
         * Check if either team has no players. The team with no players loose 
         * automatically by desertion
         */
        if (teamOne.isEmpty()) {
          messageEveryone("[SERVER] Team one has deserted the battle. TEAM TWO WINS!");
          hasWon = true;
        }
        else if(teamTwo.isEmpty()){
          messageEveryone("[SERVER] Team two has deserted the battle. TEAM ONE WINS!");
          hasWon = true;
        }
        else {
          /*
           * starts a make shift countdown window for voting
           */
          long timeNow = System.currentTimeMillis();
          long projectedEnd = timeNow + TimeUnit.MILLISECONDS.convert(((long) rules.getProperty(Properties.VOTING_DURATION)), TimeUnit.SECONDS);

          ConcurrentHashMap<Vote, Set<Player>> voteMap = new ConcurrentHashMap<>();
          ConcurrentHashMap<Player, Vote> votedPlayers = new ConcurrentHashMap<>();

          //voting time window
          ChannelGroup currentTeam = teamOneTurn ? teamOne : teamTwo;       
          int currentTeamID = teamOneTurn ? 1 : 2;
          
          System.out.println("---CURRENT TURN: "+currentTeamID);
          if (teamOneTurn) {
            messageTeamOne(null, ServerResponses.VOTE_NOW);
          }
          else {
            messageTeamTwo(null, ServerResponses.VOTE_NOW);
          }
          System.out.println(" ---warned them----");
          
          currentlyVoting = true;
          while (System.currentTimeMillis() < projectedEnd) {
            try {
              Vote vote = voteQueue.poll(0, TimeUnit.MILLISECONDS);
              if (vote == null) {
                continue;
              }

              System.out.println("  -> HAS VOTED: "+vote);
              
              if (currentTeam.contains(vote.getVoter().getChannel())) {
                Square square = board.querySquare(vote.getFileOrigin(), vote.getRankOrigin());
                Square destination = board.querySquare(vote.getFileDest(), vote.getRankDest());
                System.out.println("  ---> SOURCE SQUARE??? "+square.getUnit());
                
                //only consider vote if it's a valid vote, or if the rules allow for no filtering of bad votes              
                if ( (square.getUnit() != null && square.getUnit().getTeamID() == currentTeamID && square.getUnit().possibleDestinations().contains(destination)) || 
                     (rules.getProperty(Properties.ALLOW_INVL_VOTES).equals(Boolean.TRUE)) ) {                  
                  if (!voteMap.containsKey(vote)) {
                    HashSet<Player> votees = new HashSet<>();
                    votees.add(vote.getVoter());
                    voteMap.put(vote, votees);
                  }
                  else {
                    voteMap.get(vote).add(vote.getVoter());
                  }
                  
                  //if player already voted, then update the votemap to discount double counting
                  if (votedPlayers.contains(vote.getVoter())) {
                    voteMap.get(vote).remove(vote.getVoter());
                  }
                  else {
                    votedPlayers.put(vote.getVoter(), vote);
                  }

                }
                else {
                  System.out.println("   NOT A VALID DESTINATION SQUARE");
                  StringAndIOUtils.writeAndFlush(vote.getVoter().getChannel(), 
                      String.format(ServerResponses.INVALID_VOTE, 
                          vote.getFileOrigin(), 
                          vote.getRankOrigin(), 
                          vote.getFileDest(), 
                          vote.getRankDest()));
                }        
              }
              else {
                System.out.println("   PLAYER: "+vote.getVoter().getName()+" isn't in "+currentTeamID);
                StringAndIOUtils.writeAndFlush(vote.getVoter().getChannel(), 
                    String.format(ServerResponses.INVALID_VOTE, 
                        vote.getFileOrigin(), 
                        vote.getRankOrigin(), 
                        vote.getFileDest(), 
                        vote.getRankDest()));
              }
            } catch (InterruptedException e) {
              System.out.println("---INTERRUPTED");
            }
          }
          currentlyVoting = false;
          voteQueue.clear();
          
          //tell all players that voting has ended
          System.out.println("    > voting done");
          if (teamOneTurn) {
            messageTeamOne(null, ServerResponses.VOTE_DONE);
          }
          else {
            messageTeamTwo(null, ServerResponses.VOTE_DONE);
          }

          //decide on move based on plurality
          if (!voteMap.isEmpty()) {
            PriorityQueue<VoteCounter> voteQueue = new PriorityQueue<VoteCounter>((x,y) -> y.votes - x.votes);
            voteMap.entrySet().stream().map(x -> new VoteCounter(x.getKey(), x.getValue().size())).collect(Collectors.toCollection(() -> voteQueue));
            
            VoteCounter mostPopular = voteQueue.poll();
            System.out.println(" MOST POPULAR: "+mostPopular.vote.getVoter().getName()+" | "+mostPopular.vote);
            if (mostPopular != null) {
              Square square = board.querySquare(mostPopular.vote.getFileOrigin(), mostPopular.vote.getRankOrigin());
              Square destination = board.querySquare(mostPopular.vote.getFileDest(), mostPopular.vote.getRankDest());

              if (!voteQueue.isEmpty() && voteQueue.peek().votes == mostPopular.votes) {
                messageEveryone(" ----> TEAM "+currentTeamID+" is tied on a move. NO MOVE FROM THEM! <----");
              }
              else if(square.getUnit() == null){
                messageEveryone(" ----> TEAM "+currentTeamID+" has voted to move a none existant unit. NO MOVE MADE! <---- ");
              }
              else if (square.getUnit().getTeamID() == currentTeamID) {
                messageEveryone(" ----> TEAM "+currentTeamID+" has voted to move a unit THAT'S NOT THEIRS!. NO MOVE MADE! <---- ");
              }
              else {
                try {
                  square.getUnit().moveTo(destination);
                } catch (InvalidMove e) {
                  messageEveryone(" ----> TEAM "+currentTeamID+" has voted on an invalid move. NO MOVE MADE! <---- ");
                  System.out.println("----> TEAM "+currentTeamID+" has voted on an invalid move. NO MOVE FROM THEM! <----");
                }
              }

              messageEveryone(board.toString());
            }
          }
          else if(teamOneTurn){
            messageTeamOne(null, "[SERVER] No vote has been made. No movement will be made for this turn....");
          }
          else {
            messageTeamOne(null, "[SERVER] No vote has been made. No movement will be made for this turn....");
          }

          if (teams.getTeamOne().get(UnitType.KING).get(0).getCurrentSquare() == null) {
            hasWon = true;
            messageEveryone("[SERVER] TEAM ONE WON!");
          }
          else if (teams.getTeamOne().get(UnitType.KING).get(0).getCurrentSquare() == null) {
            hasWon = true;
            messageEveryone("[SERVER] TEAM TWO WON!");
          }
        }

        //switch turns
        teamOneTurn = teamOneTurn ? false : true;
      }

      //clear both teams of members
      teamOne.clear();
      teamTwo.clear();
      
      running = false;  
    }
  }
  
  private void messageTeamOne(Channel sender, String message){
    //message team 1 first
    for(Channel team1Player : teamOne){
      if (sender != null || team1Player != sender){
        StringAndIOUtils.writeAndFlush(team1Player, message);
      }
    }
  }
  
  private void messageTeamTwo(Channel sender, String message){
    //message team 1 first
    for(Channel team2Player : teamTwo){
      if (sender != null || team2Player != sender) {
        StringAndIOUtils.writeAndFlush(team2Player, message);
      }
    }
  }
  
  private void messageEveryone(String message){    
    //message team 1 first
    messageTeamOne(null, message);
    
    //message team 2 first
    messageTeamTwo(null, message);
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
    
    messageEveryone("[SERVER] "+player.getName()+" has joined the session!");
    if (teamOneTurn && isTeamOne) {
      StringAndIOUtils.writeAndFlush(playerChannel, "-----> YOUR TEAM IS CURRENTLY VOTING <-----");
    }
    else if (!teamOneTurn && !isTeamOne) {
      StringAndIOUtils.writeAndFlush(playerChannel, "-----> YOUR TEAM IS CURRENTLY VOTING <-----");
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
    
    playerChannel.close().sync();
    
    if (server.getDatabase().findPlayer(player.getID()) != null) {
      //player dropped off unexpectedly. No quit request sent
      messageEveryone("[SERVER] "+player.getName()+" has left the session!");
      server.getDatabase().removePlayer(player.getID());
    }
  }
  
  @Override
  protected void channelRead0(ChannelHandlerContext ctx, String msg) throws Exception {
    Channel sender = ctx.channel();
    Player player = (Player) sender.attr(AttributeKey.valueOf("player")).get();
    System.out.println("FROM: "+ctx.channel().remoteAddress()+" | "+msg);
   
    ArrayList<String> arguments = new ArrayList<String>(Arrays.asList(msg.split(":")));
    String first = arguments.remove(0);
   
    System.out.println("   IS COMMAND! "+first);
         
    if (first.equals("~cuser")) {
      if (arguments.size() == ServerRequest.CUSER.argAmount()) {
        player.setName(arguments.get(0));
        StringAndIOUtils.writeAndFlush(sender, ServerRequest.CUSER.getName()+":"+arguments.get(0)+":"+player.getID().toString());
      }
      else {
        StringAndIOUtils.writeAndFlush(sender, ServerRequest.CUSER.createErrorString(String.format(ServerResponses.BAD_ARGS, 
            ServerRequest.CUSER, 
            ServerRequest.CUSER.argAmount(), 
            arguments.size())));
      }
    }
    else if (first.equals("~vote")) {
      String rawVote = arguments.stream().collect(Collectors.joining());
      Vote vote = Vote.parseVote(rawVote, player);
      if (vote != null) {
        if (currentlyVoting) {
          voteQueue.put(vote);
          StringAndIOUtils.writeAndFlush(sender, ServerRequest.VOTE.getName()+":"+vote.toString());
        }
        else {
          StringAndIOUtils.writeAndFlush(sender, ServerRequest.VOTE.createErrorString(String.format(ServerResponses.NOT_VOTING, 
              vote.getFileOrigin(),
              vote.getRankOrigin(),
              vote.getFileDest(),
              vote.getRankDest())));
        }
      }
      else {
        StringAndIOUtils.writeAndFlush(sender, ServerRequest.VOTE.createErrorString("Can't parse vote '"+rawVote+"'"));
      }
    }
    else if (first.equals("~plist")) {
      if (arguments.size() == ServerRequest.PLIST.argAmount()) {
        boolean includeUUID = Boolean.parseBoolean(arguments.get(0).toLowerCase());
        AttributeKey<Player> playerKey = AttributeKey.valueOf("player");
        AttributeKey<Boolean> teamKey = AttributeKey.valueOf("teamone");
        
        //get team one first, then team two
        String mess = "";
        for (Channel channel : teamOne) {
          Player attachedPlayer = channel.attr(playerKey).get();
          boolean isTeamOne = channel.attr(teamKey).get();
          mess += attachedPlayer.getName()+","+isTeamOne+(includeUUID ? ","+attachedPlayer.getID() : "");
          mess += ":";
        }
        
        for (Channel channel : teamTwo) {
          Player attachedPlayer = channel.attr(playerKey).get();
          boolean isTeamOne = channel.attr(teamKey).get();
          mess += attachedPlayer.getName()+","+isTeamOne+(includeUUID ? ","+attachedPlayer.getID() : "");
          mess += ":";
        }
        
        //remove trailing semicolon
        if (!mess.isEmpty()) {
          mess = mess.substring(0, mess.length() - 1);
        }
        StringAndIOUtils.writeAndFlush(sender, mess);
      }
      else {
        StringAndIOUtils.writeAndFlush(sender, ServerRequest.PLIST.createErrorString(String.format(ServerResponses.BAD_ARGS, 
            ServerRequest.PLIST, 
            ServerRequest.PLIST.argAmount(), 
            arguments.size())));
      }
    }
    else if (first.equals("~update")) {
      System.out.println("  ---UPDATING");
      StringAndIOUtils.writeAndFlush(sender, ServerRequest.UPDATE.getName()+":"+board.parsableToString());
      System.out.println(" ---SENT BOARD");
    }
    else if (first.equals("~quit")) {
      if (teamOne.contains(sender)) {
        //warn team one
        messageTeamOne(sender, "[SERVER] "+player.getName()+" has LEFT!");
        teamOne.remove(sender);
      }
      else {
        //warn team two
        messageTeamTwo(sender, "[SERVER] "+player.getName()+" has LEFT!");
        teamTwo.remove(sender);
      }
      
      server.getDatabase().removePlayer(player.getID());
      sender.pipeline().remove(this);
      sender.close();
    }
    else if (first.equals("~ses")) {
      if (arguments.size() == ServerRequest.SES.argAmount()) {        
        String whole = "";
        for(UUID uuid : server.getDatabase().getAllSessionIDS()) {
          Session session = server.getDatabase().findSession(uuid);
          if (session != null && session.isAcceptingPlayers()) {
            whole += uuid.toString()+","+session.totalPlayers()+","+session.getRules().getProperty(Properties.PRISON_DILEMMA)+":";
          }
        }
        
        StringAndIOUtils.writeAndFlush(sender, ServerRequest.SES.getName()+":"+whole);
      }
      else {
        StringAndIOUtils.writeAndFlush(sender, ServerRequest.SES.createErrorString(String.format(ServerResponses.BAD_ARGS, 
            ServerRequest.SES.toString(),
            ServerRequest.SES.argAmount(),
            arguments.size())));
      }
    }
    else if (first.equals("~all")) {
      if (arguments.size() == ServerRequest.ALL.argAmount()) {
        if (rules.getProperty(Properties.PRISON_DILEMMA).equals(Boolean.TRUE)) {
          StringAndIOUtils.writeAndFlush(sender, ServerRequest.ALL.createErrorString("Prisoner's Dilemmas enforced!"));
        }
        else {
          String message = arguments.get(0);
          messageEveryone(message);
          
          StringAndIOUtils.writeAndFlush(sender, String.format(ServerResponses.ALL_MESS, player.getName(), message));
        }
      }
      else {
        StringAndIOUtils.writeAndFlush(sender, ServerRequest.ALL.createErrorString(String.format(ServerResponses.BAD_ARGS, 
            ServerRequest.ALL, 
            ServerRequest.ALL.argAmount(), 
            arguments.size())));
      }
    }
    else if (first.equals("~team")) {
      if (arguments.size() == ServerRequest.TEAM.argAmount()) {
        if (rules.getProperty(Properties.PRISON_DILEMMA).equals(Boolean.TRUE)) {
          StringAndIOUtils.writeAndFlush(sender, ServerRequest.TEAM.createErrorString("Prisoner's Dilemmas enforced!"));
        }
        else {
          String message = arguments.get(0);
          StringAndIOUtils.writeAndFlush(sender, String.format(ServerResponses.TEAM_MESS, player.getName(), message));
          
          if (teamOne.contains(sender)) {
            messageTeamOne(sender, message);
          }
          else {
            messageTeamTwo(sender, message);
          }          
        }
      }
      else {
        StringAndIOUtils.writeAndFlush(sender, ServerRequest.TEAM.createErrorString(String.format(ServerResponses.BAD_ARGS, 
            ServerRequest.TEAM, 
            ServerRequest.TEAM.argAmount(), 
            arguments.size())));
      }
    }
    else {
      StringAndIOUtils.writeAndFlush(sender, String.format(ServerResponses.BAD_REQ, "!", "Unknown request for session"));
    }  
  }
  
  public boolean isRunning(){
    return running;
  }
  
  public boolean isAcceptingPlayers() {
    return acceptingPlayers;
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
  
  public SessionRules getRules(){
    return rules;
  }
    
  public static class VoteCounter{
    
    public final Vote vote; 
    public final int votes;
    
    public VoteCounter(Vote vote, int voteCount) {
      this.vote = vote;
      this.votes = voteCount;
    }
    
    @Override
    public String toString() {
      return "CNT: "+votes+" , "+vote;
    }
  }
}
