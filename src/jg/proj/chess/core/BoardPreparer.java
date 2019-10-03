package jg.proj.chess.core;

/**
 * A BoardPreparer prepares a board prior to a game
 * @author Jose
 *
 */
public interface BoardPreparer {

  /**
   * Prepares the given board with
   * starting unit positions
   * @param board - the Board to prepare
   */
  public TeamInformation prepareBoard(Board board, int fileWidth, int rankWidth);
}
