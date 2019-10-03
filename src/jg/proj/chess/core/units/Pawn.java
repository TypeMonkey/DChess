package jg.proj.chess.core.units;

import java.util.Set;

import jg.proj.chess.core.Square;
import jg.proj.chess.utils.NonNullHashSet;

public class Pawn extends Unit{

  private boolean hasMovedAlready;
  
  public Pawn(int teamID, Square initialSquare) {
    super(UnitType.PAWN, teamID, initialSquare);
  }
  
  public Square moveTo(Square desination) throws InvalidMove{
    Square square = super.moveTo(desination);
    if (!hasMovedAlready) {
      hasMovedAlready = true;
    }
    
    return square;
  }

  @Override
  public Set<Square> possibleDestinations() {  
    //Use a hashset that doesn't allow for null values
    NonNullHashSet<Square> possibles = new NonNullHashSet<>();
    
    if (getTeamID() == 1) {
      Square nextStep = currentSquare.downSquare();
      possibles.add(nextStep == null || nextStep.getUnit() == null ? nextStep : null);
      
      if (!hasMovedAlready && nextStep != null && nextStep.getUnit() == null) {
        possibles.add(nextStep.downSquare());
      }
      
      Square west = currentSquare.leftSquare();
      Square southWest = west != null ? west.downSquare() : null;
      
      //only add diagonal moves if enemy unit is present on them
      possibles.add(southWest != null && southWest.getUnit() != null && southWest.getUnit().getTeamID() != getTeamID() ? southWest : null);
      
      Square east = currentSquare.rightSquare();
      Square southEast = east != null ? east.downSquare() : null;
      
      //only add diagonal moves if enemy unit is present on them
      possibles.add(southEast != null && southEast.getUnit() != null && southEast.getUnit().getTeamID() != getTeamID() ? southEast : null);
    }
    else if (getTeamID() == 2) {
      Square nextStep = currentSquare.upSquare();
      possibles.add(nextStep == null || nextStep.getUnit() == null ? nextStep : null);
      
      if (!hasMovedAlready && nextStep != null && nextStep.getUnit() == null) {
        possibles.add(nextStep.upSquare());
      }     
      
      Square west = currentSquare.leftSquare();
      Square northWest = west != null ? west.upSquare() : null;
      
      //only add diagonal moves if enemy unit is present on them
      possibles.add(northWest != null && northWest.getUnit() != null && northWest.getUnit().getTeamID() != getTeamID() ? northWest : null);
      
      Square east = currentSquare.rightSquare();
      Square northEast = east != null ? east.upSquare() : null;
      
      //only add diagonal moves if enemy unit is present on them
      possibles.add(northEast != null && northEast.getUnit() != null && northEast.getUnit().getTeamID() != getTeamID() ? northEast : null);
    }
    
    return possibles;
  }

}
