package jg.proj.chess.core;

/**
 * Represents a game board
 * @author Jose
 *
 */
public class Board {
  private final int fileWidth;
  private final int rankWidth;

  private final Square [][] squares;

  public Board(int fileWidth, int rankWidth){
    squares = new Square[fileWidth][rankWidth];
    this.fileWidth = fileWidth;
    this.rankWidth = rankWidth;
  }

  public TeamInformation initialize(BoardPreparer preparer){
    return preparer.prepareBoard(this, fileWidth, rankWidth);
  }
  
  public Square[][] getSquares(){
    return squares;
  }
  
  public Square querySquare(char file, int rank){
    int row = rank - 1;
    int col = Character.toUpperCase(file) - 'A';
    
    //System.out.println("files: "+row+" | ranks: "+col);
    if ((0 <= row && row < rankWidth) && (0 <= col && col < fileWidth)) {
      return squares[row][col];
    }
    
    return null;
  }
  
  public String toString(){
    return parsableToString().replace("~", System.lineSeparator());
  }
  
  public String parsableToString(){
    String header = "  | a | b | c | d | e | f | g | h |~";
    header       += "-----------------------------------~";
    for(int i = 0; i < rankWidth; i++){
      Square [] row = squares[i];
      header += (i+1)+" |"; 
      for(Square square : row){
        header += (square.getUnit() != null ? square.getUnit().toString() : "   ")+"|";
      }
      header += "~";
    }
    return header;
  }
}
