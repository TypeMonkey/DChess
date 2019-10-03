package jg.proj.chess.net;

public interface ServerRequests {
  
  public static final String UPDATE = "~update";
  public static final String CUSER = "~cuser:%s";
  public static final String VOTE = "~vote:%c%d>%c%d";
  public static final String PLIST = "~plist";
  public static final String QUIT = "~quit";
  
}
