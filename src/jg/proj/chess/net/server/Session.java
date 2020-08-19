package jg.proj.chess.net.server;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Map.Entry;
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
  
  private volatile boolean hasStarted;
  
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
    msgTeamOne(String.format(ServerResponses.SIGNAL, signal));
  }
  
  private void sendSignallTeam2(int signal) {
    msgTeamTwo(String.format(ServerResponses.SIGNAL, signal));
  }
  
  @Override
  public void run() {
    if (!running) {
      running = true;

      //Wait until both teams reach the minimum size
      int minTeamSize = (int) rules.getProperty(Properties.MIN_TEAM_COUNT);
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

          ConcurrentHashMap<Vote, Set<Player>> voteMap = new ConcurrentHashMap<>();
          ConcurrentHashMap<Player, Vote> votedPlayers = new ConcurrentHashMap<>();

          //voting time window
          ChannelGroup currentTeam = teamOneTurn ? teamOne : teamTwo;       
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
                      String.format(ServerResponses.BAD_REQUEST, ServerRequest.VOTE.getName(), ServerResponses.BAD_VOTE));
                }        
              }
              else {
                System.out.println("   PLAYER: "+vote.getVoter().getName()+" isn't in "+currentTeamID);
                StringAndIOUtils.writeAndFlush(vote.getVoter().getChannel(), 
                        String.format(ServerResponses.BAD_REQUEST, ServerRequest.VOTE.getName(), ServerResponses.BAD_VOTE));
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
            sendSignallTeam1(ServerResponses.VOTE_END);
          }
          else {
            sendSignallTeam2(ServerResponses.VOTE_END);
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
        //turn has ended
        sendSignalAll(ServerResponses.TURN_END);

        //switch turns
        teamOneTurn = teamOneTurn ? false : true;
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
        StringAndIOUtils.writeAndFlush(playerChannel, String.format(ServerResponses.SIGNAL, ServerResponses.VOTE_START));
        System.out.println("-----> "+player.getName()+"'s team IS CURRENTLY VOTING <-----");
      }
      else if (!teamOneTurn && !isTeamOne) {
        StringAndIOUtils.writeAndFlush(playerChannel, String.format(ServerResponses.SIGNAL, ServerResponses.VOTE_START));    
        System.out.println("-----> "+player.getName()+"'s team IS CURRENTLY VOTING <-----");
      }
    }
    else {
      StringAndIOUtils.writeAndFlush(playerChannel, String.format(ServerResponses.SIGNAL, ServerResponses.VOTE_END));
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
    System.out.println("FROM: "+ctx.channel().remoteAddress()+" | "+msg);
   
    ArrayList<String> arguments = new ArrayList<String>(Arrays.asList(msg.split(":")));
    String first = arguments.remove(0);
   
    System.out.println("   IS COMMAND! "+first);
         
    if (first.equals(ServerRequest.CUSER.getReqName())) {
      if (arguments.size() == ServerRequest.CUSER.argAmount()) {
        player.setName(arguments.get(0));
        StringAndIOUtils.writeAndFlush(sender, ServerRequest.CUSER.getName()+":"+arguments.get(0)+":"+player.getID().toString());
      }
      else {
        StringAndIOUtils.writeAndFlush(sender, ServerRequest.CUSER.createErrorString(ServerResponses.WRONG_ARGS));
      }
    }
    else if (first.equals(ServerRequest.TALLY.getReqName())) {
      //if session is in prisoner's dilemma, send error back
      if (rules.getProperty(Properties.PRISON_DILEMMA).equals(Boolean.TRUE)) {
        StringAndIOUtils.writeAndFlush(sender, ServerRequest.TALLY.createErrorString(ServerResponses.PRISON_DIL));
      }
      else {
        //Count votes
        HashMap<Vote, Integer> voteCount = new HashMap<>();
        ChannelGroup playerTeam = teamOne.contains(sender) ? teamOne : teamTwo;
        
        for (Vote vote : voteQueue) {
          if (playerTeam.contains(vote.getVoter().getChannel())) {
            //only count the player's team votes
            if (voteCount.containsKey(vote)) {
              voteCount.put(vote, voteCount.get(vote) + 1);
            }
            else {
              voteCount.put(vote, 1);
            }
          }
        }
        
        //now, concat all votes
        String tally = "";
        for (Entry<Vote, Integer> voteCountEntry : voteCount.entrySet()) {
          tally += voteCountEntry.getKey().toString()+">"+voteCountEntry.getValue()+":";
        }
        
        //if no votes have been received, then just send empty string
        tally = tally.isEmpty() ? tally : tally.substring(0, tally.length() - 1);
        
        //send tally over
        StringAndIOUtils.writeAndFlush(sender, ServerRequest.TALLY.getName()+":"+tally);
      }
    }
    else if (first.equals(ServerRequest.VOTE.getReqName())) {
      if (arguments.size() == ServerRequest.VOTE.argAmount()) {
        int fromFile = Integer.parseInt(arguments.remove(0));
        char fromRank = arguments.remove(0).charAt(0);
        int destFile = Integer.parseInt(arguments.remove(0));
        char destRank = arguments.remove(0).charAt(0);
        
        Vote vote = new Vote(fromFile, fromRank, destFile, destRank, player);
        if (currentlyVoting) {
          voteQueue.put(vote);
          StringAndIOUtils.writeAndFlush(sender, ServerRequest.VOTE.getName()+":"+vote.toString());
          
          //send vote signal to player's team (so they can request their current tally)
          if (teamOne.contains(sender)) {
            sendSignallTeam1(ServerResponses.VOTE_RECIEVED);
          }
          else {
            sendSignallTeam2(ServerResponses.VOTE_RECIEVED);
          }
        }
        else {
          StringAndIOUtils.writeAndFlush(sender, ServerRequest.VOTE.createErrorString(ServerResponses.NO_VOTE));
        }
      }
      else {
        StringAndIOUtils.writeAndFlush(sender, ServerRequest.VOTE.createErrorString(ServerResponses.WRONG_ARGS));
      }
    }
    else if (first.equals(ServerRequest.PLIST.getReqName())) {
      if (arguments.size() == ServerRequest.PLIST.argAmount()) {
        boolean includeUUID = Boolean.parseBoolean(arguments.get(0).toLowerCase());
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
        StringAndIOUtils.writeAndFlush(sender, ServerRequest.PLIST.getName()+":"+mess);
      }
      else {
        StringAndIOUtils.writeAndFlush(sender, ServerRequest.PLIST.createErrorString(ServerResponses.WRONG_ARGS));
      }
    }
    else if (first.equals(ServerRequest.UPDATE.getReqName())) {
      System.out.println("  ---UPDATING TEAMS!!! ");
      StringAndIOUtils.writeAndFlush(sender, ServerRequest.UPDATE.getName()+":"+board.parsableToString());
      System.out.println(" ---SENT BOARD");
    }
    else if (first.equals(ServerRequest.DISC.getReqName())) {
      System.out.println(" ---->>>> "+player.getName()+" (T-ID: "+(teamOne.contains(sender) ? 1 : 2)+") has DISCONNECTED!");
      
      if (teamOne.contains(sender)) {
        //remove sender from team one
        teamOne.remove(sender);
      }
      else {
        //remove sender from team two
        teamTwo.remove(sender);
      }
      
      sendSignalAll(ServerResponses.PLAYER_LEFT);
      server.getDatabase().removePlayer(player.getID());
      sender.close();
    }
    else if (first.equals(ServerRequest.QUIT.getReqName())) {
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
      
      //server.getDatabase().removePlayer(player.getID());
      sender.pipeline().addLast("handler", new StagingHandler(server));    
      StringAndIOUtils.writeAndFlush(sender, ServerRequest.QUIT.getName());
      sender.pipeline().remove(this);
      //sender.close();
    }
    else if (first.equals(ServerRequest.SES.getReqName())) {
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
        StringAndIOUtils.writeAndFlush(sender, ServerRequest.SES.createErrorString(ServerResponses.WRONG_ARGS));
      }
    }
    else if (first.equals(ServerRequest.ALL.getReqName())) {
      if (arguments.size() == ServerRequest.ALL.argAmount()) {
        System.out.println("---> PRISON DIL??? "+rules.getProperty(Properties.PRISON_DILEMMA));
        if ( (boolean) rules.getProperty(Properties.PRISON_DILEMMA) == true) {
          StringAndIOUtils.writeAndFlush(sender, ServerRequest.ALL.createErrorString(ServerResponses.PRISON_DIL));
        }
        else {
          String message = arguments.stream().collect(Collectors.joining());
          msgEveryone(String.format(ServerResponses.ALL_MSG, player.getName(), message));       
        }
      }
      else {
        StringAndIOUtils.writeAndFlush(sender, ServerRequest.ALL.createErrorString(ServerResponses.WRONG_ARGS));
      }
    }
    else if (first.equals(ServerRequest.TEAM.getReqName())) {
      if (arguments.size() == ServerRequest.TEAM.argAmount()) {
        System.out.println("---> PRISON DIL??? "+rules.getProperty(Properties.PRISON_DILEMMA));
        if ( (boolean) rules.getProperty(Properties.PRISON_DILEMMA) == true) {
          StringAndIOUtils.writeAndFlush(sender, ServerRequest.TEAM.createErrorString(ServerResponses.PRISON_DIL));
        }
        else {
          String message = arguments.stream().collect(Collectors.joining());
          
          if (teamOne.contains(sender)) {
            msgTeamOne(String.format(ServerResponses.TEAM_MSG, player.getName(), message));
          }
          else {
            msgTeamTwo(String.format(ServerResponses.TEAM_MSG, player.getName(), message));
          }        
        }
      }
      else {
        StringAndIOUtils.writeAndFlush(sender, ServerRequest.TEAM.createErrorString(ServerResponses.WRONG_ARGS));
      }
    }
    else {
      StringAndIOUtils.writeAndFlush(sender, String.format(ServerResponses.BAD_REQUEST, first, ServerResponses.UNKNOWN));
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
