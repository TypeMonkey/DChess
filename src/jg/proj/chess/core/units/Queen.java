package jg.proj.chess.core.units;

import java.util.Set;

import jg.proj.chess.core.Board;
import jg.proj.chess.core.Square;
import jg.proj.chess.utils.NonNullHashSet;

public class Queen extends Unit {

  public Queen(int teamID, Square initialSquare) {
    super(UnitType.QUEEN, teamID, initialSquare);
    // TODO Auto-generated constructor stub
  }

  @Override
  public Set<Square> possibleDestinations() {
    NonNullHashSet<Square> possibles = new NonNullHashSet<>();
    
    //a Queen moves in a star. It's a combination of the Knight and Rook moves
    
    Board board = currentSquare.getHostBoard();
    Square north = null, south = null, east = null, west = null, northWest = null, northEast = null, southWest = null, southEast = null;
    
    //flags on whether to stop pursuing directions
    boolean northFlag = false, southFlag = false, eastFlag = false, westFlag = false, 
        northWestFlag = false, northEastFlag = false, southWestFlag = false, southEastFlag = false;
    
    //calculate each direction    
    int i = 1;
    do {
    //calculate north
      north = northFlag ? null : board.querySquare(currentSquare.getFile(), currentSquare.getRank() - i);
      if (north != null && north.getUnit() != null) {
        northFlag = true;
      }
      
      //calculate south
      south = southFlag ? null : board.querySquare(currentSquare.getFile(), currentSquare.getRank() + i);
      if (south != null && south.getUnit() != null) {
        southFlag = true;
      }
      
      //calculate east
      east = eastFlag ? null : board.querySquare((char) (currentSquare.getFile() - i), currentSquare.getRank());
      if (east != null && east.getUnit() != null) {
        eastFlag = true;
      }
      
      //calculate west
      west = westFlag ? null : board.querySquare((char) (currentSquare.getFile() + i), currentSquare.getRank());
      if (west != null && west.getUnit() != null) {
        westFlag = true;
      }
      
      possibles.add( (south != null && ((south.getUnit() != null && south.getUnit().getTeamID() != getTeamID()) || south.getUnit() == null )) ? south : null);
      possibles.add( (north != null && ((north.getUnit() != null && north.getUnit().getTeamID() != getTeamID()) || north.getUnit() == null)) ? north : null);
      possibles.add( (west != null && ((west.getUnit() != null && west.getUnit().getTeamID() != getTeamID()) || west.getUnit() == null)) ? west : null);
      possibles.add( (east != null && ((east.getUnit() != null && east.getUnit().getTeamID() != getTeamID()) || east.getUnit() == null)) ? east : null);
      
    //calculate NW
      northWest = northWestFlag ? null : board.querySquare((char) (currentSquare.getFile() - i), currentSquare.getRank() - i);
      if (northWest != null && northWest.getUnit() != null) {
        northWestFlag = true;
      }
      
      //calculate NE
      northEast = northEastFlag ? null : board.querySquare((char) (currentSquare.getFile() + i), currentSquare.getRank() - i);
      if (northEast != null && northEast.getUnit() != null) {
        northEastFlag = true;
      }
      
      //calculate SW
      southWest = southWestFlag ? null : board.querySquare((char) (currentSquare.getFile() - i), currentSquare.getRank() + i);
      if (southWest != null && southWest.getUnit() != null) {
        southWestFlag = true;
      }
      
      //calculate SE
      southEast = southEastFlag ? null : board.querySquare((char) (currentSquare.getFile() + i), currentSquare.getRank() + i);
      if (southEast != null && southEast.getUnit() != null) {
        southEastFlag = true;
      }
      
      
      possibles.add( (northEast != null && ((northEast.getUnit() != null && northEast.getUnit().getTeamID() != getTeamID()) || northEast.getUnit() == null)) ? northEast : null);
      possibles.add( (northWest != null && ((northWest.getUnit() != null && northWest.getUnit().getTeamID() != getTeamID()) || northWest.getUnit() == null)) ? northWest : null);
      possibles.add( (southEast != null && ((southEast.getUnit() != null && southEast.getUnit().getTeamID() != getTeamID()) || southEast.getUnit() == null)) ? southEast : null);
      possibles.add( (southWest != null && ((southWest.getUnit() != null && southWest.getUnit().getTeamID() != getTeamID()) || southWest.getUnit() == null))? southWest : null);
      i++;
    } while (north != null || south != null || east != null || west != null || 
             northWest != null || northEast != null || southWest != null || southEast != null);
    
    return possibles;
  }

}
