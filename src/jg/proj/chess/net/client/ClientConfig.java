package jg.proj.chess.net.client;

import java.util.HashMap;
import java.util.Map;

public class ClientConfig {
  
  public enum ConfigKey{
    IP,
    PORT;
  }
  
  private final Map<ConfigKey, String> configs;
  
  public ClientConfig() {
    configs = new HashMap<>();
  }
  
  public void setValue(ConfigKey key, String value) {
    configs.put(key, value);
  }
  
  public String getValue(ConfigKey key){
    return configs.get(key);
  }
}
