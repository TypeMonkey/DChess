package jg.proj.chess.net;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.PriorityQueue;
import java.util.Queue;
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
import jg.proj.chess.net.server.Database;
import jg.proj.chess.net.server.GameServer;
import jg.proj.chess.net.server.StagingHandler;

@Sharable
public class Session extends SimpleChannelInboundHandler<String> implements Runnable{
  
  public static final String INVALID_VOTE = "BAD_VOTE\r\n";
  
  private static final Set<String> COMMANDS = Collections.unmodifiableSet(
      new HashSet<>(Arrays.asList(
          "cuser",   //Sets the player's username (Arg: Username (string), Return: None)
          "glist",   //Requests the current active game sessions on the server (Arg: None, Return: Array of Sessions)
          "vote",    //Votes a move. Only applicable if the user is in an active game session (Arg: Unit Square Coords, Destination Square Coord, Return: None)
          "plist",   //Requests the list of all players in the current session (Arg: None, Return: Array of Players in current session, or empty if not in a session)
          "join",    //Joins a game session (Arg: Session ID, Return: Same Session ID)
          "update",  //Gets the current Board of the session (Arg: None, Return: String representation of Board)
          "quit",     //Quits the current game session (Arg: None, Return: Current Session ID or -1 if not in session)
          "exit")));     //Closes the connection with the server (Arg:None, Return:None);
  
  private final GameServer server;
  
  private final UUID sessionID;  
  private final Board board;
  private final TeamInformation teams;
  
  private final ChannelGroup teamOne;
  private final ChannelGroup teamTwo;
  
  private final ArrayBlockingQueue<Vote> voteQueue;
  
  private volatile int currentRound;  
  private volatile boolean running;
  
