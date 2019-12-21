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
import jg.proj.chess.net.ServerResponses;
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
           
      if (toSend == null) {
        //Trigger error now
        request.reactor.error(original, ServerResponses.WRONG_ARGS);
      }
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
  
  private void processSignal(int signalCode) {
    if (signalCode == ServerResponses.VOTE_START) {
      mainUI.updateWarningLine("<font color=\"green\">YOUR TEAM IS VOTING!!!</font> ");
      submitRequest(new RequestFuture(new PendingRequest(ServerRequest.UPDATE), mainUI));
    }
    else if (signalCode == ServerResponses.VOTE_END) {
      mainUI.updateWarningLine("<font color=\"red\">STOP VOTING!!!</font> ");
      submitRequest(new RequestFuture(new PendingRequest(ServerRequest.UPDATE), mainUI));
    }
    else if (signalCode == ServerResponses.TEAM1_WON) {
      mainUI.updateMessages("TEAM 1 HAS WON!!!", true, "SERVER");
    }
    else if (signalCode == ServerResponses.TEAM2_WON) {
      mainUI.updateMessages("TEAM 2 HAS WON!!!", true, "SERVER");
    }
    else if (signalCode == ServerResponses.TEAM1_DESS) {
      mainUI.updateMessages("TEAM 1 has deserted the battle. TEAM 2 WINS!!!", true, "SERVER");
    }
    else if (signalCode == ServerResponses.TEAM2_DESS) {
      mainUI.updateMessages("TEAM 2 has deserted the battle. TEAM 1 WINS!!!", true, "SERVER");
    }
    else if (signalCode == ServerResponses.TEAM1_TIED) {
      mainUI.updateMessages("TEAM 1 is tied on a vote and thus has made no moves....", true, "SERVER");
    }
    else if (signalCode == ServerResponses.TEAM2_TIED) {
      mainUI.updateMessages("TEAM 2 is tied on a vote and thus has made no moves....", true, "SERVER");
    }
    else if (signalCode == ServerResponses.TEAM1_NO_UNIT) {
      mainUI.updateMessages("TEAM 1 has decided to move a unit .. that doesn't exist. Idiots...", true, "SERVER");
    }
    else if (signalCode == ServerResponses.TEAM2_NO_UNIT) {
      mainUI.updateMessages("TEAM 2 has decided to move a unit .. that doesn't exist. Idiots...", true, "SERVER");
    }
    else if (signalCode == ServerResponses.TEAM1_OTHER_UNIT) {
      mainUI.updateMessages("TEAM 1 has decided to move a unit that doesn't belong to them! Idiots....", true, "SERVER");
    }
    else if (signalCode == ServerResponses.TEAM2_OTHER_UNIT) {
      mainUI.updateMessages("TEAM 2 has decided to move a unit that doesn't belong to them! Idiots....", true, "SERVER");
    }
    else if (signalCode == ServerResponses.TEAM1_IDIOT_VOTE) {
      mainUI.updateMessages("TEAM 1 has decided to do an illegal move! No move from them!", true, "SERVER");
    }
    else if (signalCode == ServerResponses.TEAM2_IDIOT_VOTE) {
      mainUI.updateMessages("TEAM 2 has decided to do an illegal move! No move from them!", true, "SERVER");
    }
    else if (signalCode == ServerResponses.TEAM1_NO_VOTE) {
      mainUI.updateMessages("No one from TEAM 1 voted! So, no move has been made. Idiots....", true, "SERVER");
    }
    else if (signalCode == ServerResponses.TEAM2_NO_VOTE) {
      mainUI.updateMessages("No one from TEAM 2 voted! So, no move has been made. Idiots....", true, "SERVER");
    }
    else if (signalCode == ServerResponses.PLAYER_JOINED ||
             signalCode == ServerResponses.PLAYER_LEFT) {
      submitRequest(new RequestFuture(new PendingRequest(ServerRequest.PLIST, true), mainUI));
    }
    else if (signalCode == ServerResponses.TURN_END) {
      submitRequest(new RequestFuture(new PendingRequest(ServerRequest.UPDATE), mainUI));
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
      processSignal(signalCode);
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
