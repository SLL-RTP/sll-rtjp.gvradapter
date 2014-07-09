package se.sll.reimbursementadapter;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import se.sll.ersmo.xml.indata.ERSMOIndata.Ersättningshändelse;

public class RetryBin
{

    /**
     * Remove all in erhsList from retry bin (both from new and old collections).
     */
    public void removeAll(List<Ersättningshändelse> ershList)
    {
        // TODO Auto-generated method stub
        
    }

    /**
     * Discard all old (compared to fileUpdateTime entries) in the old collection.
     */
    public void discardExpired(Date fileUpdatedTime)
    {
        // TODO Auto-generated method stub
        
    }

    /**
     * Load old ersh from file if not already loaded.
     */
    public void load()
    {
        // TODO Auto-generated method stub
    }

    /**
     * Put ersh to collection of new but only update if it originates from newer file.
     */
    public void put(Ersättningshändelse ersh, Date fileUpdatedTime)
    {
        // TODO Auto-generated method stub
    }

    /**
     * Add all from new collection to old collection and save all to file.
     */
    public void acceptNewAndSave()
    {
        // TODO Auto-generated method stub
    }

    public List<Ersättningshändelse> getOld(Date fileUpdatedTime)
    {
        return new ArrayList<Ersättningshändelse>();
        // TODO Auto-generated method stub
    }

    public void remove(Ersättningshändelse ersh)
    {
        // TODO Auto-generated method stub
    }

    public Path getCurrentFile()
    {
        // TODO Auto-generated method stub
        return Paths.get("");
    }

    public void discardOld(Date fileUpdatedTime)
    {
        // TODO Auto-generated method stub
        
    }
    
}
