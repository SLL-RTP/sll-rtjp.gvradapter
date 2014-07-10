package se.sll.reimbursementadapter.gvr;

import java.math.BigDecimal;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.xml.datatype.DatatypeConfigurationException;
import javax.xml.datatype.DatatypeFactory;
import javax.xml.datatype.Duration;
import javax.xml.datatype.XMLGregorianCalendar;
import javax.xml.datatype.DatatypeConstants.Field;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import se.sll.ersmo.xml.indata.ERSMOIndata.Ersättningshändelse;

@Component
public class RetryBin
{
    
    /** 
     * Path to directory where the retry bin is stored. If empty the retry bin will be disabled.
     */
    @Value("${pr.gvr.io.retryBinDir:}")
    public String dir;

    /**
     * All Ersättningshändelser that came from the retry bin file (been there since last request).
     */
    public Map<String, Ersättningshändelse> old;
    
    /**
     * All Ersättningshändelser that was added in this request.
     */
    public Map<String, Ersättningshändelse> nev;


    public RetryBin() {
        old = new HashMap<String, Ersättningshändelse>();
        nev = new HashMap<String, Ersättningshändelse>();
    }
    
    /**
     * Put ersh to collection of new but only update if it originates from newer file.
     */
    public void put(Ersättningshändelse ersh, Date fileUpdatedTimestamp) throws DatatypeConfigurationException
    {
        String id = ersh.getID();
        
        Ersättningshändelse existing = old.get(id);
        if (existing != null && fileUpdatedTimestamp.after(xmlCalToDate(existing.getLastUpdated()))) {
            old.remove(id); 
            existing = null;
        }
        else {
            existing = nev.get(id);
            if  (existing != null && fileUpdatedTimestamp.after(xmlCalToDate(existing.getLastUpdated()))) {
                nev.remove(id);
                existing = null;
            }
        }
        
        if (existing == null) {
            // Adding one millisecond to the fileUpdatedTimestamp before setting it on the ersh. This later timestamp will make the care event seem updated
            // in clients. This is kind of a hack and not very nice.
            //
            // Ersättningshändelse has no clone, but I think this should be fine. The danger is multiple additions to a single ersh at different times.
            XMLGregorianCalendar xcal = dateToXmlCal(fileUpdatedTimestamp);
            xcal.add(DatatypeFactory.newInstance().newDuration(1));
            ersh.setLastUpdated(xcal);
            nev.put(id, ersh);
        }
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
        System.out.println("HEJ " + dir);
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
    
    
    public static Date xmlCalToDate(XMLGregorianCalendar xcal) throws DatatypeConfigurationException
    {
        GregorianCalendar cal = xcal.toGregorianCalendar();
        return cal.getTime();
    }

    public static XMLGregorianCalendar dateToXmlCal(Date date) throws DatatypeConfigurationException
    {
        XMLGregorianCalendar xmlGregorianCalendar;
        GregorianCalendar c = new GregorianCalendar();
        c.setTime(date);
        xmlGregorianCalendar = DatatypeFactory.newInstance().newXMLGregorianCalendar(c);
        return xmlGregorianCalendar;
    }
    
}
