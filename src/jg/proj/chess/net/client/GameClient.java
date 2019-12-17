package jg.proj.chess.net.client;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Map;
import java.util.Queue;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.Future;
import java.util.stream.Collectors;

import javax.swing.SwingUtilities;

import io.netty.bootstrap.Bootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.channel.ChannelHandler.Sharable;
import jg.proj.chess.net.ServerRequest;
import jg.proj.chess.net.StringAndIOUtils;
import jg.proj.chess.net.client.uis.IntroScreen;
import jg.proj.chess.net.client.uis.MainFrame;
import jg.proj.chess.net.client.uis.UsernameDialog;
import jg.proj.chess.net.server.SessionRules;

@Sharable
public class GameClient extends SimpleChannelInboundHandler<String>{

  public static final int PORT = 9999;
  
  private final EventLoopGroup workerGroup;  
  private final MainFrame mainUI;
  
  private Channel channel; 
 
  private volatile String userName;
  private volatile UUID uuid;  
  private volatile boolean isConnected;
  private volatile SessionInfo sessionInfo;
  
  public GameClient(String userName) {
    this.workerGroup = new NioEventLoopGroup();
    this.userName = userName;
    this.mainUI = new MainFrame(this);
  }
  
  public UUID initAndConnect(String ipAddress){
    Bootstrap bootstrap = new Bootstrap()
        .group(workerGroup)
        .channel(NioSocketChannel.class)
        .handler(new GameClientInitializer(this));

    try {
      channel = bootstrap.connect(ipAddress, PORT).sync().channel();
      
      isConnected = true;
      System.out.println("*Connected to server at "+ipAddress+":"+PORT);

      //send a cuser request
      Object [] name = {userName};      
      PendingRequest cuserFuture = new PendingRequest(ServerRequest.CUSER, name);
      submitRequest(cuserFuture);
      System.out.println("*Requesting username change to '"+userName+"'. Waiting for response.....");     

      while(uuid == null);
      System.out.println("changed! "+userName+" | "+uuid);
      
      return uuid;
    } catch (Exception e) {
      System.out.println("** ENCOUNTERED ERROR AT SERVER CONNECTION!!");
      e.printStackTrace();
      
      System.out.println("** RESETTING!!!");
      channel = null;
      isConnected = false;
      
      return uuid;
    } 
  }
  
  public void appearUI() {    
    mainUI.updateDislay("Click 'Connect' to join or create a session.");
    SwingUtilities.invokeLater(new Runnable() {
      public void run() {
        mainUI.setVisible(true);
      }
    });
  }
  
  public void disconnect() throws InterruptedException{
    if (isConnected) {
      channel.close().sync();
    }
    workerGroup.shutdownGracefully();
    isConnected = false;
  }
  
  /**
   * Submits a request to be fulfilled by a connected DChess server
   * @param request - the PendingRequest to make
   * @return A Future, or null if this client is not yet connected to a DChess server
   */
  public void submitRequest(PendingRequest request){
    if (isConnected) { 
      String toSend = request.getRequest().addArguments(request.getArguments());
      StringAndIOUtils.writeAndFlush(channel, toSend);    
    }
  }
  
