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
    String actualBoard = board.replace("~", "<br>");
    actualBoard = actualBoard.replace(" ", "&nbsp;");
    
    actualBoard += "-----------------------------------<br>";
    actualBoard += warningLine + "<br>";
    
    return "<html>"+actualBoard+"</html>";
  }
}
