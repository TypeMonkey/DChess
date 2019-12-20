package jg.proj.chess.net.client;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.stream.Collectors;

import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import jg.proj.chess.net.ServerRequest;
import jg.proj.chess.net.StringAndIOUtils;
import jg.proj.chess.net.client.RequestFuture.Status;
import jg.proj.chess.net.client.uis.MainFrame;
import jg.proj.chess.net.server.SessionRules;

public class RequestHandler extends SimpleChannelInboundHandler<String>{
  
  private final GameClient gameClient;
  private final MainFrame mainUI;
  private final Channel channel;  
  private final Map<ServerRequest, ConcurrentLinkedQueue<RequestFuture>> reqMap;
  
  public RequestHandler(GameClient gameClient, MainFrame mainUI, Channel channel) {
    this.gameClient = gameClient;
    this.mainUI = mainUI;
    this.channel = channel;
    
    reqMap = new ConcurrentHashMap<>();
  }
  
  /**
   * Submits a request to be fulfilled by a connected DChess server
   * @param request - the PendingRequest to make
   * @return A Future, or null if this client is not yet connected to a DChess server
   */
  public void submitRequest(RequestFuture request){
    PendingRequest original = request.getOriginalRequest();
    if (gameClient.isConnected()) { 
      String toSend = original.getRequest().addArguments(original.getArguments());
           
      if (reqMap.containsKey(original.getRequest())) {
        reqMap.get(original.getRequest()).add(request);
      }
      else {
        ConcurrentLinkedQueue<RequestFuture> queue = new ConcurrentLinkedQueue<>();
        queue.add(request);
        reqMap.put(original.getRequest(), queue);
      }
      
      StringAndIOUtils.writeAndFlush(channel, toSend);    
    }
  }

  @Override
  protected void channelRead0(ChannelHandlerContext ctx, String msg) throws Exception {
    System.out.println("!!!!!!FROM SERVER: "+msg);
    
    ArrayList<String> arguments = new ArrayList<String>(Arrays.asList(msg.split(":")));
    String reqName = arguments.remove(0);
    if (reqName.startsWith("!")) {
      //This should never happen as we're only sending VALID requests
      //if this response is sent, there is a major error.....
      System.out.println("Command '"+reqName.substring(1)+"' unavailable!");
    }
    else if (reqName.equals("signal")) {
      int signalCode = Integer.parseInt(arguments.remove(0));
      //TODO: Check for vote start, vote end, team x won, etc....
    }
    else if (reqName.equals("serv")) {
      //general server messages
      mainUI.updateMessages(arguments.get(0), true, "SERVER");
    }
    else {
      ServerRequest matching = ServerRequest.valueOf(reqName.toUpperCase());
      
      if (!reqMap.containsKey(matching) ||
          (reqMap.containsKey(matching) && reqMap.get(matching).isEmpty())) {
        //these are responses sent by the server voluntarily TEAM and ALL
        String userName = arguments.get(0);
        String message = arguments.get(1);
        mainUI.updateMessages(message, matching == ServerRequest.ALL, userName);
      }
      else {
        RequestFuture future = reqMap.get(matching).poll();
        //check for error
        if (!arguments.isEmpty() && arguments.get(0).equals("ERROR")) {
          int errorCode = Integer.parseInt(arguments.get(1));
          future.changeStatus(Status.ERROR);
          future.error(errorCode);
        }
        else {
          future.react(arguments.toArray(new String[arguments.size()]));
        }
      }     
    }
  
  }

}