  @Override
  protected void channelRead0(ChannelHandlerContext ctx, String msg) throws Exception {
    System.out.println("!!!!!!FROM SERVER: "+msg);
    
    ArrayList<String> arguments = new ArrayList<String>(Arrays.asList(msg.split(":")));
    String reqName = arguments.remove(0);
    if (reqName.startsWith("!")) {
      System.out.println("Command '"+reqName.substring(1)+"' unavailable!");
    }
    else {
      ServerRequest matching = ServerRequest.valueOf(reqName.toUpperCase());

      String potentialError = arguments.remove(0);
      if (potentialError.equals("ERROR")) {
        String errorArgs = arguments.stream().collect(Collectors.joining());
        mainUI.updateMessages("**GOT ERROR FOR: "+errorArgs, false, "SERVER");
      }
      else {
        arguments.add(0, potentialError);

        if (matching == ServerRequest.JOIN || matching == ServerRequest.CSESS) {
          UUID sessionID = UUID.fromString(arguments.remove(0));
          boolean isTeamOne = Boolean.parseBoolean(arguments.remove(0));

          String rulesToParse = arguments.stream().collect(Collectors.joining());
          SessionRules rules = SessionRules.parseFromString(rulesToParse);

          sessionInfo = new SessionInfo(rules, sessionID, isTeamOne);
        }
        else if (matching == ServerRequest.UPDATE) {
          System.out.println(arguments.remove(0));
        }
        else if(matching == ServerRequest.CUSER){
          String echoName = arguments.remove(0);
          uuid = UUID.fromString(arguments.remove(0));
          userName = echoName;
          System.out.println(" **** GOT NAME: "+echoName+" "+uuid);
        }
        else if(matching == ServerRequest.VOTE){
          System.out.println("*Vote '"+arguments.remove(0)+" has been casted!");
        }
        else if(matching == ServerRequest.PLIST){
          //print the list
          ArrayList<String> teamOnePlayers = new ArrayList<String>();
          ArrayList<String> teamTwoPlayers = new ArrayList<String>();
          
          for (String string : arguments) {
            //there should only be at least two infos: name, isTeamOne
            //if we have UUID, then UUID should be the last piece of info
            String [] playerInfo = string.split(",");
            if (playerInfo[1].equals("true")) {
              teamOnePlayers.add(playerInfo[0]);
            }
            else {
              teamTwoPlayers.add(playerInfo[0]);
            }
          }
          
          mainUI.updateTeam1Roster(teamOnePlayers);
          mainUI.updateTeam2Roster(teamTwoPlayers);
        }
        else if(matching == ServerRequest.QUIT){
          //A DChess server doesn't respond back to a QUIT request
          System.out.println("****DISCONNECTING****");
          disconnect();
          System.out.println("****DONE****");
        }
        else if (matching == ServerRequest.ALL) {
          String sender = arguments.remove(0);
          String message = arguments.remove(0);
          boolean toAll = true;
          
          mainUI.updateMessages(message, toAll, sender);
        }
        else if (matching == ServerRequest.TEAM) {
          String sender = arguments.remove(0);
          String message = arguments.remove(0);
          boolean toAll = false;

          mainUI.updateMessages(message, toAll, sender);
        }
      }      
    }
  }
  
  public PendingRequest parseInput(String input){
    if (input.startsWith("~")) {
      //then this is a command
      String wholeCommand = input.substring(1, input.length());
      String [] segments = wholeCommand.split(":");
      String commandName = segments[0];

      try {
        ServerRequest actualRequest = ServerRequest.valueOf(commandName.toUpperCase());

        if (actualRequest == ServerRequest.ALL || actualRequest == ServerRequest.TEAM) {
          PendingRequest request = new PendingRequest(actualRequest, 
              Arrays.stream(segments, 1, segments.length).collect(Collectors.joining()));
          submitRequest(request);

          return request;
        }
        else if (wholeCommand.length() - 1 == actualRequest.argAmount()) {
          PendingRequest request = new PendingRequest(actualRequest, 
              Arrays.copyOfRange(segments, 1, segments.length));
          submitRequest(request);

          return request;
        }
        else {
          return null;
        }

      } catch (IllegalArgumentException e) {
        return null;
      }
    }
    else {
      //default to team chat
      PendingRequest request = new PendingRequest(ServerRequest.TEAM, input);
      submitRequest(request);
      return request;
    }
  }

  public boolean isConnected(){
    return isConnected;
  }
  
  public UUID getID(){
    return uuid;
  }
  
  public boolean inSession() {
    return sessionInfo != null;
  }
  
  public String getUserName() {
    return userName;
  }

  public static void main(String [] args) throws Exception{
    UsernameDialog dialog = new UsernameDialog();
    
    SwingUtilities.invokeLater(new Runnable() {
      public void run() {
        dialog.setVisible(true);
      }
    });
    
    String userName = dialog.blockUntilUsername();
    dialog.dispose();
    if (userName != null) {
      System.out.println("----user disposed");
      
      IntroScreen screen = new IntroScreen("imgs/intro/wigand.jpg");
      screen.setVisible(true);
      
      GameClient gameClient = new GameClient(userName);
      UUID recievedUUID = gameClient.initAndConnect("localhost");
      if (recievedUUID != null) {
        screen.dispose();        
        gameClient.appearUI();      
        System.out.println("----main frame up");
      }
      else {
        System.err.println("------> CONNECTION ERROR <------");
      }
    }
    
  }
}
