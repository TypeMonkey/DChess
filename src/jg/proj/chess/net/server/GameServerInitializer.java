package jg.proj.chess.net.server;

import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.socket.SocketChannel;
import io.netty.handler.codec.DelimiterBasedFrameDecoder;
import io.netty.handler.codec.Delimiters;
import io.netty.handler.codec.string.StringDecoder;
import io.netty.handler.codec.string.StringEncoder;
import io.netty.util.AttributeKey;

/**
 * Initializer for connecting clients.
 * @author Jose
 *
 */
public class GameServerInitializer extends ChannelInitializer<SocketChannel> {

  private final GameServer server;
  
  private final Database playerStore;
  
  public GameServerInitializer(GameServer server) {
    this.server = server;
    this.playerStore = server.getDatabase();
  }
  
  @Override
  protected void initChannel(SocketChannel ch) throws Exception {
    ChannelPipeline pipeline = ch.pipeline();
    
    pipeline.addLast("framer", new DelimiterBasedFrameDecoder(8192, Delimiters.nulDelimiter()));  
    pipeline.addLast("encoder", new StringEncoder());
    pipeline.addLast("decoder", new StringDecoder());
    
    Player player = new Player(ch);
    
    AttributeKey<Player> playerKey = AttributeKey.valueOf("player");
    ch.attr(playerKey).set(player);
    
    playerStore.addPlayer(player);
    
    System.out.println(" created player for "+ch.remoteAddress());
    pipeline.addLast("handler", new StagingHandler(server));
    System.out.println(" handler create for plater "+ch.remoteAddress());
  }

}
