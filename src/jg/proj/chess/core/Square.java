package jg.proj.chess.core;

import java.util.Objects;

import jg.proj.chess.core.units.Unit;

public class Square {

  private final int file;
  private final char rank;
  private final Board hostBoard;

  private Unit attachedUnit;

  public Square(int file, char rank, Board hostBoard){
    this.file = file;
    this.rank = Character.toUpperCase(rank);
    this.hostBoard = hostBoard;
  }

  public void placeUnit(Unit unit){
    attachedUnit = unit;
  }

  public Unit getUnit(){
    return attachedUnit;
  }

  public boolean equals(Object object){
    if (object instanceof Square) {
      Square square = (Square) object;
      return square.file == file && square.rank == rank;
    }
    return false;
  }

  public int hashCode(){
    return Objects.hash(file, rank);
  }

  public Square leftSquare(){
    char newRank = (char) (rank - 1);  
    
    if ('A' <= newRank && newRank <= 'H') {
      return hostBoard.getSquares()[file - 1][newRank - 'A'];
    }
    return null;
  }

  public Square rightSquare(){
    char newRank = (char) (rank + 1);  
    
    if ('A' <= newRank && newRank <= 'H') {
      return hostBoard.getSquares()[file - 1][newRank - 'A'];
    }
    return null;
  }

  public Square upSquare(){
    int newFile = file - 1;
    
    if (1 <= newFile && newFile <= 8) {
      return hostBoard.getSquares()[newFile - 1][rank - 'A'];
    }
    
    return null;
  }

  public Square downSquare(){
    int newFile = file + 1;
    
    if (1 <= newFile && newFile <= 8) {
      return hostBoard.getSquares()[newFile - 1][rank - 'A'];
    }
    
    return null;
  }

  public int getFile() {
    return file;
  }

  public char getRank() {
    return rank;
  }
  
  public Board getHostBoard(){
    return hostBoard;
  }

  public String toString(){
    return file+String.valueOf(rank)+"|"+(attachedUnit != null ? attachedUnit.toString() : "");
  }
}
