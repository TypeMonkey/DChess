package jg.proj.chess.net;

import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;

public final class IOUtils {
  
  /**
   * Writes to this channel a message and flushes the channel of buffered data
   * @param channel - the Channel to write to
   * @param message - the String message to send. A line break "\r\n" is appended to it.
   */
  public static void writeAndFlush(Channel channel, String message) {
    ChannelFuture future = channel.writeAndFlush(message+"\r\n");
    future.syncUninterruptibly();
    
    if (!future.isSuccess()) {
      System.err.println(" write error! "+future.cause());
    }
    else {
      System.out.println("  ---> WROTE: "+message);
    }
  }
  
}
