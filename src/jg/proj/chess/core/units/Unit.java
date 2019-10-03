package jg.proj.chess.core.units;

import java.util.Set;

import jg.proj.chess.core.Square;

/**
 * Represents a chess unit.
 * @author Jose
 *
 */
public abstract class Unit {
  
  /**
   * Descriptions for the default units of a chess game 
   * @author Jose
   *
   */
  public enum UnitType{
    KING(1, 'K'),
    QUEEN(1, 'Q'),
    ROOK(2, 'R'),
    BISHOP(2, 'B'),
    KNIGHT(2, 'K'),
    PAWN(8, 'P');
    
    /**
     * Default amount each unit gets per team on a regular 8x8 board
     */
    public final int defaultAmount;
    
    public final char shortName;
    
    private UnitType(int defAmount, char shortName){
      this.defaultAmount = defAmount;
      this.shortName = shortName;
    }
  }

  private final UnitType type;
  private final int teamID;
  
  protected Square currentSquare;
  
  public Unit(UnitType type, int teamID, Square initialSquare){
    this.type = type;
    this.teamID = teamID;   
    this.currentSquare = initialSquare;
  }
  
  /**
   * Moves this unit to the given destination Square. 
   * @param desination - the Square to land on
   * @return the Square this unit occupies prior to moving
   * @throws InvalidMove when the given Square is not a legal destination
   */
  public Square moveTo(Square desination) throws InvalidMove{
    Set<Square> possibles = possibleDestinations();
    
    if (!possibles.contains(desination)) {
      throw new InvalidMove(type, currentSquare, desination);
    }
    
    if (desination.getUnit() != null) {
      desination.getUnit().updateSquare(null);
    }
    desination.placeUnit(this);
    
    Square lastSquare = currentSquare;
    if (lastSquare.getUnit() != null) {
      lastSquare.getUnit().updateSquare(null);
    }
    lastSquare.placeUnit(null);
    
    updateSquare(desination);
    return lastSquare;
  }
  
  /**
   * Calculates the set of possible squares this Unit can land in
   * @return a set of possible squares
   */
  public abstract Set<Square> possibleDestinations();
  
  public void updateSquare(Square square){
    this.currentSquare = square;
  }
  
  public Square getCurrentSquare(){
    return currentSquare;
  }

  public UnitType getType() {
    return type;
  }
  
  public String toString(){
    return type.shortName+"-"+teamID;
  }

  public int getTeamID(){
    return teamID;
  }
}
