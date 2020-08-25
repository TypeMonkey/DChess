package jg.proj.chess.net.server;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.ServerSocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;

/**
 * Acts as the front-end for the DChess server
 * 
 * The network infrastructure for DChess categorizes three main
 * exchanges between client and server:
 * 
 *  -> Signals - numerical values sent to clients, not necessarily at the request of clients
 *  -> Messages - string messages sent to clients, not necessarily at the request of clients
 *  -> Requests - string requests sent by clients to the server. The server, assuming a healthy connection,
 *                will eventually respond to such requests either with a result or an error
 * 
 * Note: There's only one instance of GameServer
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
  
  private ServerSocketChannel serverSocket;
  
  /**
   * Constructs a GameServer
   */
  private GameServer(){
    playerStore = new Database();

    sessionWorkerPool = Executors.newCachedThreadPool();
    workerThreadPool = new NioEventLoopGroup();  
  }
  
  /**
   * Starts the server, awaiting incoming connections
   * @throws InterruptedException
   */
  public void start() throws InterruptedException{
    try {
      ServerBootstrap serverBootstrap = new ServerBootstrap()
          .group(workerThreadPool)
          .channel(NioServerSocketChannel.class)
          .childHandler(new GameServerInitializer(this));
      
      serverBootstrap.option(ChannelOption.SO_REUSEADDR, true);

      
      serverSocket = (ServerSocketChannel) serverBootstrap.bind(PORT).sync().channel();
      serverSocket.closeFuture();
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
  
  /**
   * Retrieves the database for this server
   * @return the database for this server
   */
  public Database getDatabase(){
    return playerStore;
  }
  
  /**
   * Shuts down this server
   */
  public void shutdown(){
    serverSocket.close().syncUninterruptibly();
    workerThreadPool.shutdownGracefully();
  }
  
  /**
   * Retrieves the static instance of this GameServer
   * @return the static instance of this GameServer
   */
  public static GameServer getGameServer(){
    return GAME_SERVER;
  }
  
  public static void main(String [] args) throws Exception{  
    System.out.println("---DEMOCRATIC CHESS SERVER V1.0---");
    GAME_SERVER.start();
  }
}
