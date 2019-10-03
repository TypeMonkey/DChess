package jg.proj.chess.core;

import java.util.Objects;

import jg.proj.chess.core.units.Unit;

public class Square {

  private final char file;
  private final int rank;
  private final Board hostBoard;

  private Unit attachedUnit;

  public Square(char file, int rank, Board hostBoard){
    this.file = Character.toUpperCase(file);
    this.rank = rank;
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
    char newFile = (char) (file - 1);
    if ('A' <= newFile && newFile <= 'H') {
      return hostBoard.getSquares()[rank - 1][newFile - 'A'];
    }
    return null;
  }

  public Square rightSquare(){
    char newFile = (char) (file + 1);
    if ('A' <= newFile && newFile <= 'H') {
      return hostBoard.getSquares()[rank - 1][newFile - 'A'];
    }
    return null;
  }

  public Square upSquare(){
    int newRank = rank - 1;
    if (1 <= newRank && newRank <= 8) {
      return hostBoard.getSquares()[newRank - 1][file - 'A'];
    }
    return null;
  }

  public Square downSquare(){
    int newRank = rank + 1;
    if (1 <= newRank && newRank <= 8) {
      return hostBoard.getSquares()[newRank - 1][file - 'A'];
    }
    return null;
  }

  public char getFile() {
    return file;
  }

  public int getRank() {
    return rank;
  }
  
  public Board getHostBoard(){
    return hostBoard;
  }

  public String toString(){
    return String.valueOf(file)+rank+"|"+(attachedUnit != null ? attachedUnit.toString() : "");
  }
}