  public Session(GameServer server){
    this.server = server;
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

      //alert all players that the game has started
      messageEveryone("started");

      //play the game
      boolean hasWon = false;

      boolean teamOneTurn = true;
      while (!hasWon) {
        /*
         * Per turn, we will allow a voting period of 30 seconds
         */

        long timeNow = System.currentTimeMillis();
        long projectedEnd = timeNow + 15000;

        ConcurrentHashMap<Vote, Set<Player>> voteMap = new ConcurrentHashMap<>();

        //voting time window
        ChannelGroup currentTeam = teamOneTurn ? teamOne : teamTwo;       
        int currentTeamID = teamOneTurn ? 1 : 2;
        
        System.out.println("---CURRENT TURN: "+currentTeamID);
        if (teamOneTurn) {
          messageTeamOne(null, "---->VOTE NOW TEAM 1<----");
        }
        else {
          messageTeamTwo(null, "---->VOTE NOW TEAM 2<----");
        }
        
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
              if (square.getUnit() != null && square.getUnit().getTeamID() == currentTeamID && square.getUnit().possibleDestinations().contains(destination)) {
                if (!voteMap.containsKey(vote)) {
                  HashSet<Player> votees = new HashSet<>();
                  votees.add(vote.getVoter());
                  voteMap.put(vote, votees);
                }
                else if(voteMap.get(vote).contains(vote.getVoter())) {
                  System.out.println((vote == null)+" | "+(vote.getVoter() == null)+" | "+(vote.getVoter().getChannel() == null));
                  vote.getVoter().getChannel().write(INVALID_VOTE);
                }
                else {
                  voteMap.get(vote).add(vote.getVoter());
                }

              }
              else {
                System.out.println("   NOT A VALID DESTINATION SQUARE");
                vote.getVoter().getChannel().write(INVALID_VOTE);
              }        
            }
            else {
              System.out.println("   PLAYER: "+vote.getVoter().getName()+" isn't in "+currentTeamID);
              vote.getVoter().getChannel().write(INVALID_VOTE);
            }
          } catch (InterruptedException e) {
            System.out.println("---INTERRUPTED");
          }
        }
        //tell all players that voting has ended
        System.out.println("    > voting done");
        messageEveryone("VOTING HAS ENDED!!!!");

        //decide on move
        if (!voteMap.isEmpty()) {
          PriorityQueue<VoteCounter> voteQueue = new PriorityQueue<VoteCounter>((x,y) -> y.votes - x.votes);
          voteMap.entrySet().stream().map(x -> new VoteCounter(x.getKey(), x.getValue().size())).collect(Collectors.toCollection(() -> voteQueue));
          
          System.out.println("Q: "+voteQueue+" | "+voteMap);

          VoteCounter mostPopular = voteQueue.poll();
          System.out.println(" MOST POPULAR: "+mostPopular.vote.getVoter().getName()+" | "+mostPopular.vote);
          if (mostPopular != null) {
            Square square = board.querySquare(mostPopular.vote.getFileOrigin(), mostPopular.vote.getRankOrigin());
            Square destination = board.querySquare(mostPopular.vote.getFileDest(), mostPopular.vote.getRankDest());

            try {
              square.getUnit().moveTo(destination);
            } catch (InvalidMove e) {
              continue;
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

        //switch turns
        teamOneTurn = teamOneTurn ? false : true;
      }

      running = false;  
    }
  }
  
  private void messageTeamOne(Channel sender, String message){
    //message team 1 first
    for(Channel team1Player : teamOne){
      if (sender != null && team1Player != sender) {
        team1Player.writeAndFlush(message+"\r\n");
      }
    }
  }
  
  private void messageTeamTwo(Channel sender, String message){
    //message team 1 first
    for(Channel team1Player : teamOne){
      if (sender != null && team1Player != sender) {
        IOUtils.writeAndFlush(team1Player, message);
      }
    }
  }
  
  private void messageEveryone(String message){    
    //message team 1 first
    for(Channel team1Player : teamOne){
      IOUtils.writeAndFlush(team1Player, message);
    }
    
    //message team 2 first
    for(Channel team2Player : teamTwo){
      IOUtils.writeAndFlush(team2Player, message);
    }
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
  }
  
  @Override
  public void handlerRemoved(ChannelHandlerContext ctx) throws Exception {
    //tell everyone in the session that someone left
    Channel playerChannel = ctx.channel();
    Player player = (Player) playerChannel.attr(AttributeKey.valueOf("player")).get();
    player.setSession(null);
    
    messageEveryone("[SERVER] "+player.getName()+" has left the session!");
  }
  
  @Override
  protected void channelRead0(ChannelHandlerContext ctx, String msg) throws Exception {
    Channel sender = ctx.channel();
    Player player = (Player) sender.attr(AttributeKey.valueOf("player")).get();
    System.out.println("FROM: "+ctx.channel().remoteAddress()+" | "+msg);
    
    /*
     * Command messages begin with "~"
     * 
     * "cuser",   //Sets the player's username (Arg: Username (string), Return: None)
    "vote",    //Votes a move. Only applicable if the user is in an active game session (Arg: Unit Square Coords, Destination Square Coord, Return: None)
    "plist",   //Requests the list of all players in the current session (Arg: None, Return: Array of Players in current session, or empty if not in a session)
    "update",  //Gets the current Board of the session (Arg: None, Return: String representation of Board)
    "quit",     //Quits the current game session (Arg: None, Return: Current Session ID or -1 if not in session)
     */
    
    if (msg.startsWith("~")) {
      String [] commandBody = msg.substring(1).split(":");
      String commandName = commandBody[0];
      
      System.out.println("   IS COMMAND! "+commandName);
           
      if (commandName.equals("cuser")) {
        if (commandBody.length == 2) {
          player.setName(commandBody[1]);
        }
        else {
          sender.writeAndFlush(StagingHandler.INVALID_ARG_AMNT);
        }
      }
      else if (commandName.equals("vote")) {
        if (commandBody.length == 2) {
          Vote vote = Vote.parseVote(commandBody[1], player);
          if (vote != null) {
            voteQueue.add(vote);
          }
          else {
            sender.writeAndFlush(INVALID_VOTE);
          }
        }
        else {
          sender.writeAndFlush(StagingHandler.INVALID_ARG_AMNT);
        }
      }
      else if (commandName.equals("plist")) {
        String teamOneList = teamOne.stream().map(x -> ((Player) x.attr(AttributeKey.valueOf("player")).get()).getName()+":").collect(Collectors.joining());
        System.out.println("  -> team one: "+teamOneList);
        String teamTwoList = teamTwo.stream().map(x -> ((Player) x.attr(AttributeKey.valueOf("player")).get()).getName()+":").collect(Collectors.joining());
        System.out.println("  -> team two: "+teamTwoList);
        String wholeList = teamOneList+"|"+teamTwoList;
        System.out.println("  -> whole: "+wholeList);
        sender.writeAndFlush(wholeList+"\r\n");
        System.out.println("     -> done!");
      }
      else if (commandName.equals("update")) {
        System.out.println("  ---UPDATING");
        sender.writeAndFlush(board.toString()+"\r\n");
        System.out.println(" ---SENT BOARD");
      }
      else if (commandName.equals("quit")) {
        if (teamOne.contains(sender)) {
          //warn team one
          messageTeamOne(sender, "[SERVER] "+player.getName()+" has LEFT!");
        }
        else {
          //warn team two
          messageTeamTwo(sender, "[SERVER] "+player.getName()+" has LEFT!");
        }
        
        server.getDatabase().removePlayer(player.getID());
        sender.close();
      }
      else {
        sender.writeAndFlush(StagingHandler.INVALID_ARG_AMNT);
      }
    }
    else if (teamOne.contains(sender)) {
      messageTeamOne(sender, msg);
    }
    else if (teamTwo.contains(sender)){
      messageTeamTwo(sender, msg);
    }
    
  }
  
  public boolean isRunning(){
    return running;
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
