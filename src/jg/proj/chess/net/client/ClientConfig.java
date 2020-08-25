package jg.proj.chess.net.client;

import java.util.HashMap;
import java.util.Map;

/**
 * Stores configuration values for a DChess client
 * @author Jose
 */
public class ClientConfig {
  
  /**
   * The key associated with a configuration value
   * @author Jose
   *
   */
  public enum ConfigKey{
    /**
     * The IP Address of the server
     */
    IP,
    
    /**
     * The port the server is bound on
     */
    PORT;
  }
  
  private final Map<ConfigKey, String> configs;
  
  /**
   * Constructs a ClientConfig
   */
  public ClientConfig() {
    configs = new HashMap<>();
  }
  
  /**
   * Sets the value of a configuration
   * @param key - the Configuration key
   * @param value - the value to set this configuration to be
   */
  public void setValue(ConfigKey key, String value) {
    configs.put(key, value);
  }
  
  /**
   * Retrieves the value of a given configuration
   * @param key - the ConfigKey to lookup
   * @return the value of the configuration, or null if such configuration has not been set
   */
  public String getValue(ConfigKey key){
    return configs.get(key);
  }
}
