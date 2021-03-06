package jg.proj.chess.net.server;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.PriorityQueue;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import io.netty.channel.Channel;
import io.netty.channel.ChannelHandler.Sharable;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.util.AttributeKey;
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
import jg.proj.chess.net.SessionStatus;
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
  private final Set<Player> teamOne;
  private final Set<Player> teamTwo;
  
  /**
   * List to add votes to. Cleared at every vote end
   */
  private final Map<Player, Vote> votes;
  
  /**
   * The rules of this session
   */
  private final SessionRules rules;
  
  /**
   * Current status of this session.
   */
  private volatile SessionStatus status;
  
  /**
   * Round count
   */
  private volatile int currentRound;  
  
  /**
   * Whether it's currently team one's turn
   */
  private volatile boolean teamOneTurn;
 
  /**
   * Constructs a Session 
   * @param server - the GameServer running this Session
   * @param rules - the SessionRules governing this Session
   */
  public Session(GameServer server, SessionRules rules){
    this.server = server;
    this.rules = rules;
    this.sessionID = UUID.randomUUID();
    
    votes = new ConcurrentHashMap<>();
    
    board = new Board(DEFAULT_BOARD_SIZE, DEFAULT_BOARD_SIZE);
    teams = board.initialize(new DefaultBoardPreparer());
    currentRound = 1;
    
    teamOne = Collections.newSetFromMap(new ConcurrentHashMap<Player, Boolean>());
    teamTwo = Collections.newSetFromMap(new ConcurrentHashMap<Player, Boolean>());
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
  
  /**
   * Sends a message to all players in Team 1
   * @param message - the message to send
   */
  private void msgTeamOne(String message){    
    //message team 1 
    for(Player player : teamOne) {
      System.out.println("   SENDING MSG: "+message+"  || TO: "+player.getName());
      StringAndIOUtils.writeAndFlush(player.getChannel(), message);
    }
  }
  
  /**
   * Sends a message to all players in Team 2
   * @param message - the message to send
   */
  private void msgTeamTwo(String message){    
    //message team 2 
    for(Player player : teamTwo) {
      System.out.println("   SENDING MSG: "+message+"  || TO: "+player.getName());
      StringAndIOUtils.writeAndFlush(player.getChannel(), message);
    }
  }
  
  /**
   * Sends a message to all players in the session
   * @param message - the message to send
   */
  private void msgEveryone(String message){    
    //message team 1 first
    msgTeamOne(message);
    
    //message team 2 first
    msgTeamTwo(message);
  }
  
  /**
   * Sends a signal to all players in the session
   * @param signal - the signal to send
   */
  private void sendSignalAll(int signal) {
   sendSignallTeam1(signal);
   sendSignallTeam2(signal);
  }
  
  /**
   * Sends a signal to all players in Team 1
   * @param signal - the signal to send
   */
  private void sendSignallTeam1(int signal) {
    msgTeamOne(String.format(ServerResponses.SIGNAL_MSG, signal));
  }
  
  /**
   * Sends a signal to all players in Team 2
   * @param signal - the signal to send
   */
  private void sendSignallTeam2(int signal) {
    msgTeamTwo(String.format(ServerResponses.SIGNAL_MSG, signal));
  }
  
  @Override
  public void run() {
    if (status != SessionStatus.ENDED) {
      status = SessionStatus.RUNNING;

      //Wait until both teams reach the minimum size
      final int minTeamSize = (int) rules.getProperty(Properties.MIN_TEAM_COUNT);
      if (teamOne.size() < minTeamSize || teamTwo.size() < minTeamSize) {
        msgEveryone(String.format(ServerResponses.SERVER_MSG, "WAITING FOR MORE PLAYERS!"));
        status = SessionStatus.ACCEPTING;
        while (teamOne.size() < minTeamSize || teamTwo.size() < minTeamSize) {
          if (teamOne.size() == 0 && teamTwo.size() == 0) {
            //session has been abandoned.
            status = SessionStatus.ENDED;
            break;
          }
        }
      }
      
      //change Session status to Playing
      status = SessionStatus.PLAYING;
      //alert all players that the game has started
      System.out.println("---SIGNALLING GAME START");
      sendSignalAll(ServerResponses.GAME_START);
      System.out.println("---SIGNALLED GAME START");
      
      //play the game
      boolean hasWon = false;

      teamOneTurn = true;
      while (!hasWon && status != SessionStatus.ENDED) {
        
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
          //retrieve the amount of seconds set for voting
          final long votingSeconds = (long) rules.getProperty(Properties.VOTING_DURATION);

          //retrieve the current voting team's ID
          final int currentTeamID = teamOneTurn ? 1 : 2;
          
          System.out.println("---CURRENT TURN: "+currentTeamID+" | "+teamOneTurn);
          //signal the voting team that their vote has started
          if (teamOneTurn) {
            sendSignallTeam1(ServerResponses.VOTE_START);
          }
          else {
            sendSignallTeam2(ServerResponses.VOTE_START);
          }
          System.out.println(" ---warned them----");
          
          status = SessionStatus.VOTING;
          
          //start voting window. Send messages to all players
          countDownWindow(votingSeconds, ServerResponses.TIME_MSG, 0);      
          
          //clear out previous votes and signal vote end
          status = SessionStatus.PROCESSING;
          if (teamOneTurn) {
            sendSignallTeam1(ServerResponses.VOTE_END);
          }
          else {
            sendSignallTeam2(ServerResponses.VOTE_END);
          }
          
          msgEveryone(String.format(ServerResponses.SERVER_MSG, "Processing votes!"));
          System.out.println("---PROCESSING VOTES!!! "+votes);

          //decide on move based on plurality
          if (!votes.isEmpty()) {
            //creates a Max heap based on vote count
            PriorityQueue<VoteCounter> voteQueue = new PriorityQueue<VoteCounter>((x,y) -> y.votes - x.votes);
            
            HashMap<Vote, Integer> voteCounter = new HashMap<>();
            for(Vote voteEntry : votes.values()) {
                voteCounter.put(voteEntry, voteCounter.containsKey(voteEntry) ? voteCounter.get(voteEntry) + 1 : 1);             
            }
            
            voteCounter.entrySet().stream().map(x -> new VoteCounter(x.getKey(), x.getValue())).collect(Collectors.toCollection(() -> voteQueue));
            
            votes.clear(); //now clear the votes map
            msgEveryone(String.format(ServerResponses.SERVER_MSG, "Votes processed!"));
            
            VoteCounter mostPopular = voteQueue.poll();
            System.out.println(" MOST POPULAR: "+voteQueue);
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
        
        
        //start break window, if the break duration > 0
        final long breakDuration = (long) rules.getProperty(Properties.BREAK_AMOUNT);
        if (breakDuration > 0) {
          status = SessionStatus.BREAKING;
          sendSignalAll(ServerResponses.BREAK_START);
          countDownWindow(breakDuration, ServerResponses.BREAK_MSG, 0);
          sendSignalAll(ServerResponses.BREAK_END);
        }
        
        //switch turns
        teamOneTurn = teamOneTurn ? false : true;
        currentRound++;
      }

      //clear both teams of members
      teamOne.clear();
      teamTwo.clear();

      
      status = SessionStatus.ENDED;  
    }
    
    //remove session from database
    System.out.println("---REMOVED SESSION: "+sessionID);
    server.getDatabase().removeSession(sessionID);
  }
  
  /**
   * Creates a count down window that blocks the current 
   * thread until the given amount of seconds has passed.
   * 
   * While blocking, messages - according to the given msgFormat - 
   * will be sent to the designated team at every second with the 
   * remaining amount of seconds left in this countdown.
   * 
   * @param seconds - the amount of seconds this count down should be
   * @param msgFormat - the String format to use when sending out remaining seconds
   * @param team - the team to send the messages to (by team ID). 
   *               If the team ID is anything other than 1 or 2, the messages are sent to everyone
   */
  private void countDownWindow(long seconds, String msgFormat, int team) {
    ScheduledExecutorService executor = new ScheduledThreadPoolExecutor(0);
    
    //sleep thread for the amount of time the voting window is
    System.out.println("----STARTING COUNTDOWN WINDOW USING FORMAT: "+msgFormat);
    Runnable timerTask = new Runnable() {
      private long execCount = seconds - 1;       
      @Override
      public void run() {
        if (execCount >= 0) {
          if (team == 1) {
            msgTeamOne(String.format(msgFormat, execCount));
          }
          else if(team == 2){
            msgTeamTwo(String.format(msgFormat, execCount));
          }
          else {
            msgEveryone(String.format(msgFormat, execCount));
          }
          execCount--;
        }
        else {
          executor.shutdown();
        }
      }
    };
    executor.scheduleAtFixedRate(timerTask, 1, 1, TimeUnit.SECONDS);
    try {
      executor.awaitTermination(Long.MAX_VALUE, TimeUnit.SECONDS);
      System.out.println("----ENDING COUNTDOWN WINDOW USING FORMAT: "+msgFormat);
    } catch (InterruptedException e1) {
      e1.printStackTrace();
    }
  }
  
  @Override
  public void handlerAdded(ChannelHandlerContext ctx) throws Exception {
    /*
     * Player has joined the session
     */
    Channel playerChannel = ctx.channel();
    Player player = (Player) playerChannel.attr(AttributeKey.valueOf("player")).get();
    player.setSession(this);    
        
    boolean isTeamOne = (boolean) playerChannel.attr(AttributeKey.valueOf("teamone")).get();
    if (isTeamOne) {
      System.out.println(" USER: "+player.getName()+" added to Team One of session "+sessionID);
      teamOne.add(player);
    }
    else {
      System.out.println(" USER: "+player.getName()+" added to Team Two of session "+sessionID);
      teamTwo.add(player);
    }
    
    //Send a signal to all players that a new player has joined
    sendSignalAll(ServerResponses.PLAYER_JOINED);
    
    if (status == SessionStatus.VOTING) {
      /*
       * If the new player's team is voting, send them the 
       * VOTE_START signal.
       */
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
    Channel playerChannel = ctx.channel();
    Player player = (Player) playerChannel.attr(AttributeKey.valueOf("player")).get();
    player.setSession(null);

    /*
     * Remove the player from their team 
     */
    boolean isTeamOne = (boolean) playerChannel.attr(AttributeKey.valueOf("teamone")).get();
    if (isTeamOne) {
      teamOne.remove(player);
    }
    else {
      teamTwo.remove(player);
    }

    //Send a singla to everyone that a player has left
    sendSignalAll(ServerResponses.PLAYER_LEFT);

    msgEveryone(String.format(ServerResponses.SERVER_MSG, player.getName()+" has left the session!"));
    System.out.println("[SERVER] "+player.getName()+" has left the session!");
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
            
            //remove them from their team
            if (teamOne.contains(player)) {
              //remove sender from team one
              System.out.println(" ---->>>> "+player.getName()+" (TEAM ONE) has LEFT!");
              teamOne.remove(player);
            }
            else {
              //remove sender from team two
              System.out.println("[SERVER] "+player.getName()+" (TEAM TWO) has LEFT!");
              teamTwo.remove(player);
            }
            
            sendSignalAll(ServerResponses.PLAYER_LEFT);
            
            server.getDatabase().removePlayer(player.getID());
            sender.pipeline().remove(this);
            sender.close();
            
            response = null;
            break;
          }
          case QUIT:
          {
            if (teamOne.contains(player)) {
              //remove sender from team one
              System.out.println(" ---->>>> "+player.getName()+" (TEAM ONE) has LEFT!");
              teamOne.remove(player);
            }
            else {
              //remove sender from team two
              System.out.println("[SERVER] "+player.getName()+" (TEAM TWO) has LEFT!");
              teamTwo.remove(player);
            }
            
            sendSignalAll(ServerResponses.PLAYER_LEFT);
            
            sender.pipeline().addLast("handler", new StagingHandler(server));    
            StringAndIOUtils.writeAndFlush(sender, ServerRequest.QUIT.getName());
            sender.pipeline().remove(this);
          
            response = null;
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
              Set<Player> playerTeam = teamOne.contains(player) ? teamOne : teamTwo;
              
              for (Vote vote : votes.values()) {
                if (playerTeam.contains(vote.getVoter())) {
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
            if (status == SessionStatus.VOTING) {
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

                //make sure to only be sorting votes from the current team
                
                Square origin = board.querySquare(vote.getFileOrigin(), vote.getRankOrigin());
                Square destination = board.querySquare(vote.getFileDest(), vote.getRankDest());
                Unit targetUnit = origin.getUnit();
                
                //only consider vote if it's a valid vote, or if the rules allow for no filtering of bad votes              
                if ( (targetUnit != null && targetUnit.getTeamID() == currentVotingTeam && targetUnit.possibleDestinations().contains(destination)) || 
                     (rules.getProperty(Properties.ALLOW_INVL_VOTES).equals(Boolean.TRUE)) ) {             
                  votes.put(vote.getVoter(), vote);
                  
                  response = vote.toString();
                  
                  if (senderTeam == 1) {
                    sendSignallTeam1(ServerResponses.VOTE_RECIEVED);
                  }
                  else {
                    sendSignallTeam2(ServerResponses.VOTE_RECIEVED);
                  }
                }
                else {
                  System.out.println("   NOT A VALID DESTINATION SQUARE");
                  errorCode = ServerResponses.BAD_VOTE;
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
            
            //get team one first, then team two
            String mess = "";
            for (Player teamPlayer : teamOne) {
              mess += teamPlayer.getName()+",true"+(includeUUID ? ","+teamPlayer.getID() : "");
              mess += ":";
            }
            
            for (Player teamPlayer : teamTwo) {
              mess += teamPlayer.getName()+",false"+(includeUUID ? ","+teamPlayer.getID() : "");
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
          case STATUS:
          {
            UUID sessionID = UUID.fromString(arguments[0]);
            Session targetSession = server.getDatabase().findSession(sessionID);
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
        else if (response != null) {
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
    
  public SessionStatus getStatus() {
    return status;
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
