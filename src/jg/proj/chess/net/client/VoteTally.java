package jg.proj.chess.net.client;

/**
 * Stores the vote count of a specific move
 * @author Jose
 *
 */
public class VoteTally {

  private final int oldFile;
  private final char oldRank;
  
  private final int newFile;
  private final char newRank;
  
  private final int voteCount;
  
  public VoteTally(int oldFile, char oldRank, int newFile, char newRank, int voteCount) {
    this.oldFile = oldFile;
    this.oldRank = oldRank;
    this.newFile = newFile;
    this.newRank = newRank;
    this.voteCount = voteCount;
  }
  
  public int getVoteCount() {
    return voteCount;
  }

  public int getOldFile() {
    return oldFile;
  }

  public char getOldRank() {
    return oldRank;
  }

  public int getNewFile() {
    return newFile;
  }

  public char getNewRank() {
    return newRank;
  }
}
