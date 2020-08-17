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
    
    System.out.println(board);
    System.out.println("-----------------");
  }
  
  @Test
  public void test1DefaultPreparation(){    
    //check Team 1 positions
    assertEquals(board.querySquare(1, 'a').getUnit().getType(), UnitType.ROOK);
    assertEquals(board.querySquare(1, 'b').getUnit().getType(), UnitType.KNIGHT);
    assertEquals(board.querySquare(1, 'c').getUnit().getType(), UnitType.BISHOP);
    assertEquals(board.querySquare(1, 'd').getUnit().getType(), UnitType.KING);
    assertEquals(board.querySquare(1, 'e').getUnit().getType(), UnitType.QUEEN);
    assertEquals(board.querySquare(1, 'f').getUnit().getType(), UnitType.BISHOP);
    assertEquals(board.querySquare(1, 'g').getUnit().getType(), UnitType.KNIGHT);
    assertEquals(board.querySquare(1, 'h').getUnit().getType(), UnitType.ROOK);
    
    //check pawns
    for(char rank = 'A'; rank <= 'H'; rank++){
      assertEquals(board.querySquare(2, rank).getUnit().getType(), UnitType.PAWN);
    }
       
    for(int file = 1; file <= 2; file++){
      for(char rank = 'A'; rank <= 'H'; rank++){
        assertEquals(board.querySquare(file, rank).getUnit().getTeamID(), 1);
      }
    }
    
    //check Team 2 positions
    assertEquals(board.querySquare(8, 'a').getUnit().getType(), UnitType.ROOK);
    assertEquals(board.querySquare(8, 'b').getUnit().getType(), UnitType.KNIGHT);
    assertEquals(board.querySquare(8, 'c').getUnit().getType(), UnitType.BISHOP);
    assertEquals(board.querySquare(8, 'd').getUnit().getType(), UnitType.KING);
    assertEquals(board.querySquare(8, 'e').getUnit().getType(), UnitType.QUEEN);
    assertEquals(board.querySquare(8, 'f').getUnit().getType(), UnitType.BISHOP);
    assertEquals(board.querySquare(8, 'g').getUnit().getType(), UnitType.KNIGHT);
    assertEquals(board.querySquare(8, 'h').getUnit().getType(), UnitType.ROOK);
    
    //check pawns
    for(char rank = 'A'; rank <= 'H'; rank++){
      assertEquals(board.querySquare(7, rank).getUnit().getType(), UnitType.PAWN);
    }
       
    for(int file = 7; file <= 8; file++){
      for(char rank = 'A'; rank <= 'H'; rank++){
        assertEquals(board.querySquare(file, rank).getUnit().getTeamID(), 2);
      }
    }
    
    //the ranks from 3 to 6 should have no units
    for(int file = 3; file <= 6; file++){
      for(char rank = 'A'; rank <= 'H'; rank++){
        assertEquals(board.querySquare(file, rank).getUnit() == null, true);
      }
    }
  }
  
  @Test
  public void test2UnitPossibles(){
    //all the none-pawn units at the start should have zero possible landings except knights   
    
    //Team 1
    for(char rank = 'A'; rank <= 'H'; rank++){
      Unit unit = board.querySquare(1, rank).getUnit();
      if (unit.getType() == UnitType.KNIGHT) {
        continue;
      }
      
      Set<Square> poss = unit.possibleDestinations();
      System.out.println(" FOR: "+ unit);
      System.out.println("     "+poss);
      
      assertTrue(unit.possibleDestinations().isEmpty());
    }
    
    //Team 2
    for(char rank = 'A'; rank <= 'H'; rank++){
      Unit unit = board.querySquare(8, rank).getUnit();
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
    System.out.println(board.querySquare(1, 'b').getUnit().possibleDestinations());
    assertEquals(board.querySquare(1, 'b').getUnit().possibleDestinations().size(), 2);
    
    assertEquals(board.querySquare(1, 'g').getUnit().possibleDestinations().size(), 2);
    assertEquals(board.querySquare(8, 'b').getUnit().possibleDestinations().size(), 2);
    assertEquals(board.querySquare(8, 'g').getUnit().possibleDestinations().size(), 2);

    //all pawns should have two possible destinations
    //Team 1
    for(char rank = 'A'; rank <= 'H'; rank++){
      Pawn unit = (Pawn) board.querySquare(2, rank).getUnit();
      Set<Square> poss = unit.possibleDestinations();
      System.out.println(" FOR: "+ unit);
      System.out.println("     "+poss);
      
      assertTrue(unit.possibleDestinations().size() == 2);
    }
    
    //Team 2
    for(char rank = 'A'; rank <= 'H'; rank++){
      Pawn unit = (Pawn) board.querySquare(7, rank).getUnit();
      Set<Square> poss = unit.possibleDestinations();
      System.out.println(" FOR: "+ unit);
      System.out.println("     "+poss);
      
      assertTrue(unit.possibleDestinations().size() == 2);
    }
  }
  
  @Test
  public void test3Pawn(){
    //Test Team 1 first (occupies the north)
    Pawn pawn = (Pawn) board.querySquare(2, 'b').getUnit();
    
    try {
      pawn.moveTo(board.querySquare(3, 'b'));
      
      assertTrue(pawn.getCurrentSquare().equals(board.querySquare(3, 'b')));
      assertTrue(pawn == board.querySquare(3, 'b').getUnit());
    } catch (InvalidMove e) {
      fail("Actually a valid move!");
    }
    
    pawn = (Pawn) board.querySquare(2, 'f').getUnit();
    try {
      pawn.moveTo(board.querySquare(4, 'f'));
      pawn.moveTo(board.querySquare(5, 'f'));
    } catch (InvalidMove e) {
      fail("Actually a valid move!");
    }
    
    Pawn enemyPawn = (Pawn) board.querySquare(7, 'f').getUnit();
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
    
    enemyPawn = (Pawn) board.querySquare(7, 'e').getUnit();
    try {
      enemyPawn.moveTo(enemyPawn.getCurrentSquare().upSquare());
      pawn.moveTo(pawn.getCurrentSquare().downSquare().leftSquare());
      
      System.out.println(enemyPawn.getCurrentSquare());
      assertTrue(enemyPawn.getCurrentSquare() == null);
      assertTrue(pawn.getCurrentSquare() != null);
      
      assertEquals(pawn.getCurrentSquare(), board.querySquare(6, 'e'));
    } catch (InvalidMove e) {
      fail("Actually a valid move!");
    }
    
    System.out.println("-----------------------");
    System.out.println(board);
    
    pawn = (Pawn) board.querySquare(2, 'a').getUnit();
    try {
      pawn.moveTo(pawn.getCurrentSquare().downSquare());      
      assertEquals(pawn.getCurrentSquare(), board.querySquare(3, 'a'));
      
      System.out.println(board);
      
      pawn.moveTo(pawn.getCurrentSquare().downSquare());      
      assertEquals(pawn.getCurrentSquare(), board.querySquare(4, 'a'));
      
      pawn.moveTo(pawn.getCurrentSquare().downSquare());      
      assertEquals(pawn.getCurrentSquare(), board.querySquare(5, 'a'));
      
      pawn.moveTo(pawn.getCurrentSquare().downSquare());      
      assertEquals(pawn.getCurrentSquare(), board.querySquare(6, 'a'));
      
      pawn.moveTo(pawn.getCurrentSquare().downSquare().rightSquare());
      assertEquals(pawn.getCurrentSquare(), board.querySquare(7, 'b'));
      
      pawn.moveTo(pawn.getCurrentSquare().downSquare().rightSquare());
      assertEquals(pawn.getCurrentSquare(), board.querySquare(8, 'c'));

      System.out.println(board);
    } catch (InvalidMove e) {
      fail("Actually a valid move!");
    }
    
    Rook rook = (Rook) board.querySquare(1, 'a').getUnit();
    try {
      rook.moveTo(board.querySquare(7, 'a'));
      assertEquals(rook.getCurrentSquare(), board.querySquare(7, 'a'));
      
      rook.moveTo(rook.getCurrentSquare().downSquare());
      assertEquals(rook.getCurrentSquare(), board.querySquare(8, 'a'));

      rook.moveTo(rook.getCurrentSquare().rightSquare());
      assertEquals(rook.getCurrentSquare(), board.querySquare(8, 'b'));

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
      assertEquals(rook.getCurrentSquare(), board.querySquare(7, 'b'));
      
      rook.moveTo(rook.getCurrentSquare().rightSquare());
      assertEquals(rook.getCurrentSquare(), board.querySquare(7, 'c'));

      rook.moveTo(rook.getCurrentSquare().rightSquare());
      assertEquals(rook.getCurrentSquare(), board.querySquare(7, 'd'));
      
      rook.moveTo(rook.getCurrentSquare().downSquare());
      assertEquals(rook.getCurrentSquare(), board.querySquare(8, 'd'));

      //assert win condition. A.k.A: Team 2's only king's current square is null
      assertNull(info.getTeamTwo().get(UnitType.KING).get(0).getCurrentSquare());
    } catch (InvalidMove e) {
      fail("Actually a valid move!");
    }
  }

}
