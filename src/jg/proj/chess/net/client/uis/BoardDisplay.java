package jg.proj.chess.net.client.uis;

public class BoardDisplay {
  
  private volatile String board;
  private volatile String warningLine;
  
  public BoardDisplay(String board, String warningLine) {
    this.board = board;
    this.warningLine = warningLine;
  }  

  public void setBoard(String board) {
    this.board = board;
  }

  public void setWarningLine(String warningLine) {
    this.warningLine = warningLine;
  }

  public String getBoard() {
    return board;
  }

  public String getWarningLine() {
    return warningLine;
  }
  
  public String toString() {
    String actualBoard = board.replace(" ", "&nbsp;");
    actualBoard = actualBoard.replace("~", "<br>");
    
    actualBoard += "-----------------------------------<br>";
    actualBoard += warningLine + "<br>";
    
    
    System.out.println("----> NEW DISPLAY!!! "+actualBoard);
    
    return "<html>"+actualBoard+"</html>";
  }
}
