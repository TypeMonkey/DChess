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
import jg.proj.chess.net.client.uis.MainFrame;
import jg.proj.chess.net.client.uis.UsernameDialog;
import jg.proj.chess.net.server.SessionRules;

@Sharable
public class GameClient extends SimpleChannelInboundHandler<String>{

  public static final int PORT = 9999;
  
  private final EventLoopGroup workerGroup;  
  private final Map<ServerRequest, Queue<RequestFuture>> pendingFutures;
  private final MainFrame mainUI;
  
  private Channel channel; 
 
  private volatile String userName;
  private volatile UUID uuid;  
  private volatile boolean isConnected;
  private volatile SessionInfo sessionInfo;
  
  public GameClient(String userName) {
    this.workerGroup = new NioEventLoopGroup();
    this.pendingFutures = new ConcurrentHashMap<>();
    this.userName = userName;
    this.mainUI = new MainFrame(this);
  }
  
  public void initAndConnect(String ipAddress) throws InterruptedException{
    Bootstrap bootstrap = new Bootstrap()
        .group(workerGroup)
        .channel(NioSocketChannel.class)
        .handler(new GameClientInitializer(this));

    channel = bootstrap.connect(ipAddress, PORT).sync().channel();
    isConnected = true;
    System.out.println("*Connected to server at "+ipAddress+":"+PORT);

    //send a cuser request
    Object [] name = {userName};      
    PendingRequest cuserFuture = new PendingRequest(ServerRequest.CUSER, name);
    submitRequest(cuserFuture);
    System.out.println("*Requesting username change to '"+userName+"'. Waiting for response.....");     

    while(userName == null);
    System.out.println("changed! "+userName+" | "+uuid);
  }
  
  public void appearUI() {
    mainUI.updateDislay("<html>Connect to a session by typing '~join:SESSION_ID' . <br>Replace SESSION_ID with the UUID of the session you want to join</html>");
    
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
    pendingFutures.clear();
    workerGroup.shutdownGracefully();
    isConnected = false;
  }
  
  /**
   * Submits a request to be fulfilled by a connected DChess server
   * @param request - the PendingRequest to make
   * @return A Future, or null if this client is not yet connected to a DChess server
   */
  public Future<Object []> submitRequest(PendingRequest request){
    if (isConnected) {
      RequestFuture future = new RequestFuture(request);
      if (pendingFutures.containsKey(request.getRequest())) {
        pendingFutures.get(request.getRequest()).add(future);
      }
      else {
        ConcurrentLinkedQueue<RequestFuture> futures = new ConcurrentLinkedQueue<>();
        futures.add(future);
        pendingFutures.put(request.getRequest(), futures);
      }
      
      String toSend = request.getRequest().addArguments(request.getArguments());
      StringAndIOUtils.writeAndFlush(channel, toSend);
      
      return future;
    }
    return null;
  }
  
  @Override
  protected void channelRead0(ChannelHandlerContext ctx, String msg) throws Exception {
    System.out.println("!!!!!!FROM SERVER: "+msg);
    
    ArrayList<String> arguments = new ArrayList<String>(Arrays.asList(msg.split(":")));
    String reqName = arguments.remove(0);
    ServerRequest matching = ServerRequest.valueOf(reqName.toUpperCase());
    if (matching != null && pendingFutures.containsKey(matching) && !pendingFutures.get(matching).isEmpty()) {
      RequestFuture pendingRequest = pendingFutures.get(matching).poll();
      
      String potentialError = arguments.remove(0);
      if (potentialError.equals("ERROR")) {
        System.out.println("* GOT ERROR: "+arguments.stream().collect(Collectors.joining()));
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
          String originalRequest = pendingRequest.getOriginalRequest().getArguments()[0].toString();
          if (echoName.equals(originalRequest)) {
            uuid = UUID.fromString(arguments.remove(0));
            userName = echoName;
            System.out.println(" **** GOT NAME: "+echoName+" "+uuid);
          }
          else {
            System.err.println("* CRITICAL ERROR: Requested name '"+originalRequest+"' isn't the same as echo '"+echoName+"'");
          }
        }
        else if(matching == ServerRequest.VOTE){
          System.out.println("*Vote '"+arguments.remove(0)+" has been casted!");
        }
        else if(matching == ServerRequest.PLIST){
          //print the list
          for (String string : arguments) {
            String [] split = string.split(",");
            System.out.println(Arrays.toString(split));
          }
        }
        else if(matching == ServerRequest.QUIT){
          //A DChess server doesn't respond back to a QUIT request
          System.out.println("****DISCONNECTING****");
          disconnect();
          System.out.println("****DONE****");
        }
      }      
    }
    else {
      System.out.println(msg);
    }  
  }
  
  public void parseInput(String input) {
    
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

  public static void main(String [] args) throws Exception{
    UsernameDialog dialog = new UsernameDialog();
    
    SwingUtilities.invokeLater(new Runnable() {
      public void run() {
        dialog.setVisible(true);
      }
    });
    
    String userName = dialog.blockUntilUsername();
    if (userName != null) {
      System.out.println("----user disposed");
      
      GameClient gameClient = new GameClient(userName);
      gameClient.initAndConnect("localhost");
      gameClient.appearUI();
      
      System.out.println("----main frame up");
    }
    
    return;

    
    /*
    GameClient gameClient = new GameClient("sample");
    System.out.println(" ----> DChess Client v1.0 <---- ");
    gameClient.initAndConnect("localhost");
    
    PendingRequest pendingRequest = new PendingRequest(ServerRequest.CSESS, 
        2, 
        false,
        15000,
        1,
        false,
        true);
    gameClient.submitRequest(pendingRequest);
    */
  }
}
