package jg.proj.chess.core.units;

import java.util.Set;

import jg.proj.chess.core.Board;
import jg.proj.chess.core.Square;
import jg.proj.chess.utils.NonNullHashSet;

public class Rook extends Unit{

  public Rook(int teamID, Square initialSquare) {
    super(UnitType.ROOK, teamID, initialSquare);
  }

  @Override
  public Set<Square> possibleDestinations() {
    NonNullHashSet<Square> possibles = new NonNullHashSet<>();
    
    //a Rook moves in a straight cross in north, south, west, east directions
    
    System.out.println("null???? "+currentSquare);
    Board board = currentSquare.getHostBoard();
    Square north = null, south = null, east = null, west = null;
    
    //flags on whether to stop pursuing directions
    boolean northFlag = false, southFlag = false, eastFlag = false, westFlag = false;
    
    //calculate each direction    
    int i = 1;
    do {
      //calculate north
      north = northFlag ? null : board.querySquare(currentSquare.getFile(), (char) (currentSquare.getRank() - i));
      if (north != null && north.getUnit() != null) {
        northFlag = true;
      }
      
      //calculate south
      south = southFlag ? null : board.querySquare(currentSquare.getFile(), (char) (currentSquare.getRank() + i));
      if (south != null && south.getUnit() != null) {
        southFlag = true;
      }
      
      //calculate east
      east = eastFlag ? null : board.querySquare(currentSquare.getFile() - i, currentSquare.getRank());
      if (east != null && east.getUnit() != null) {
        eastFlag = true;
      }
      
      //calculate west
      west = westFlag ? null : board.querySquare(currentSquare.getFile() + i, currentSquare.getRank());
      if (west != null && west.getUnit() != null) {
        westFlag = true;
      }
      
      possibles.add( (south != null && ((south.getUnit() != null && south.getUnit().getTeamID() != getTeamID()) || south.getUnit() == null )) ? south : null);
      possibles.add( (north != null && ((north.getUnit() != null && north.getUnit().getTeamID() != getTeamID()) || north.getUnit() == null)) ? north : null);
      possibles.add( (west != null && ((west.getUnit() != null && west.getUnit().getTeamID() != getTeamID()) || west.getUnit() == null)) ? west : null);
      possibles.add( (east != null && ((east.getUnit() != null && east.getUnit().getTeamID() != getTeamID()) || east.getUnit() == null)) ? east : null);
      i++;
    } while (north != null || south != null || east != null || west != null );
    
    return possibles;
  }

}
