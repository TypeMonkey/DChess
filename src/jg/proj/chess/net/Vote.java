package jg.proj.chess.net;

public class Vote {

  private final Player voter;
  
  private final char fileOrigin; 
  private final int rankOrigin;
  
  private final char fileDest; 
  private final int rankDest;
  
  public Vote(char fileOrigin, int rankOrigin, char fileDest, int rankDest, Player voter) {
    this.fileOrigin = fileOrigin;
    this.rankOrigin = rankOrigin;
    
    this.fileDest = fileDest;
    this.rankDest = rankDest;
    
    this.voter = voter;
  }
  
  @Override
  public int hashCode() {
    return toString().hashCode();
  }
  
  @Override
  public boolean equals(Object obj) {
    if (obj instanceof Vote) {
      Vote vote = (Vote) obj;
      return vote.toString().equals(toString());
    }
    return false;
  }
  
  @Override
  public String toString() {
    return String.valueOf(fileOrigin)+rankOrigin+">"+String.valueOf(fileDest)+rankDest;
  }

  public char getFileOrigin() {
    return fileOrigin;
  }

  public int getRankOrigin() {
    return rankOrigin;
  }

  public char getFileDest() {
    return fileDest;
  }

  public int getRankDest() {
    return rankDest;
  } 
  
  public Player getVoter(){
    return voter;
  }
  
  public static Vote parseVote(String message, Player voter){
    String [] split = message.split(">");
    if (split.length == 2) {
      String origin = split[0];
      String dest = split[1];
      
      if (origin.length() == 2 && dest.length() == 2) {
        char fileOrigin;
        int rankOrigin; 
        
        char fileDest; 
        int rankDest;
        
        fileOrigin = origin.charAt(0);
        rankOrigin = Integer.parseInt(String.valueOf(origin.charAt(1)));
        
        fileDest = dest.charAt(0);
        rankDest = Integer.parseInt(String.valueOf(dest.charAt(1)));
        
        return new Vote(fileOrigin, rankOrigin, fileDest, rankDest, voter);
      }      
    }
    
    System.out.println("INVALID VOTE!!!: "+message);
    return null;
  }
}
