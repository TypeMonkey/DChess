package jg.proj.chess.net.client;

/**
 * Stores the vote count of a specific vote
 * @author Jose
 */
public class VoteTally {

  private final int oldFile;
  private final char oldRank;
  
  private final int newFile;
  private final char newRank;
  
  private final int voteCount;
  
  /**
   * Constructs a VoteTally
   * @param oldFile - the original file
   * @param oldRank - the original rank
   * @param newFile - the destination file
   * @param newRank - the destination rank
   * @param voteCount - the amount of players who voted for this move.
   */
  public VoteTally(int oldFile, char oldRank, int newFile, char newRank, int voteCount) {
    this.oldFile = oldFile;
    this.oldRank = oldRank;
    this.newFile = newFile;
    this.newRank = newRank;
    this.voteCount = voteCount;
  }
  
  /**
   * Retrieves the vote count (the amount of players who voted for this move)
   * 
   * Note: The vote count returned may not accurately reflect
   *       the actual, current vote count for this move.
   *       The vote count returned is the vote count at the time 
   *       this VoteTally was created.
   * 
   * @return the vote count (the amount of players who voted for this move)
   */
  public int getVoteCount() {
    return voteCount;
  }

  /**
   * Retrieves the old file
   * @return the old file
   */
  public int getOldFile() {
    return oldFile;
  }

  /**
   * Retrieves the old rank
   * @return the old rank
   */
  public char getOldRank() {
    return oldRank;
  }

  /**
   * Retrieves the destination file
   * @return the destination file
   */
  public int getNewFile() {
    return newFile;
  }

  /**
   * Retrieves the destination rank
   * @return the destination rank
   */
  public char getNewRank() {
    return newRank;
  }
}
