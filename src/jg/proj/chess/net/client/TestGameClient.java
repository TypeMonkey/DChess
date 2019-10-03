package jg.proj.chess.net.client;

import java.util.Scanner;

import io.netty.bootstrap.Bootstrap;
import io.netty.channel.Channel;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioSocketChannel;

public class TestGameClient {

  private final int clientID;
    
  public TestGameClient(int clientID){
    this.clientID = clientID;
  }
  
  public void talk() throws InterruptedException{
    EventLoopGroup group = new NioEventLoopGroup();
    
    try {
      Bootstrap bootstrap = new Bootstrap()
          .group(group)
          .channel(NioSocketChannel.class)
          .handler(new GameClientInitializer(this));
      
      Channel channel = bootstrap.connect("localhost", 9999).sync().channel();
      System.out.println("---CONNECTED!!!!");
      
     
      Scanner scanner = new Scanner(System.in);
      
      channel.writeAndFlush("implayer:csess\r\n");      
      channel.writeAndFlush("~update\r\n");
      while(true){      
        channel.writeAndFlush(scanner.nextLine()+"\r\n");
      }   
    } finally {
      group.shutdownGracefully();
    }
  }

  public int getClientID() {
    return clientID;
  }
  
  public static void main(String[] args) throws Exception{
    TestGameClient gameClient = new TestGameClient(0);
    gameClient.talk();
  }
}
