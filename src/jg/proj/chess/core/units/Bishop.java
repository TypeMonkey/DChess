package jg.proj.chess.core.units;

import java.util.Set;

import jg.proj.chess.core.Board;
import jg.proj.chess.core.Square;
import jg.proj.chess.utils.NonNullHashSet;

public class Bishop extends Unit{

  public Bishop(int teamID, Square initialSquare) {
    super(UnitType.BISHOP, teamID, initialSquare);
  }

  @Override
  public Set<Square> possibleDestinations() {
    NonNullHashSet<Square> possibles = new NonNullHashSet<>();

    Board board = currentSquare.getHostBoard();
    Square northWest = null, northEast = null, southWest = null, southEast = null;
    
    //flags on whether to stop pursuing directions
    boolean northWestFlag = false, northEastFlag = false, southWestFlag = false, southEastFlag = false;
    
    //calculate each direction    
    int i = 1;
    do {
      //calculate NW
      northWest = northWestFlag ? null : board.querySquare( currentSquare.getFile() - i, (char) (currentSquare.getRank() - i));
      if (northWest != null && northWest.getUnit() != null) {
        northWestFlag = true;
      }
      
      //calculate NE
      northEast = northEastFlag ? null : board.querySquare(currentSquare.getFile() + i, (char) (currentSquare.getRank() - i));
      if (northEast != null && northEast.getUnit() != null) {
        northEastFlag = true;
      }
      
      //calculate SW
      southWest = southWestFlag ? null : board.querySquare(currentSquare.getFile() - i, (char) (currentSquare.getRank() + i));
      if (southWest != null && southWest.getUnit() != null) {
        southWestFlag = true;
      }
      
      //calculate SE
      southEast = southEastFlag ? null : board.querySquare(currentSquare.getFile() + i, (char) (currentSquare.getRank() + i));
      if (southEast != null && southEast.getUnit() != null) {
        southEastFlag = true;
      }
      
      
      possibles.add( (northEast != null && ((northEast.getUnit() != null && northEast.getUnit().getTeamID() != getTeamID()) || northEast.getUnit() == null)) ? northEast : null);
      possibles.add( (northWest != null && ((northWest.getUnit() != null && northWest.getUnit().getTeamID() != getTeamID()) || northWest.getUnit() == null)) ? northWest : null);
      possibles.add( (southEast != null && ((southEast.getUnit() != null && southEast.getUnit().getTeamID() != getTeamID()) || southEast.getUnit() == null)) ? southEast : null);
      possibles.add( (southWest != null && ((southWest.getUnit() != null && southWest.getUnit().getTeamID() != getTeamID()) || southWest.getUnit() == null))? southWest : null);
      i++;
    } while (northWest != null || northEast != null || southWest != null || southEast != null );
    
    return possibles;
  }

}
