package jg.proj.chess.net.client;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * Manages resources that are high-latency when it comes
 * to loading. This class allows for the cacheing
 * of such resources to reduce load times.
 * @author Jose
 *
 */
public class ResourceManager {

  /**
   * Associates a resource with a unique key
   * @author Jose
   *
   */
  public static class ResourceInfo{
    private final String key;
    private final String path;  
    
    private boolean isLoaded;
    private byte [] resource;
    
    public ResourceInfo(String key, String path) {
      this.key = key;
      this.path = path;
    }
    
    @Override
    public boolean equals(Object obj) {
      if (obj instanceof ResourceInfo) {
        ResourceInfo other = (ResourceInfo) obj;
        return other.key.equals(key);
      }
      return false;
    }
    
    @Override
    public int hashCode() {
      return key.hashCode();
    }
    
    public void setResource(byte[] resource) {
      this.resource = resource;
    }
    
    public void setLoaded(boolean isLoaded) {
      this.isLoaded = isLoaded;
    }
    
    public byte[] getResource() {
      return resource;
    }
    
    public String getPath() {
      return path;
    }
    
    public String getKey() {
      return key;
    }
    
    public boolean isLoaded() {
      return isLoaded;
    }
    
    @Override
    public String toString() {
      return "RESOURCE: key="+key+" , "+path;
    }
  }
  
  private final Map<String, ResourceInfo> resourceMap;
  
  public ResourceManager(Set<ResourceInfo> resourceInfos) {
    this.resourceMap = new HashMap<>();
    
    //add resourceInfos to map
    for (ResourceInfo resourceInfo : resourceInfos) {
      resourceMap.put(resourceInfo.key, resourceInfo);
    }
  }
  
  public boolean addResource(ResourceInfo resource) {
    if (containsResource(resource.key)) {
      return false;
    }
    resourceMap.put(resource.key, resource);
    return true;
  }
  
  public boolean addResource(String key, String path) {
    return addResource(new ResourceInfo(key, path));
  }
  
  public boolean removeResource(String key) {
    return resourceMap.remove(key) != null;
  }
  
  public boolean containsResource(String key) {
    return resourceMap.containsKey(key);
  }
  
  public void loadAllResources() throws IOException {
    for (ResourceInfo resource : resourceMap.values()) {
      File file = new File(resource.path);
      
      byte [] bytes = Files.readAllBytes(file.toPath());
      resource.setResource(bytes);
      resource.setLoaded(true); 
    }
  }
  
  public byte [] getResource(String key) throws IOException{
    ResourceInfo resourceInfo = resourceMap.get(key);
    if (resourceInfo != null) {
      File file = new File(resourceInfo.path);
      
      byte [] bytes = Files.readAllBytes(file.toPath());
      resourceInfo.setResource(bytes);
      resourceInfo.setLoaded(true);
      
      return bytes;
    }
    return null;
  }
  
}
