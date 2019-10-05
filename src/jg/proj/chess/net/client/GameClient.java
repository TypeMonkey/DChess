package jg.proj.chess.net.client;

import java.util.Scanner;

import io.netty.bootstrap.Bootstrap;
import io.netty.channel.Channel;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioSocketChannel;
import jg.proj.chess.net.ServerRequests;
import jg.proj.chess.net.StringAndIOUtils;

public class GameClient {

  public static final int PORT = 9999;
  
  private final EventLoopGroup workerGroup;
  
  private Channel channel; 
  
  public GameClient() {
    this.workerGroup = new NioEventLoopGroup();
  }
  
  public void initAndConnect(String ipAddress) throws InterruptedException{
    try {
      Bootstrap bootstrap = new Bootstrap()
          .group(workerGroup)
          .channel(NioSocketChannel.class)
          .handler(new GameClientInitializer(this));
      
      channel = bootstrap.connect(ipAddress, 9999).sync().channel();
      System.out.println("---CONNECTED!!!!");
      
     
      Scanner scanner = new Scanner(System.in);
      
      StringAndIOUtils.writeAndFlush(channel, ServerRequests.CUSER.addArguments("implayer"));
      StringAndIOUtils.writeAndFlush(channel, ServerRequests.CSESS.addArguments(1, 15000));
      StringAndIOUtils.writeAndFlush(channel, ServerRequests.UPDATE.getFormatString());
      while(true){
        StringAndIOUtils.writeAndFlush(channel, scanner.nextLine());
      }   
    } finally {
      workerGroup.shutdownGracefully();
    }
  }
  
  public void disconnect() throws InterruptedException{
    if (channel != null) {
      channel.close().await();
    }
    workerGroup.shutdownGracefully();
  }
  
  public static void main(String [] args) throws Exception{
    GameClient gameClient = new GameClient();
    gameClient.initAndConnect("35.166.15.181");
  }
}
