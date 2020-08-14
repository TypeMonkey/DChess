package jg.proj.chess.utils;

import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;

/**
 * A collection of handy IO and String utility methods
 * @author Jose
 *
 */
public final class StringAndIOUtils {
  
  /**
   * Writes to this channel a message and flushes the channel of buffered data
   * @param channel - the Channel to write to
   * @param message - the String message to send. A line break "\r\n" is appended to it.
   */
  public static void writeAndFlush(Channel channel, String message) {
    ChannelFuture future = channel.writeAndFlush(message+"\r\n");
    future.syncUninterruptibly();
    
    //DEV_CODE: Debug code
    if (!future.isSuccess()) {
      System.err.println(" write error! "+future.cause());
    }
    else {
      System.out.println("  ---> WROTE: "+message);
    }
  }
  
  /**
   * Parses a string in the form of "identifier=value".
   * 
   * @param parseme - the string to parse
   * 
   * @return a String array of size 2 where the first index
   *         is the identifier and the second index is the value
   *         , or null if the string provided is not in the given form
   */
  public static String [] parseAssignment(String parseme) {
    String [] split = parseme.split("=");
    if (split.length == 2) {
      return split;
    }
    return null;
  }
}
