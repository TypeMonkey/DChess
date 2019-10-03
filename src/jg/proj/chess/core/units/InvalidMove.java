package jg.proj.chess.core.units;

import jg.proj.chess.core.Square;
import jg.proj.chess.core.units.Unit.UnitType;

public class InvalidMove extends Exception{
  
  public InvalidMove(UnitType type, Square origin, Square destination){
    super("A "+type+" cannot move from "+origin+" to "+destination);
  }

}
