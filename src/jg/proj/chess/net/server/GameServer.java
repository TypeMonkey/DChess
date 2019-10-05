package jg.proj.chess.net.server;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import jg.proj.chess.net.Session;

/**
 * An implementation for the DChess server
 * 
 * By default, DChess servers are hosted on the port 9999
 * 
 * @author Jose
 *
 */
public class GameServer {
  
  private static final int PORT = 9999;
  private static final GameServer GAME_SERVER = new GameServer();
 
  private final Database playerStore;
  
  private final EventLoopGroup workerThreadPool;
  private final ExecutorService sessionWorkerPool;
  
  private GameServer(){
    playerStore = new Database();

    sessionWorkerPool = Executors.newCachedThreadPool();
    workerThreadPool = new NioEventLoopGroup();  
  }
  
  public void start() throws InterruptedException{
    try {
      ServerBootstrap serverBootstrap = new ServerBootstrap()
          .group(workerThreadPool)
          .channel(NioServerSocketChannel.class)
          .childHandler(new GameServerInitializer(this));
      
      serverBootstrap.option(ChannelOption.SO_REUSEADDR, true);
      
      serverBootstrap.bind(PORT).sync().channel().closeFuture().sync();
    } 
    finally {
       workerThreadPool.shutdownGracefully();
    }
  }
  
  /**
   * Runs the given Session
   * @param session - the Session to start
   */
  protected void runSession(Session session){
    sessionWorkerPool.execute(session);
  }
  
  public Database getDatabase(){
    return playerStore;
  }
  
  public void shutdown(){
    workerThreadPool.shutdownGracefully();
  }
  
  public static GameServer getGameServer(){
    return GAME_SERVER;
  }
  
  public static void main(String [] args) throws Exception{  
    System.out.println("---DEMOCRATIC CHESS SERVER V1.0---");
    GameServer gameServer = new GameServer();   
    gameServer.start();
  }
}
