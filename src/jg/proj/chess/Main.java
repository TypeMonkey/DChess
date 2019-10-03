package jg.proj.chess;

import jg.proj.chess.net.ServerRequests;

public class Main {

  public static void main(String[] args) {
    System.out.println(String.format(ServerRequests.VOTE, 'a', 10, 'c', 7));
    System.out.println("------");
  }

}
