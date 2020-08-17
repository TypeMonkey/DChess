package jg.proj.chess.core.units;

import java.util.Set;

import jg.proj.chess.core.Board;
import jg.proj.chess.core.Square;
import jg.proj.chess.utils.NonNullHashSet;

public class King extends Unit{

  public King(int teamID, Square initialSquare) {
    super(UnitType.KING, teamID, initialSquare);
  }

  @Override
  public Set<Square> possibleDestinations() {
    NonNullHashSet<Square> possibles = new NonNullHashSet<>();
    
    //A King moves one step in all directions
    
    Board board = currentSquare.getHostBoard();
    
    Square north = null, south = null, east = null, west = null, northWest = null, northEast = null, southWest = null, southEast = null;
    
    //calculate north
    north = board.querySquare(currentSquare.getFile(), (char) (currentSquare.getRank() - 1));
    //calculate south
    south = board.querySquare(currentSquare.getFile(), (char) (currentSquare.getRank() + 1));
    //calculate east
    east = board.querySquare((char) (currentSquare.getFile() - 1), currentSquare.getRank());
    //calculate west
    west = board.querySquare((char) (currentSquare.getFile() + 1), currentSquare.getRank());
    
    possibles.add( (south != null && south.getUnit() != null && south.getUnit().getTeamID() != getTeamID()) ? south : null);
    possibles.add( (north != null && north.getUnit() != null && north.getUnit().getTeamID() != getTeamID()) ? north : null);
    possibles.add( (west != null && west.getUnit() != null && west.getUnit().getTeamID() != getTeamID()) ? west : null);
    possibles.add( (east != null && east.getUnit() != null && east.getUnit().getTeamID() != getTeamID()) ? east : null);
    
    //calculate NW
    northWest = board.querySquare(currentSquare.getFile() - 1, (char) (currentSquare.getRank() - 1));
    //calculate NE
    northEast = board.querySquare(currentSquare.getFile() + 1, (char) (currentSquare.getRank() - 1));
    //calculate SW
    southWest = board.querySquare(currentSquare.getFile() - 1, (char) (currentSquare.getRank() + 1));
    //calculate SE
    southEast = board.querySquare(currentSquare.getFile() + 1, (char) (currentSquare.getRank() + 1));
    
    possibles.add( (northEast != null && northEast.getUnit() != null && northEast.getUnit().getTeamID() != getTeamID()) ? northEast : null);
    possibles.add( (northWest != null && northWest.getUnit() != null && northWest.getUnit().getTeamID() != getTeamID()) ? northWest : null);
    possibles.add( (southEast != null && southEast.getUnit() != null && southEast.getUnit().getTeamID() != getTeamID()) ? southEast : null);
    possibles.add( (southWest != null && southWest.getUnit() != null && southWest.getUnit().getTeamID() != getTeamID()) ? southWest : null);
    
    return possibles;
  }

}
