package jg.proj.chess.net;

import jg.proj.chess.net.server.Player;

public class Vote {

  private final Player voter;
  
  private final char fileOrigin; 
  private final int rankOrigin;
  
  private final char fileDest; 
  private final int rankDest;
  
  public Vote(char fileOrigin, int rankOrigin, char fileDest, int rankDest, Player voter) {
    this.fileOrigin = fileOrigin;
    this.rankOrigin = rankOrigin;
    
    this.fileDest = fileDest;
    this.rankDest = rankDest;
    
    this.voter = voter;
  }
  
  @Override
  public int hashCode() {
    return toString().hashCode();
  }
  
  @Override
  public boolean equals(Object obj) {
    if (obj instanceof Vote) {
      Vote vote = (Vote) obj;
      return vote.toString().equals(toString());
    }
    return false;
  }
  
  @Override
  public String toString() {
    return String.valueOf(fileOrigin)+rankOrigin+">"+String.valueOf(fileDest)+rankDest;
  }

  public char getFileOrigin() {
    return fileOrigin;
  }

  public int getRankOrigin() {
    return rankOrigin;
  }

  public char getFileDest() {
    return fileDest;
  }

  public int getRankDest() {
    return rankDest;
  } 
  
  public Player getVoter(){
    return voter;
  }
}
