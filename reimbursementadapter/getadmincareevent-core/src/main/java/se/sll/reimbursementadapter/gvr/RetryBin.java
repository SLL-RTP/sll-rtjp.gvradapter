package se.sll.reimbursementadapter.gvr;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import javax.xml.bind.JAXBException;
import javax.xml.datatype.DatatypeConfigurationException;
import javax.xml.datatype.DatatypeFactory;
import javax.xml.datatype.XMLGregorianCalendar;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.xml.sax.SAXException;

import se.sll.ersmo.xml.indata.ERSMOIndata;
import se.sll.ersmo.xml.indata.ERSMOIndata.Ersättningshändelse;
import se.sll.reimbursementadapter.gvr.transform.ERSMOIndataMarshaller;
import se.sll.reimbursementadapter.gvr.transform.TransformHelper;

/**
 * Component that keeps a list of files that triggered an error when processing,
 * but was deemed that the fault was fixable (i.e. updated code server definitions).
 * Used by the AbstractProducer to continually resend messages along with the new ones.
 */
@Component
public class RetryBin
{
    private static final Logger LOG = LoggerFactory.getLogger(RetryBin.class);
    
    /** Path to directory where the retry bin is stored. If empty the retry bin will be disabled. */
    @Value("${pr.gvr.io.retryBinDir:}")
    public String dir;

    /** The number of files to keep in the history. */
    @Value("${pr.gvr.io.retryBinFileKeepCount:240}")
    public int fileKeepCount;

    /** The age in days after which the files should be discarded from the bin. */
    @Value("${pr.gvr.io.discardOldDays:180}")
    public long discardOldDays;
    
    /** All Ersättningshändelser that came from the retry bin file (been there since last request). */
    public Map<String, Ersättningshändelse> old;
    
    /** All Ersättningshändelser that was added in this request. */
    public Map<String, Ersättningshändelse> nev;

    public File lastLoadedFile;
    

    public RetryBin() {
        old = new HashMap<String, Ersättningshändelse>();
        nev = new HashMap<String, Ersättningshändelse>();
        lastLoadedFile = null;
    }

