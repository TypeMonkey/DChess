package jg.proj.chess.utils;

import java.util.HashSet;

/**
 * An extension of HashSet, but it passively doesn't allow null values to be
 * added to it. 
 * 
 * By "passive", add() doesn't throw an Exception and simply returns false when given a null argument
 * @author Jose
 *
 * @param <E> - element to hold
 */
public class NonNullHashSet<E> extends HashSet<E>{
  
  public boolean add(E e){
    if (e == null) {
      return false;
    }
    return super.add(e);
  }

  
}
