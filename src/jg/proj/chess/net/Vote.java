package jg.proj.chess.net;

import jg.proj.chess.net.server.Player;

public class Vote {

  private final Player voter;
  
  private final int fileOrigin; 
  private final char rankOrigin;
  
  private final int fileDest; 
  private final char rankDest;
  
  public Vote(int fileOrigin, char rankOrigin, int fileDest, char rankDest, Player voter) {
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
    return fileOrigin+String.valueOf(rankOrigin)+">"+fileDest+String.valueOf(rankDest);
  }

  public int getFileOrigin() {
    return fileOrigin;
  }

  public char getRankOrigin() {
    return rankOrigin;
  }

  public int getFileDest() {
    return fileDest;
  }

  public char getRankDest() {
    return rankDest;
  } 
  
  public Player getVoter(){
    return voter;
  }
}
