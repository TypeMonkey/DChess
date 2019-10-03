package jg.proj.chess.net;

import io.netty.channel.Channel;

public final class IOUtils {
  
  /**
   * Writes to this channel a message and flushes the channel of buffered data
   * @param channel - the Channel to write to
   * @param message - the String message to send. A line break "\r\n" is appended to it.
   */
  public static void writeAndFlush(Channel channel, String message) {
    channel.writeAndFlush(message+"\r\n");
  }
  
}
