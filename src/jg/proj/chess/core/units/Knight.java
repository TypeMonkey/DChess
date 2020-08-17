package jg.proj.chess.core.units;

import java.util.Set;
import java.util.stream.Collectors;

import jg.proj.chess.core.Board;
import jg.proj.chess.core.Square;
import jg.proj.chess.utils.NonNullHashSet;

public class Knight extends Unit{

  public Knight(int teamID, Square initialSquare) {
    super(UnitType.KNIGHT, teamID, initialSquare);
  }

  @Override
  public Set<Square> possibleDestinations() {
    NonNullHashSet<Square> possibles = new NonNullHashSet<>();
    
    //A knight moves in a shape similar to the templar cross
    //For each compass direction: North, South, East, West
    //there's two destinations for each
    
    Board board = currentSquare.getHostBoard();   
    
    //north possibilities
    Square northOne = board.querySquare(currentSquare.getFile() - 1, (char) (currentSquare.getRank() - 2));
    Square northTwo = board.querySquare(currentSquare.getFile() + 1, (char) (currentSquare.getRank() - 2)); 
    possibles.add(northOne);
    possibles.add(northTwo);

    //south possibilities
    Square southOne = board.querySquare(currentSquare.getFile() - 1, (char) (currentSquare.getRank() + 2));
    Square southTwo = board.querySquare(currentSquare.getFile() + 1, (char) (currentSquare.getRank() + 2));
    possibles.add(southOne);
    possibles.add(southTwo);
    
    //west possibilities
    Square westOne = board.querySquare(currentSquare.getFile() - 2, (char) (currentSquare.getRank() + 1));
    Square westTwo = board.querySquare(currentSquare.getFile() - 2, (char) (currentSquare.getRank() - 1));
    possibles.add(westOne);
    possibles.add(westTwo);
    
    //east possibilities
    Square eastOne = board.querySquare(currentSquare.getFile() + 2, (char) (currentSquare.getRank() + 1));
    Square eastTwo = board.querySquare(currentSquare.getFile() + 2, (char) (currentSquare.getRank() - 1));
    possibles.add(eastOne);
    possibles.add(eastTwo);
    
    Set<Square> actual = possibles.stream().filter(x -> x.getUnit() == null || x.getUnit().getTeamID() != getTeamID()).collect(Collectors.toSet()); 
    return actual;
  }

}
