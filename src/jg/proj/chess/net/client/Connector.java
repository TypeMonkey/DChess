package jg.proj.chess.net.client;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Deque;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.concurrent.ConcurrentLinkedQueue;

import io.netty.bootstrap.Bootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.codec.DelimiterBasedFrameDecoder;
import io.netty.handler.codec.Delimiters;
import io.netty.handler.codec.string.StringDecoder;
import io.netty.handler.codec.string.StringEncoder;
import javafx.application.Platform;
import jg.proj.chess.net.ArgType;
import jg.proj.chess.net.ServerRequest;
import jg.proj.chess.net.ServerResponses;
import jg.proj.chess.net.client.RequestFuture.Status;
import jg.proj.chess.utils.StringAndIOUtils;

/**
 * Front end for all network communications with the server
 * @author Jose
 *
 */
public class Connector extends SimpleChannelInboundHandler<String>{

  private final ChessClient client;
  private final String ip;
  private final int port;
  private final EventLoopGroup workerGroup;
  
  //Sent server request map
  private final Map<UUID, RequestFuture> reqMap;
  
  //List of all signal listeners
  private final List<SignalListener> signalListeners;
  //List of all message listeners
  private final List<MessageListener> messageListeners;
  
  private Channel channel;
  private boolean isConnected;
  
  public Connector(ChessClient client, EventLoopGroup workerPool, String ip, int port) {
    this.client = client;
    this.ip = ip;
    this.port = port;
    this.workerGroup = workerPool;
    reqMap = new ConcurrentHashMap<>();
    signalListeners = new ArrayList<>();
    messageListeners = new ArrayList<>();
  }
  
  public void connect() throws IOException, InterruptedException{
    Bootstrap bootstrap = new Bootstrap()
        .group(workerGroup)
        .channel(NioSocketChannel.class)
        .handler(this);
    
    channel = bootstrap.connect(ip, port).sync().channel();
    
    ChannelPipeline pipeline = channel.pipeline();
    pipeline.addFirst("encoder", new StringEncoder());
    pipeline.addFirst("decoder", new StringDecoder());
    pipeline.addFirst("framer", new DelimiterBasedFrameDecoder(8192, Delimiters.nulDelimiter()));  
    
    isConnected = true;

    System.out.println("*Connected to server at "+ip+":"+port);
  }
  
  

  @Override
  protected void channelRead0(ChannelHandlerContext ctx, String msg) throws Exception {
    String [] split = msg.split(":"); 
    
    System.out.println("----FROM SERVER: "+msg);
    
    if (split[0].equals(ServerResponses.SIGNAL)) {
      //alert all signal listeners
      for (SignalListener listener : signalListeners) {
        Platform.runLater(() -> listener.handleSignal(Integer.parseInt(split[1])));
      }
    }
    else if (split[0].equals(ServerResponses.SERV) || 
             split[0].equals(ServerResponses.RESULT) ||
             split[0].equals(ServerResponses.ALL) ||
             split[0].equals(ServerResponses.TEAM)) {
      //get subarray from 1 <-> split.length-1
      String [] mess = Arrays.copyOfRange(split, 1, split.length);
      for (MessageListener listener : messageListeners) {
        Platform.runLater(() -> listener.handleMessage(split[0], mess));
      }
    }   
    else {
      UUID identifier = UUID.fromString(split[0]);
      String req = split[1];
      String [] arguments = split.length <= 2 ? new String[0] : Arrays.copyOfRange(split, 2, split.length);

      //error responses are formatted as such: SERVER_REQ:ERROR:ERROR_CODE
      boolean gotError = arguments[0].equals("ERROR");
      
      //now activate appropriate reactors
      try {
        RequestFuture future = reqMap.remove(identifier);
        if (future != null) {
          if (gotError) {
            future.changeStatus(Status.ERROR);
            Platform.runLater(() -> future.error(Integer.parseInt(split[2])));
          }
          else {
            future.changeStatus(Status.COMPLETE);
            Platform.runLater(() -> future.react(Arrays.copyOfRange(split, 1, split.length)));
          }
        }
        else {
          client.recordException("---UNKNOWN REQ: "+identifier+" | req map: "+reqMap);
        }
      } catch (IllegalArgumentException e) {
        //should not happen! But just report as a sanity check
        client.recordException(e);
      }

    }
  }
  
  public synchronized void sendRequest(PendingRequest request, Reactor reactor) {
    reqMap.put(request.getIdentifier(), new RequestFuture(request, reactor));
    
    //send out request
    System.out.println("----CONNECTOR: SENDING "+request.toString()+" | "+Arrays.toString(request.getArguments())+" | "+Arrays.toString(ArgType.getStringRep(request.getArguments())));
    StringAndIOUtils.writeAndFlush(channel, request.toString());
  }
  
  public void addSignalListener(SignalListener listener) {
    signalListeners.add(listener);
  }
  
  public void addMessageListener(MessageListener listener) {
    messageListeners.add(listener);
  }
  
  public void shutdown() throws InterruptedException {
    if (isConnected) {
      channel.close().sync();
    }
    signalListeners.clear();
    messageListeners.clear();
    isConnected = false;
  }
  
  public boolean isConnected() {
    return isConnected;
  }
}
