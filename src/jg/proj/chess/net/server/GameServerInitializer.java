package jg.proj.chess.net.server;

import java.util.List;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.socket.SocketChannel;
import io.netty.handler.codec.ByteToMessageDecoder;
import io.netty.handler.codec.DelimiterBasedFrameDecoder;
import io.netty.handler.codec.Delimiters;
import io.netty.handler.codec.LengthFieldBasedFrameDecoder;
import io.netty.handler.codec.LengthFieldPrepender;
import io.netty.handler.codec.LineBasedFrameDecoder;
import io.netty.handler.codec.string.StringDecoder;
import io.netty.handler.codec.string.StringEncoder;
import io.netty.util.AttributeKey;
import io.netty.util.CharsetUtil;
import jg.proj.chess.net.Player;

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
    
    pipeline.addLast("framer", new DelimiterBasedFrameDecoder(8192, Delimiters.lineDelimiter()));  
    pipeline.addLast("encoder", new StringEncoder());
    pipeline.addLast("decoder", new StringDecoder());
    /*
    // Decoders
    pipeline.addLast("frameDecoder", new LineBasedFrameDecoder(8192));
    pipeline.addLast("stringDecoder", new StringDecoder(CharsetUtil.UTF_8));

    // Encoder
    pipeline.addLast("stringEncoder", new StringEncoder(CharsetUtil.UTF_8));
    */
    
    /*
    pipeline.addLast("framer", new LengthFieldBasedFrameDecoder(8192, 0, 4, 0, 4));  

    //pipeline.addLast("framer", new DelimiterBasedFrameDecoder(8192, Delimiters.lineDelimiter()));  
    pipeline.addLast("encoder", new LengthFieldPrepender(4, 0, false));
    pipeline.addLast("decoder", new StringDecoder());
    */
    
    Player player = new Player(ch);
    
    AttributeKey<Player> playerKey = AttributeKey.valueOf("player");
    ch.attr(playerKey).set(player);
    
    playerStore.addPlayer(player);
    
    System.out.println(" created player for "+ch.remoteAddress());
    pipeline.addLast("handler", new StagingHandler(server));
    System.out.println(" handler create for plater "+ch.remoteAddress());
  }

}
