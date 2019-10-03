package jg.proj.chess.net.client;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;

public class GameClientHandler extends SimpleChannelInboundHandler<String> {

  private final TestGameClient gameClient;
  
  public GameClientHandler(TestGameClient gameClient) {
    this.gameClient = gameClient;
  }
  
  @Override
  protected void channelRead0(ChannelHandlerContext ctx, String msg) throws Exception {
    System.out.println("DA SERVE: "+msg);
  }

}
