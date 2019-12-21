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
import jg.proj.chess.net.ServerResponses;
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
  
  private RequestHandler requestHandler;  
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
      
      requestHandler = new RequestHandler(this, mainUI, channel);

      //send a cuser request
      PendingRequest pendingRequest = new PendingRequest(ServerRequest.CUSER, userName);     
      RequestFuture cuserFuture = new RequestFuture(pendingRequest, Reactor.BLANK_REACTOR);
      
      submitRequest(cuserFuture);
      System.out.println("*Requesting username change to '"+userName+"'. Waiting for response.....");     

      while(uuid == null);
      System.out.println("changed! "+userName+" | "+uuid);
      
      //change handler
      channel.pipeline().remove(this);
      channel.pipeline().addLast("handler",requestHandler);
            
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
  
  public void setName(String newName) {
    userName = newName;
  }
  
  public void setUUID(UUID newUUID) {
    uuid = newUUID;
  }
  
  public void setSession(SessionInfo newSessionInfo) {
    sessionInfo = newSessionInfo;
  }
  
  public void appearUI() {    
    //mainUI.updateDislay("Click 'Connect' to join or create a session.");
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
  public void submitRequest(RequestFuture request){
    requestHandler.submitRequest(request);
  }
  
  @Override
  protected void channelRead0(ChannelHandlerContext ctx, String msg){
    ArrayList<String> arguments = new ArrayList<String>(Arrays.asList(msg.split(":")));
    String reqName = arguments.remove(0);
    
    if (reqName.equalsIgnoreCase(ServerRequest.CUSER.getName())) {
      userName = arguments.remove(0);
      uuid = UUID.fromString(arguments.remove(0));
      System.out.println(" **** GOT NAME: "+userName+" "+uuid);
    }
    else {
      System.err.println("UNKNOWN INITIAL SERVER RESPONSE!!!: "+msg);
    }
  }
  
  public void parseInput(String input, Reactor reactor){
    if (input.startsWith("~")) {
      //then this is a command
      String wholeCommand = input.substring(1, input.length());
      String [] segments = wholeCommand.split(":");
      String commandName = segments[0];

      System.out.println("----> PARSED: |"+commandName+"| "+Arrays.toString(segments)+" | command length "+commandName.length());


      ServerRequest actualRequest = ServerRequest.valueOf(commandName.toUpperCase().trim());

      if (actualRequest == ServerRequest.ALL || actualRequest == ServerRequest.TEAM) {
        PendingRequest request = new PendingRequest(actualRequest, 
            Arrays.stream(segments, 1, segments.length).collect(Collectors.joining()));

        RequestFuture future = new RequestFuture(request, reactor);
        submitRequest(future);
      }
      else if (actualRequest == ServerRequest.VOTE) {
        if (segments.length - 1 == ServerRequest.VOTE.argAmount()) {
          /*
           * OOOHH, user is attentive and may have entered the "vote" command in it's full form, 
           * as in "~vote:%c:%d:%c:%d"
           */
          System.out.println("--> USER ENTERED POTENTIAL FULL FORM!");
          
          String [] voteComponents = Arrays.copyOfRange(segments, 1, segments.length);
          PendingRequest pendingRequest = new PendingRequest(ServerRequest.VOTE, voteComponents);

          RequestFuture future = new RequestFuture(pendingRequest, reactor);
          submitRequest(future);
        }
        else if (segments.length - 1 == 1) {
          //user entered shortform. Must parse ):
          System.out.println("--> USER ENTERED POTENTIAL short FORM!");
          
          final String VOTE_REGEX = "[a-zA-Z][0-9]>[a-zA-Z][0-9]";

          String actualVote = segments[1];
          if (actualVote.matches(VOTE_REGEX)) {
            System.out.println("---IT MATCHES, YAY!!!");
            char [] chars = actualVote.toCharArray();
            
            //guarantees: actualVote is of length 5. char 0 -> char , char 1 -> int , char 2 -> '>', char 3 -> char , char 4 -> int
            char fromChar = chars[0];
            int fromInt = Integer.parseInt(String.valueOf(chars[1]));
            char destChar = chars[3];
            int destInt = Integer.parseInt(String.valueOf(chars[4]));
            
            Object [] voteComponents = {fromChar, fromInt, destChar, destInt};
            PendingRequest pendingRequest = new PendingRequest(actualRequest, voteComponents);
            
            RequestFuture requestFuture = new RequestFuture(pendingRequest, mainUI);
            submitRequest(requestFuture);
          }
          else {
            System.out.println("--- IT doesn't match. Trigerring error!!");
            reactor.error(new PendingRequest(actualRequest, Arrays.copyOfRange(segments, 1, segments.length)), ServerResponses.WRONG_ARGS);
          }
        }
      }
      else if (segments.length - 1 == actualRequest.argAmount()) {
        PendingRequest request = new PendingRequest(actualRequest, 
            Arrays.copyOfRange(segments, 1, segments.length));

        RequestFuture future = new RequestFuture(request, reactor);
        submitRequest(future);
      }
    }
    else {
      //default to team chat
      PendingRequest request = new PendingRequest(ServerRequest.TEAM, input);
      RequestFuture future = new RequestFuture(request, reactor);
      submitRequest(future);
    }
  }
  
  public SessionInfo getCurrentSessionInfo() {
    return sessionInfo;
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
      UUID recievedUUID = gameClient.initAndConnect("35.166.15.181");
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
