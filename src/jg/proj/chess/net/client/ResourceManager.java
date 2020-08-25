package jg.proj.chess.net.client;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * Manages resources that are high-latency when it comes
 * to loading. This class allows for the caching
 * of such resources to reduce load times.
 * @author Jose
 *
 */
public class ResourceManager {

  /**
   * Represents a resource that
   * can be identified with a unique key.
   * 
   * Two ResourceInfos are considered the same if they have
   * the same key
   * 
   * @author Jose
   */
  public static class ResourceInfo{
    private final String key;
    private final String path;  
    
    private boolean isLoaded;
    private byte [] resource;
    
    /**
     * Constructs a ResourceInfo
     * @param key - the key to associate with this resource
     * @param path - the path to the resource
     */
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
    
    /**
     * Assigns the actual resource content to this ResourceInfo
     * @param resource - the byte array containing the resource
     */
    private void setResource(byte[] resource) {
      this.resource = resource;
    }
    
    /**
     * Sets whether this resource has been loaded
     * @param isLoaded - true if the resource has been loaded. False if else.
     */
    private void setLoaded(boolean isLoaded) {
      this.isLoaded = isLoaded;
    }
    
    /**
     * Retrieves the byte array containing the resource
     * @return the byte array containing the resource
     */
    public byte[] getResource() {
      return resource;
    }
    
    /**
     * Retrieves the path to the resource
     * @return the path to the resource
     */
    public String getPath() {
      return path;
    }
    
    /**
     * Retrieves the key associated with the resource
     * @return the key associated with the resource
     */
    public String getKey() {
      return key;
    }
    
    /**
     * Retrieves the loaded status of the resource
     * @return the loaded status of the resource
     */
    public boolean isLoaded() {
      return isLoaded;
    }
    
    @Override
    public String toString() {
      return "RESOURCE: key="+key+" , "+path;
    }
  }
  
  private final Map<String, ResourceInfo> resourceMap;
  
  /**
   * Constructs a ResourceManager
   * @param resourceInfos - the set of resources this ResourceManager will manage
   */
  public ResourceManager(Set<ResourceInfo> resourceInfos) {
    this.resourceMap = new HashMap<>();
    
    //add resourceInfos to map
    for (ResourceInfo resourceInfo : resourceInfos) {
      resourceMap.put(resourceInfo.key, resourceInfo);
    }
  }
  
  /**
   * Adds a resource to this ResourceManager
   * @param resource - the resource to be managed
   * @return false if a resource with the same key already exists, true if else.
   */
  public boolean addResource(ResourceInfo resource) {
    if (containsResource(resource.key)) {
      return false;
    }
    resourceMap.put(resource.key, resource);
    return true;
  }
  
  /**
   * Adds a resource to this ResourceManager
   * @param key - the unique key to associate with this resource
   * @param path - the path to this resource
   * @return false if a resource with the same key already exists, true if else.
   */
  public boolean addResource(String key, String path) {
    return addResource(new ResourceInfo(key, path));
  }
  
  /**
   * Removes a resource
   * @param key - the unique key associated with the resource
   * @return true if a resource with the given exists is managed by this ResourceManager and has been removed,
   *         false if else
   */
  public boolean removeResource(String key) {
    return resourceMap.remove(key) != null;
  }
  
  /**
   * Checks if a resource with the given key is managed by
   * this ResourceManager
   * @param key - the unique key associated with the resource 
   * @return false if no such resource exists, true if else
   */
  public boolean containsResource(String key) {
    return resourceMap.containsKey(key);
  }
  
  /**
   * Loads all resources managed by this ResourceManager
   * 
   * @throws IOException - an IO exception occurs while reading in a resource
   */
  public void loadAllResources() throws IOException {
    for (ResourceInfo resource : resourceMap.values()) {
      if (!resource.isLoaded()) {
        File file = new File(resource.path);
        
        byte [] bytes = Files.readAllBytes(file.toPath());
        resource.setResource(bytes);
        resource.setLoaded(true); 
      }
    }
  }
  
  /**
   * Retrieves a resource as an InputStream.
   * 
   * If the resource has not been loaded, it will be loaded during the
   * invocation of this method. However, future calls to this method
   * for said resource will not reload said resource. Instead,
   * it will return the cached resource.
   * 
   * Note: The InputStream returned by each invocation of this method
   *       is independent of each other.
   * 
   * @param key - the unique associated with the resource
   * @return an InputStream containing the resource, or null if no resource
   *         exists with the given key
   * @throws IOException - if the resource has not yet been loaded and an
   *                       IO error occurs while loading said resource
   */
  public InputStream getResourceAsStream(String key) throws IOException{
    byte [] data = getResource(key);
    if (data != null) {
      return new ByteArrayInputStream(data);
    }
    return null;
  }
  
  /**
   * Retrieves a resource as a byte array
   * 
   * If the resource has not been loaded, it will be loaded during the
   * invocation of this method. However, future calls to this method
   * for said resource will not reload said resource. Instead,
   * it will return the cached resource.
   * 
   * @param key - the unique associated with the resource
   * @return the byte array containing the resource
   * @throws IOException - if the resource has not yet been loaded and an
   *                       IO error occurs while loading said resource
   */
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
