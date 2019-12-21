package jg.proj.chess.net;

/**
 * A small set of types used in arguments for ServerRequests
 * @author Jose
 */
public enum ArgType {
  STRING,  //Will be parsed/consumed as a String (format specifier "%s")
  CHAR,    //Will be parsed/consumed as a character (format specifier "%c")
  BOOLEAN, //Will be parsed/consumed as a boolean (format specifier "%b")
  INTEGER, //Will be parsed/consumed as an int (format specifier "%d")
  FLOAT;   //Will be parsed/consumed as a float (format specifier "%f")
  
  /**
   * Checks if the targetType is, value-wise, equivalent to the expected type
   * @param targetType - the actual type of the value
   * @param expected - the expected type of the value
   * @return true if both types are value-wise equivalent, false if else
   */
  public static boolean matches(Class<?> targetType, ArgType expected) {
    switch (expected) {
      case STRING:
        return targetType.equals(String.class);
      case BOOLEAN:
        return targetType.equals(boolean.class) || 
               targetType.equals(Boolean.class) || 
               targetType.equals(Boolean.TYPE);
      case INTEGER:
        return targetType.equals(int.class) || 
            targetType.equals(Integer.class) || 
            targetType.equals(Integer.TYPE);
      case CHAR:
        return targetType.equals(char.class) || 
            targetType.equals(Character.class) || 
            targetType.equals(Character.TYPE);
      case FLOAT:
        return targetType.equals(double.class) || 
            targetType.equals(Double.class) || 
            targetType.equals(Double.TYPE);
      default:
        return false;
    }
  }
  
  /**
   * Returns the string representations of the given object array
   * @param args - the Object arguments
   * @return the String representations of the arguments
   */
  public static String[] getStringRep(Object ... args) {
    String [] strArgs = new String[args.length];
    for (int i = 0; i < args.length; i++) {
      strArgs[i] = args[i].toString();
    }
    return strArgs;
  }
  
  /**
   * Converts an array of string arguments to their respective expected types
   * 
   * Conversion rules: When parsing a string that's expected to be an Integer or Double
   *                   fails parsing, null is returned immediately to signal error
   *                   
   *                   When parsing a string that's expected to be a Character,
   *                   the string's first character is seen as the converted value
   * 
   * @param args - the String arguments to convert
   * @param targetTypes - the expected ArgTypes 
   * @return the converted arguments, or null to signal error in conversion or args.length != targetTypes.length
   */
  public static Object[] convertStringArgs(String [] args, ArgType [] targetTypes) {
    if (args.length == targetTypes.length) {
      Object [] converted = new Object[args.length];
      for(int i = 0; i < args.length ; i++) {
        if (targetTypes[i] != STRING) {
          if (targetTypes[i] == CHAR) {
            converted[i] = args[i].toCharArray()[0];
          }
          else if (targetTypes[i] == BOOLEAN) {
            if (args[i].equalsIgnoreCase("true") || args[i].equalsIgnoreCase("false")) {
              converted[i] = Boolean.parseBoolean(args[i]);
            }
            else {
              return null;
            }
          }
          else if (targetTypes[i] == INTEGER) {
            try {
              converted[i] = Integer.parseInt(args[i]);
            } catch (NumberFormatException e) {
              return null;
            }
          }
          else if (targetTypes[i] == FLOAT) {
            try {
              converted[i] = Double.parseDouble(args[i]);
            } catch (NumberFormatException e) {
              return null;
            }
          }
        }
        else {
          converted[i] = args[i];
        }
      }
      return converted;
    }
    else {
      return null;
    }
  }
}
