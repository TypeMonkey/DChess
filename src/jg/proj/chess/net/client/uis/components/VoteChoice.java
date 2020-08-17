package jg.proj.chess.net.client.uis.components;

import jg.proj.chess.core.Square;

/**
 * A convenient class to track a user's vote choice
 * as received from the user interface
 * 
 * @author Jose
 *
 */
public class VoteChoice {

  private Square from;
  private Square to;
  
  public VoteChoice() {}
  
  public void setFromChoice(Square from) {
    this.from = from;
  }
  
  public void setToChoice(Square to) {
    this.to = to;
  }
  
  public Square getFromChoice() {
    return from;
  }
  
  public Square getToChoice() {
    return to;
  }
  
  public boolean isVoteComplete() {
    return from != null && to != null;
  }
  
  public String toString() {
    return (from != null ? from.toString(): "")+">>"+(to != null ? to.toString() : "");
  }
}
