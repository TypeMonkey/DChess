package jg.proj.chess;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Stack;
import java.util.stream.Collectors;


public class Main {

  public static void main(String[] args) {
   ArrayList<String> test = new ArrayList<String>(Arrays.asList("yo", "bye", "what"));
   
   String x = test.stream().map(y->y+"1").collect(Collectors.joining("|"));
   System.out.println("|"+x+"|");
  }

}
