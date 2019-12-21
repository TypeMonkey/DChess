package jg.proj.chess.core.tests;

import static org.junit.Assert.*;

import java.util.Set;

import org.junit.BeforeClass;
import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.runners.MethodSorters;

import jg.proj.chess.core.Board;
import jg.proj.chess.core.DefaultBoardPreparer;
import jg.proj.chess.core.Square;
import jg.proj.chess.core.TeamInformation;
import jg.proj.chess.core.units.InvalidMove;
import jg.proj.chess.core.units.Pawn;
import jg.proj.chess.core.units.Rook;
import jg.proj.chess.core.units.Unit;
import jg.proj.chess.core.units.Unit.UnitType;

@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class DefaultBoardTests {

  private static Board board;
  private static TeamInformation info;
  
  @BeforeClass
  public static void createBoard(){
    board = new Board(8, 8);
    info = board.initialize(new DefaultBoardPreparer());
    
    System.out.println(board.parsableToString());
    System.out.println("-----------------");
  }
  
  @Test
  public void test1DefaultPreparation(){    
    //check Team 1 positions
    assertEquals(board.querySquare('a', 1).getUnit().getType(), UnitType.ROOK);
    assertEquals(board.querySquare('b', 1).getUnit().getType(), UnitType.KNIGHT);
    assertEquals(board.querySquare('c', 1).getUnit().getType(), UnitType.BISHOP);
    assertEquals(board.querySquare('d', 1).getUnit().getType(), UnitType.KING);
    assertEquals(board.querySquare('e', 1).getUnit().getType(), UnitType.QUEEN);
    assertEquals(board.querySquare('f', 1).getUnit().getType(), UnitType.BISHOP);
    assertEquals(board.querySquare('g', 1).getUnit().getType(), UnitType.KNIGHT);
    assertEquals(board.querySquare('h', 1).getUnit().getType(), UnitType.ROOK);
    
    //check pawns
    for(char file = 'A'; file <= 'H'; file++){
      assertEquals(board.querySquare(file, 2).getUnit().getType(), UnitType.PAWN);
    }
       
    for(char file = 'A'; file <= 'H'; file++){
      for(int rank = 1; rank <= 2; rank++){
        assertEquals(board.querySquare(file, rank).getUnit().getTeamID(), 1);
      }
    }
    
    //check Team 2 positions
    assertEquals(board.querySquare('a', 8).getUnit().getType(), UnitType.ROOK);
    assertEquals(board.querySquare('b', 8).getUnit().getType(), UnitType.KNIGHT);
    assertEquals(board.querySquare('c', 8).getUnit().getType(), UnitType.BISHOP);
    assertEquals(board.querySquare('d', 8).getUnit().getType(), UnitType.KING);
    assertEquals(board.querySquare('e', 8).getUnit().getType(), UnitType.QUEEN);
    assertEquals(board.querySquare('f', 8).getUnit().getType(), UnitType.BISHOP);
    assertEquals(board.querySquare('g', 8).getUnit().getType(), UnitType.KNIGHT);
    assertEquals(board.querySquare('h', 8).getUnit().getType(), UnitType.ROOK);
    
    //check pawns
    for(char file = 'A'; file <= 'H'; file++){
      assertEquals(board.querySquare(file, 7).getUnit().getType(), UnitType.PAWN);
    }
       
    for(char file = 'A'; file <= 'H'; file++){
      for(int rank = 7; rank <= 8; rank++){
        assertEquals(board.querySquare(file, 8).getUnit().getTeamID(), 2);
      }
    }
    
    //the ranks from 3 to 6 should have no units
    for(int rank = 3; rank <= 6; rank++){
      for(char file = 'A'; file <= 'H'; file++){
        assertEquals(board.querySquare(file, rank).getUnit() == null, true);
      }
    }
  }
  
  @Test
  public void test2UnitPossibles(){
    //all the none-pawn units at the star should have zero possible landings except knights   
    
    //Team 1
    for(char file = 'A'; file <= 'H'; file++){
      Unit unit = board.querySquare(file, 1).getUnit();
      if (unit.getType() == UnitType.KNIGHT) {
        continue;
      }
      
      Set<Square> poss = unit.possibleDestinations();
      System.out.println(" FOR: "+ unit);
      System.out.println("     "+poss);
      
      assertTrue(unit.possibleDestinations().isEmpty());
    }
    
    //Team 2
    for(char file = 'A'; file <= 'H'; file++){
      Unit unit = board.querySquare(file, 8).getUnit();
      if (unit.getType() == UnitType.KNIGHT) {
        continue;
      }
      Set<Square> poss = unit.possibleDestinations();
      System.out.println(" FOR: "+ unit);
      System.out.println("     "+poss);
      
      assertTrue(unit.possibleDestinations().isEmpty());
    }
    
    
    //check knights. Should only have 2 moves from the get go
    System.out.println("-------------");
    System.out.println(board.querySquare('b', 1).getUnit().possibleDestinations());
    assertEquals(board.querySquare('b', 1).getUnit().possibleDestinations().size(), 2);
    
    assertEquals(board.querySquare('g', 1).getUnit().possibleDestinations().size(), 2);
    assertEquals(board.querySquare('b', 8).getUnit().possibleDestinations().size(), 2);
    assertEquals(board.querySquare('g', 8).getUnit().possibleDestinations().size(), 2);

    //all pawns should have two possible destinations
    //Team 1
    for(char file = 'A'; file <= 'H'; file++){
      Pawn unit = (Pawn) board.querySquare(file, 2).getUnit();
      Set<Square> poss = unit.possibleDestinations();
      System.out.println(" FOR: "+ unit);
      System.out.println("     "+poss);
      
      assertTrue(unit.possibleDestinations().size() == 2);
    }
    
    //Team 2
    for(char file = 'A'; file <= 'H'; file++){
      Pawn unit = (Pawn) board.querySquare(file, 7).getUnit();
      Set<Square> poss = unit.possibleDestinations();
      System.out.println(" FOR: "+ unit);
      System.out.println("     "+poss);
      
      assertTrue(unit.possibleDestinations().size() == 2);
    }
  }
  
  @Test
  public void test3Pawn(){
    //Test Team 1 first (occupies the north)
    Pawn pawn = (Pawn) board.querySquare('b', 2).getUnit();
    
    try {
      pawn.moveTo(board.querySquare('b', 3));
      
      assertTrue(pawn.getCurrentSquare().equals(board.querySquare('b', 3)));
      assertTrue(pawn == board.querySquare('b', 3).getUnit());
    } catch (InvalidMove e) {
      fail("Actually a valid move!");
    }
    
    pawn = (Pawn) board.querySquare('f', 2).getUnit();
    try {
      pawn.moveTo(board.querySquare('f', 4));
      pawn.moveTo(board.querySquare('f', 5));
    } catch (InvalidMove e) {
      fail("Actually a valid move!");
    }
    
    Pawn enemyPawn = (Pawn) board.querySquare('f', 7).getUnit();
    try {
      enemyPawn.moveTo(enemyPawn.getCurrentSquare().upSquare());
    } catch (InvalidMove e) {
      fail("Actually a valid move!");
    }
    
    assertEquals(pawn.possibleDestinations().size(), 0);
    try {
      pawn.moveTo(pawn.getCurrentSquare().downSquare());
      fail("Not a valid move");
    } catch (InvalidMove e) {
      assertTrue(true);
    }
    assertEquals(enemyPawn.possibleDestinations().size(), 0);
    try {
      enemyPawn.moveTo(enemyPawn.getCurrentSquare().upSquare());
      fail("Not a valid move");
    } catch (InvalidMove e) {
      assertTrue(true);
    }
    
    enemyPawn = (Pawn) board.querySquare('e', 7).getUnit();
    try {
      enemyPawn.moveTo(enemyPawn.getCurrentSquare().upSquare());
      pawn.moveTo(pawn.getCurrentSquare().downSquare().leftSquare());
      
      System.out.println(enemyPawn.getCurrentSquare());
      assertTrue(enemyPawn.getCurrentSquare() == null);
      assertTrue(pawn.getCurrentSquare() != null);
      
      assertEquals(pawn.getCurrentSquare(), board.querySquare('e', 6));
    } catch (InvalidMove e) {
      fail("Actually a valid move!");
    }
    
    System.out.println("-----------------------");
    System.out.println(board);
    
    pawn = (Pawn) board.querySquare('a', 2).getUnit();
    try {
      pawn.moveTo(pawn.getCurrentSquare().downSquare());      
      assertEquals(pawn.getCurrentSquare(), board.querySquare('a', 3));
      
      System.out.println(board);
      
      pawn.moveTo(pawn.getCurrentSquare().downSquare());      
      assertEquals(pawn.getCurrentSquare(), board.querySquare('a', 4));
      
      pawn.moveTo(pawn.getCurrentSquare().downSquare());      
      assertEquals(pawn.getCurrentSquare(), board.querySquare('a', 5));
      
      pawn.moveTo(pawn.getCurrentSquare().downSquare());      
      assertEquals(pawn.getCurrentSquare(), board.querySquare('a', 6));
      
      pawn.moveTo(pawn.getCurrentSquare().downSquare().rightSquare());
      assertEquals(pawn.getCurrentSquare(), board.querySquare('b', 7));
      
      pawn.moveTo(pawn.getCurrentSquare().downSquare().rightSquare());
      assertEquals(pawn.getCurrentSquare(), board.querySquare('c', 8));

      System.out.println(board);
    } catch (InvalidMove e) {
      fail("Actually a valid move!");
    }
    
    Rook rook = (Rook) board.querySquare('a', 1).getUnit();
    try {
      rook.moveTo(board.querySquare('a', 7));
      assertEquals(rook.getCurrentSquare(), board.querySquare('a', 7));
      
      rook.moveTo(rook.getCurrentSquare().downSquare());
      assertEquals(rook.getCurrentSquare(), board.querySquare('a', 8));

      rook.moveTo(rook.getCurrentSquare().rightSquare());
      assertEquals(rook.getCurrentSquare(), board.querySquare('b', 8));

      System.out.println(board);
    } catch (InvalidMove e) {
      fail("Actually a valid move!");
    }
    
    try {
      rook.moveTo(rook.getCurrentSquare().rightSquare());
    } catch (InvalidMove e) {
      assertTrue(true);
    }
    System.out.println(board);

    try {
      rook.moveTo(rook.getCurrentSquare().upSquare());
      assertEquals(rook.getCurrentSquare(), board.querySquare('b', 7));
      
      rook.moveTo(rook.getCurrentSquare().rightSquare());
      assertEquals(rook.getCurrentSquare(), board.querySquare('c', 7));

      rook.moveTo(rook.getCurrentSquare().rightSquare());
      assertEquals(rook.getCurrentSquare(), board.querySquare('d', 7));
      
      rook.moveTo(rook.getCurrentSquare().downSquare());
      assertEquals(rook.getCurrentSquare(), board.querySquare('d', 8));

      //assert win condition. A.k.A: Team 2's only king's current square is null
      assertNull(info.getTeamTwo().get(UnitType.KING).get(0).getCurrentSquare());
    } catch (InvalidMove e) {
      fail("Actually a valid move!");
    }
  }

}
