package jg.proj.chess.net.client;

/**
 * Interface for handling server signals
 * 
 * The DChess server sends signals as formatted: "signal:SIGNAL_VALUE"
 * where SIGNAL_VALUE is an integer
 * 
 * @author Jose Guaro
 *
 */
public interface SignalListener {

  public void handleSignal(int signal);
  
}
