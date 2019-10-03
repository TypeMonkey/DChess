package jg.proj.chess.core;

import java.util.List;
import java.util.Map;

import jg.proj.chess.core.units.Unit;
import jg.proj.chess.core.units.Unit.UnitType;

public class TeamInformation{
  private Map<UnitType, List<Unit>> teamOne;
  private Map<UnitType, List<Unit>> teamTwo;
  
  public TeamInformation(Map<UnitType, List<Unit>> teamOne, Map<UnitType, List<Unit>> teamTwo){
    this.teamOne = teamOne;
    this.teamTwo = teamTwo;
  }

  public Map<UnitType, List<Unit>> getTeamOne() {
    return teamOne;
  }

  public Map<UnitType, List<Unit>> getTeamTwo() {
    return teamTwo;
  }
}