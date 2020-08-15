package jg.proj.chess.net.client;

public class VoteTally {

  private final char oldFile;
  private final int oldRank;
  
  private final char newFile;
  private final int newRank;
  
  private final int voteCount;
  
  public VoteTally(char oldFile, int oldRank, char newFile, int newRank, int voteCount) {
    this.oldFile = oldFile;
    this.oldRank = oldRank;
    this.newFile = newFile;
    this.newRank = newRank;
    this.voteCount = voteCount;
  }
  
  public int getVoteCount() {
    return voteCount;
  }

  public char getOldFile() {
    return oldFile;
  }

  public int getOldRank() {
    return oldRank;
  }

  public char getNewFile() {
    return newFile;
  }

  public int getNewRank() {
    return newRank;
  }
}