    /**
     * Put ersh to collection of new but only update if it originates from newer file.
     *
     * @param ersh The {@link Ersättningshändelse} to insert into the bin.
     * @param fileUpdatedTimestamp The {@link Date} file timestamp for the ersh.
     * @throws DatatypeConfigurationException When the Date could not be converted to a Calendar.
     */
    public void put(Ersättningshändelse ersh, Date fileUpdatedTimestamp) throws DatatypeConfigurationException
    {
        if (disabled()) return;
         
        String id = ersh.getID();
        
        Ersättningshändelse existing = old.get(id);
        if (existing != null) { 
            if (fileUpdatedTimestamp.after(xmlCalToDate(existing.getLastUpdated()))) {
                old.remove(id);
                existing = null;
            }
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
     * Load the old {@link Ersättningshändelse} from the file system.
     *
     * @throws FileNotFoundException When the listed file could not be read from the disk.
     * @throws SAXException XML error when parsing file contents.
     * @throws JAXBException XML error when parsing file contents.
     */
    public void load() throws FileNotFoundException, SAXException, JAXBException
    {
        if (disabled()) return;

        nev.clear();
        old.clear();
        
        File[] files = new File(dir).listFiles();
        Arrays.sort(files);
        if (files.length > 0) {
            lastLoadedFile = files[files.length - 1];
            ERSMOIndata xml = (new ERSMOIndataMarshaller()).unmarshal(new FileReader(lastLoadedFile));
            for (Ersättningshändelse ersh : xml.getErsättningshändelse()) {
                old.put(ersh.getID(), ersh);
            }
            LOG.info(String.format("Loaded %d events from retry bin file %s.", old.size(), lastLoadedFile.getAbsolutePath()));
        }
        
    }

    /**
     * Add all from new collection to old collection and save all to file.
     *
     * @throws SAXException XML error when parsing file contents.
     * @throws JAXBException XML error when parsing file contents.
     * @throws IOException When the new files could not be saved to disk.
     */
    public void acceptNewAndSave() throws SAXException, JAXBException, IOException
    {
        if (disabled()) return;

        // Accept new.
        
        int newSize = nev.size();
        old.putAll(nev);
        nev.clear();
        
        // Find out new file name.
        
        File[] files = new File(dir).listFiles();
        Arrays.sort(files);
        
        File saveFile;
        
        if (files.length == 0) {
            saveFile = new File(dir, String.format("retry-bin-%09d.xml", 0));
        }
        else {
            int lastIndex = Integer.valueOf(files[files.length - 1].getName().replaceFirst(".*-(\\d+)\\.xml", "$1"));
            saveFile = new File(dir, String.format("retry-bin-%09d.xml", lastIndex + 1));
        }
        
        // Save it.
        
        ERSMOIndata xml = new ERSMOIndata();
        xml.getErsättningshändelse().addAll(old.values());
        xml.setKälla(TransformHelper.SLL_GVR_SOURCE);
        xml.setID("");
        (new ERSMOIndataMarshaller()).marshal(xml, new FileWriter(saveFile));

        LOG.info(String.format("Saved %d events to retry bin file %s, accepted %d new.", old.size(), saveFile.getAbsolutePath(), newSize));
        
        // Remove excess files.
        
        for (int i = 0; i < files.length - fileKeepCount + 1; ++i) {
            LOG.info(String.format("Removing excess retry bin file %s.", files[i].getAbsolutePath()));
            files[i].delete();
        }
    }

    /**
     * Return all {@link Ersättningshändelse} from the old bin who have a updated timestamp
     * before or equal to the incoming filterTimestamp parameter.
     *
     * @param filterTimestamp A {@link Date} the filter date.
     * @return {@link Ersättningshändelse} from the old bin who have a updated timestamp
     * before or equal to the incoming filterTimestamp parameter.
     */
    public List<Ersättningshändelse> getOld(Date filterTimestamp)
    {
        if (disabled()) new ArrayList<Ersättningshändelse>();

        GregorianCalendar cal = new GregorianCalendar();
        cal.setTime(filterTimestamp);
        
        ArrayList<Ersättningshändelse> list = new ArrayList<Ersättningshändelse>();
        for (Ersättningshändelse ersh : old.values()) {
           if (!ersh.getLastUpdated().toGregorianCalendar().after(cal)) {
               list.add(ersh);
           }
        }
        return list;
    }

    /**
     * Removes the {@link Ersättningshändelse} corresponding to the incoming 'id' parameter
     * from both the new and old bins.
     *
     * @param id The id for the {@link Ersättningshändelse} that should be removed.
     */
    public void remove(String id)
    {        
        nev.remove(id);
        old.remove(id);
    }

    /**
     * Returns a {@link java.nio.file.Path} to the file that was last loaded.
     *
     * @return a {@link java.nio.file.Path} to the file that was last loaded.
     */
    public Path getCurrentFile()
    {   
        if (lastLoadedFile == null) return null;
        
        return lastLoadedFile.toPath();
    }

    /**
     * Discard all old entries to prevent file from growing out of control.
     *
     * @param now The date that constitutes now and should be used to calculate the file age.
     */
    public void discardOld(Date now)
    {
        GregorianCalendar cal = new GregorianCalendar();
        Date date = new Date(now.getTime() - 1000L * 3600L * 24L * discardOldDays);
        cal.setTime(date);
        
        Iterator<Entry<String, Ersättningshändelse>> iterator = old.entrySet().iterator();
        int removeCount = 0;
        while (iterator.hasNext()) {
            Entry<String, Ersättningshändelse> entry = iterator.next();
            GregorianCalendar entryCal = entry.getValue().getLastUpdated().toGregorianCalendar();
            if (entryCal.before(cal)) {
                iterator.remove();
                ++removeCount;
            }
        }
        LOG.info(String.format("Discarded %d old entries from retry bin.", removeCount));
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
    
    private boolean disabled()
    {
        return dir == null || dir.trim().length() == 0;
    }

}
