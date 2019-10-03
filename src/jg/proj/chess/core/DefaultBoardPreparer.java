package jg.proj.chess.core;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import jg.proj.chess.core.units.Bishop;
import jg.proj.chess.core.units.King;
import jg.proj.chess.core.units.Knight;
import jg.proj.chess.core.units.Pawn;
import jg.proj.chess.core.units.Queen;
import jg.proj.chess.core.units.Rook;
import jg.proj.chess.core.units.Unit;
import jg.proj.chess.core.units.Unit.UnitType;

/**
 * Prepares a Board using the traditional setup
 * of a classic chess game:
 *  - 8 x 8 Chess Board
 *  - 8 pawns on A2 to H2 for Team 1
 *  - 8 pawns on A6 to H6 for Team 2
 *
 *  - At A1 and H1, place Rook for Team 1
 *  - At A8 and H8, place Rook for Team 2
 *
 *  - At B1 and G1, place Knight for Team 1
 *  - At B8 and G8, place Knight for Team 2
 *
 *  - At C1 and F1, place Bishop for Team 1
 *  - At C8 and F8, place Bishop for Team 2
 *
 *  - At D1, place King for Team 1
 *  - At D8, place King for Team 2
 *
 *  - At E1, place Queen for Team 1
 *  - At E8, place Queen for Team 2
 *
 * @author Jose
 *
 */
public class DefaultBoardPreparer implements BoardPreparer{

  @Override
  public TeamInformation prepareBoard(Board board, int fileWidth, int rankWidth) {
    Square [][] squares = board.getSquares();
    
    final int TEAM_COUNT = 2;
    HashMap<UnitType, List<Unit>> [] teamArr = new HashMap[TEAM_COUNT];
    teamArr[0] = new HashMap<>();
    teamArr[1] = new HashMap<>();
    
    //create units for each unit
    for(int teamID = 0; teamID <= 1; teamID++){
      HashMap<UnitType, List<Unit>> currentMap = teamArr[teamID];
      for(UnitType type : UnitType.values()){
        for(int i = 0; i < type.defaultAmount; i++){
          
          List<Unit> units = currentMap.containsKey(type) ? currentMap.get(type) : new ArrayList<>();
          
          switch (type) {
          case KING:
            units.add(new King(teamID+1, null));
            break;
          case QUEEN:
            units.add(new Queen(teamID+1, null));
            break;
          case ROOK:
            units.add(new Rook(teamID+1, null));
            break;
          case BISHOP:
            units.add(new Bishop(teamID+1, null));
            break;
          case KNIGHT:
            units.add(new Knight(teamID+1, null));
            break;
          case PAWN:
            units.add(new Pawn(teamID+1, null));
            break;
          }
          
          currentMap.put(type, units);
        }
      }
    }
    
    //fill board matrix with squares
    for(int rank = 1; rank <= 8; rank++){
      for(char file = 'A'; file <= 'H'; file++){
        squares[rank - 1][file - 'A'] = new Square(file, rank, board);
      }
    }

    //place actual units on squares
    //Pawns for Team 1
    int pIndex = 0;
    for (Square rank2Sqaure : squares[1]) {
      Pawn pawn = (Pawn) teamArr[0].get(UnitType.PAWN).get(pIndex++);
      rank2Sqaure.placeUnit(pawn);
      
      pawn.updateSquare(rank2Sqaure);
    }

    //pawns for Team 2
    pIndex = 0;
    for (Square rank2Sqaure : squares[6]) {
      Pawn pawn = (Pawn) teamArr[1].get(UnitType.PAWN).get(pIndex++);
      rank2Sqaure.placeUnit(pawn);
      
      pawn.updateSquare(rank2Sqaure);
    }

    //place rooks
    squares[0][0].placeUnit(teamArr[0].get(UnitType.ROOK).get(0));
    teamArr[0].get(UnitType.ROOK).get(0).updateSquare(squares[0][0]);
    
    squares[0][7].placeUnit(teamArr[0].get(UnitType.ROOK).get(1));
    teamArr[0].get(UnitType.ROOK).get(1).updateSquare(squares[0][7]);
    
    squares[7][0].placeUnit(teamArr[1].get(UnitType.ROOK).get(0));
    teamArr[1].get(UnitType.ROOK).get(0).updateSquare(squares[7][0]);
    
    squares[7][7].placeUnit(teamArr[1].get(UnitType.ROOK).get(1));
    teamArr[1].get(UnitType.ROOK).get(1).updateSquare(squares[7][7]);
    

    //place knights
    squares[0][1].placeUnit(teamArr[0].get(UnitType.KNIGHT).get(0));
    teamArr[0].get(UnitType.KNIGHT).get(0).updateSquare(squares[0][1]);
    
    squares[0][6].placeUnit(teamArr[0].get(UnitType.KNIGHT).get(1));
    teamArr[0].get(UnitType.KNIGHT).get(1).updateSquare(squares[0][6]);
    
    squares[7][1].placeUnit(teamArr[1].get(UnitType.KNIGHT).get(0));
    teamArr[1].get(UnitType.KNIGHT).get(0).updateSquare(squares[7][1]);
    
    squares[7][6].placeUnit(teamArr[1].get(UnitType.KNIGHT).get(1));
    teamArr[1].get(UnitType.KNIGHT).get(1).updateSquare(squares[7][6]);
    

    //place bishops
    squares[0][2].placeUnit(teamArr[0].get(UnitType.BISHOP).get(0));
    teamArr[0].get(UnitType.BISHOP).get(0).updateSquare(squares[0][2]);
    
    squares[0][5].placeUnit(teamArr[0].get(UnitType.BISHOP).get(1));
    teamArr[0].get(UnitType.BISHOP).get(1).updateSquare(squares[0][5]);
    
    squares[7][2].placeUnit(teamArr[1].get(UnitType.BISHOP).get(0));
    teamArr[1].get(UnitType.BISHOP).get(0).updateSquare(squares[7][2]);
    
    squares[7][5].placeUnit(teamArr[1].get(UnitType.BISHOP).get(1));
    teamArr[1].get(UnitType.BISHOP).get(1).updateSquare(squares[7][5]);
    

    //place king
    squares[0][3].placeUnit(teamArr[0].get(UnitType.KING).get(0));
    teamArr[0].get(UnitType.KING).get(0).updateSquare(squares[0][3]);
    
    squares[7][3].placeUnit(teamArr[1].get(UnitType.KING).get(0));
    teamArr[1].get(UnitType.KING).get(0).updateSquare(squares[7][3]);

    //place queen
    squares[0][4].placeUnit(teamArr[0].get(UnitType.QUEEN).get(0));
    teamArr[0].get(UnitType.QUEEN).get(0).updateSquare(squares[0][4]);
    
    squares[7][4].placeUnit(teamArr[1].get(UnitType.QUEEN).get(0));
    teamArr[1].get(UnitType.QUEEN).get(0).updateSquare(squares[7][4]);
    
    return new TeamInformation(teamArr[0], teamArr[1]);
  }

}
