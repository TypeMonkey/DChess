package jg.proj.chess.core;

import jg.proj.chess.core.units.Unit;

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
  
  public Square querySquare(int file, char rank){
    int row = file - 1;
    int col = Character.toUpperCase(rank) - 'A';
    
    //System.out.println("files: "+row+" | ranks: "+col);
    if ((0 <= row && row < rankWidth) && (0 <= col && col < fileWidth)) {
      return squares[row][col];
    }
    
    return null;
  }
  
  public String toString(){
    return parsableToString().replace("|", System.lineSeparator());
  }
  
  /**
   * Returns a string representation of the board that can be easily parsed
   * 
   * Per each row, a unit's string representation is concatenated with each other
   * and separated by comma
   * 
   * Rows are then concatenated with each other, separated by the '|' character
   * 
   * @return a string representation of the board that can be easily parsed
   */
  public String parsableToString(){
    String text = "";
    
    for(Square [] row : squares) {
      String rowRep = "";
      for(Square square : row) {
        if (square.getUnit() == null) {
          rowRep += "~";
        }
        else {
          Unit unit = square.getUnit();
          rowRep += String.valueOf(unit.getType().shortName)+unit.getTeamID();
        }
        rowRep += ",";
      }
      
      //cutout last ","
      rowRep = rowRep.substring(0, rowRep.length() - 1);
      
      rowRep += "|";
      text += rowRep;
    }
    
    //cutout last '|'
    text = text.substring(0, text.length() - 1);
    
    return text;
  }
}
