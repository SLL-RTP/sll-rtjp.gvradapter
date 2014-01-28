package se.sll.gvradapter.admincareevent.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class GVRFileService {
	
    @Value("${pr.gvr.localPath:/tmp/gvr/in}")
    private String gvrDirectory;
    
    @Value("${pr.gvr.timestampFormat:yyyyMMddHHmmss}")
    private String gvrTimestampFormat;
    
    private static GVRFileService instance;
    
    public GVRFileService() {
        //log.debug("constructor");
        if (instance == null) {
            instance = this;
        }
    }
    
    /**
     * Returns the singleton instance. <p>
     * 
     * Note: This is a work-around, since the parent mule-app doesn't use spring annotations
     * as configuration mechanism.
     * 
     * @return the singleton instance, or null if none has been created.
     */
    public static GVRFileService getInstance() {
        return instance;
    }
    
    public String getGVRDirectory() {
    	return this.gvrDirectory;
    }
    
    public String getGVRTimestampFormat() {
    	return this.gvrTimestampFormat;
    }
}
