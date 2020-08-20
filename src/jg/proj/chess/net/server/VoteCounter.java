package jg.proj.chess.net.server;

import jg.proj.chess.net.Vote;

public class VoteCounter{
  
  public final Vote vote; 
  public final int votes;
  
  public VoteCounter(Vote vote, int voteCount) {
    this.vote = vote;
    this.votes = voteCount;
  }
  
  @Override
  public String toString() {
    return "CNT: "+votes+" , "+vote;
  }
}
