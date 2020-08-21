package jg.proj.chess.utils;

import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.group.ChannelGroup;
import io.netty.channel.group.ChannelGroupFuture;

/**
 * A collection of handy IO and String utility methods
 * @author Jose
 *
 */
public final class StringAndIOUtils {
  
  /**
   * Writes to this channel a message and flushes the channel of buffered data
   * @param channel - the Channel to write to
   * @param message - the String message to send. The null character is appended to the end of the message.
   */
  public static void writeAndFlush(Channel channel, String message) {
    System.out.println("---SENDING!!!!! "+message);
    
    byte[] messBytes = new byte[message.getBytes().length + 1];
    System.arraycopy(message.getBytes(), 0, messBytes, 0, message.getBytes().length);
    messBytes[messBytes.length - 1] = 0;
    
    ChannelFuture future = channel.writeAndFlush(new String(messBytes));
    
    future.syncUninterruptibly();
    
    /*
    //DEV_CODE: Debug code
    if (!future.isSuccess()) {
      System.err.println(" write error! "+future.cause());
    }
    else {
      System.out.println("  ---> WROTE: "+message);
    }
    */
  }
  
  /**
   * Writes to this channel group a message and flushes the channel of buffered data
   * @param channel - the Channel to write to
   * @param message - the String message to send. The null character is appended to the end of the message.
   */
  public static void writeAndFlushGroup(ChannelGroup group, String message) {
    System.out.println("---SENDING TO GROUP!!!!! "+message);
    
    byte[] messBytes = new byte[message.getBytes().length + 1];
    System.arraycopy(message.getBytes(), 0, messBytes, 0, message.getBytes().length);
    messBytes[messBytes.length - 1] = 0;
    
    ChannelGroupFuture future = group.writeAndFlush(new String(messBytes));
    
    future.syncUninterruptibly();
    
    /*
    //DEV_CODE: Debug code
    if (!future.isSuccess()) {
      System.err.println(" write error! "+future.cause());
    }
    else {
      System.out.println("  ---> WROTE: "+message);
    }
    */
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
