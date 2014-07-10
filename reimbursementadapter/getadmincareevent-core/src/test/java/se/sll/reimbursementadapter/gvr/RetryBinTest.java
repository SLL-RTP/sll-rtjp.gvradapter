/**
 *  Copyright (c) 2013 SLL <http://sll.se/>
 *
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 */
package se.sll.reimbursementadapter.gvr;

import java.io.File;
import java.nio.file.Paths;
import java.util.Date;
import java.util.GregorianCalendar;

import javax.xml.datatype.DatatypeFactory;
import javax.xml.datatype.XMLGregorianCalendar;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import se.sll.ersmo.xml.indata.ERSMOIndata.Ersättningshändelse;
import se.sll.ersmo.xml.indata.ERSMOIndata.Ersättningshändelse.Händelseklass;
import se.sll.ersmo.xml.indata.ERSMOIndata.Ersättningshändelse.Patient;
import se.sll.ersmo.xml.indata.Vkhform;
import se.sll.ersmo.xml.indata.Vårdkontakt;

public class RetryBinTest 
{
    @Rule
    public TemporaryFolder tmp = new TemporaryFolder();
    public RetryBin retryBin;

    @Before
    public void setUp()
    {
        retryBin = new RetryBin();
        retryBin.dir = tmp.getRoot().getAbsolutePath();
        retryBin.fileKeepCount = 4;
    }

    @Test
    public void testEmptyLoad() throws Exception 
    {
        retryBin.load();
        
        Assert.assertEquals(0, retryBin.old.size());
        Assert.assertEquals(0, retryBin.nev.size());
    }
    
    @Test
    public void testSaveAndLoad() throws Exception 
    {        
        Date now = new Date();
        retryBin.put(createMinimalErsh("123", now ), now);
        retryBin.put(createMinimalErsh("124", now), now);
        retryBin.put(createMinimalErsh("125", now), now);

        Assert.assertEquals(0, retryBin.old.size());
        Assert.assertEquals(3, retryBin.nev.size());

        retryBin.acceptNewAndSave();
        
        Assert.assertEquals(3, retryBin.old.size());
        Assert.assertEquals(0, retryBin.nev.size());
        
        retryBin.load();
        
        Assert.assertEquals(3, retryBin.old.size());
        Assert.assertEquals(0, retryBin.nev.size());
    }

    @Test
    public void testAcceptAndSaveSavesToFirstNewFile() throws Exception 
    {
        Date now = new Date();
        Ersättningshändelse ersh = createMinimalErsh("123", now);
        retryBin.put(ersh, now);
        
        Assert.assertEquals(null, retryBin.old.get("123"));
        Assert.assertEquals(ersh, retryBin.nev.get("123"));
        
        retryBin.acceptNewAndSave();
        
        Assert.assertEquals(ersh, retryBin.old.get("123"));
        Assert.assertEquals(null, retryBin.nev.get("123"));
        
        Assert.assertTrue(Paths.get(retryBin.dir, "retry-bin-000000000.xml").toFile().isFile());
    }

    @Test
    public void testAcceptAndSaveSavesToNewFile() throws Exception 
    {
        new File(retryBin.dir, "retry-bin-000012300.xml").createNewFile();
        new File(retryBin.dir, "retry-bin-000023100.xml").createNewFile();
        
        retryBin.acceptNewAndSave();
        
        Assert.assertTrue(Paths.get(retryBin.dir, "retry-bin-000023100.xml").toFile().isFile());
    }

    @Test
    public void testAcceptAndSaveDeletesExcessFiles() throws Exception 
    {
        new File(retryBin.dir, "retry-bin-000012300.xml").createNewFile();
        new File(retryBin.dir, "retry-bin-000023100.xml").createNewFile();
        new File(retryBin.dir, "retry-bin-000023101.xml").createNewFile();
        new File(retryBin.dir, "retry-bin-000023102.xml").createNewFile();
        new File(retryBin.dir, "retry-bin-000023103.xml").createNewFile();
        new File(retryBin.dir, "retry-bin-000023104.xml").createNewFile();
        new File(retryBin.dir, "retry-bin-000023105.xml").createNewFile();
        
        retryBin.acceptNewAndSave();

        Assert.assertEquals(4, new File(retryBin.dir).listFiles().length);
        
        Assert.assertFalse(Paths.get(retryBin.dir, "retry-bin-000023102.xml").toFile().isFile());
        
        Assert.assertTrue(Paths.get(retryBin.dir, "retry-bin-000023103.xml").toFile().isFile());
        Assert.assertTrue(Paths.get(retryBin.dir, "retry-bin-000023104.xml").toFile().isFile());
        Assert.assertTrue(Paths.get(retryBin.dir, "retry-bin-000023105.xml").toFile().isFile());
        Assert.assertTrue(Paths.get(retryBin.dir, "retry-bin-000023106.xml").toFile().isFile());
    }

    @Test
    public void testPutInsertsInNewIfAllEmpty() throws Exception 
    {
        Ersättningshändelse ersh = new Ersättningshändelse();
        ersh.setID("123");
        retryBin.put(ersh , new Date());
        Assert.assertEquals(null, retryBin.old.get("123"));
        Assert.assertEquals(ersh, retryBin.nev.get("123"));
    }

    @Test
    public void testPutReplacesInNewIfOlderInOLd() throws Exception 
    {
        Date now = new Date();
        
        Ersättningshändelse ersh1 = new Ersättningshändelse();
        XMLGregorianCalendar xcal1 = RetryBin.dateToXmlCal(now);
        xcal1.add(DatatypeFactory.newInstance().newDuration(-1));
        ersh1.setID("123");
        ersh1.setLastUpdated(xcal1);
        retryBin.old.put("123", ersh1);
        
        Ersättningshändelse ersh2 = new Ersättningshändelse();
        ersh2.setID("123");
        retryBin.put(ersh2, now);
        Assert.assertEquals(null, retryBin.old.get("123"));
        Assert.assertEquals(ersh2, retryBin.nev.get("123"));
    }
   
    @Test
    public void testPutReplacesInNewIfOlderInNew() throws Exception 
    {
        Date now = new Date();
        
        Ersättningshändelse ersh1 = new Ersättningshändelse();
        XMLGregorianCalendar xcal1 = RetryBin.dateToXmlCal(now);
        xcal1.add(DatatypeFactory.newInstance().newDuration(-1));
        ersh1.setID("123");
        ersh1.setLastUpdated(xcal1);
        retryBin.nev.put("123", ersh1);
        
        Ersättningshändelse ersh2 = new Ersättningshändelse();
        ersh2.setID("123");
        retryBin.put(ersh2, now);
        
        Assert.assertEquals(null, retryBin.old.get("123"));
        Assert.assertEquals(ersh2, retryBin.nev.get("123"));
    }
   
    @Test
    public void testPutDoesNothingIfNewerInOld() throws Exception 
    {
        Date now = new Date();
        
        Ersättningshändelse ersh1 = new Ersättningshändelse();
        XMLGregorianCalendar xcal1 = RetryBin.dateToXmlCal(now);
        xcal1.add(DatatypeFactory.newInstance().newDuration(1));
        ersh1.setID("123");
        ersh1.setLastUpdated(xcal1);
        retryBin.old.put("123", ersh1);
        
        Ersättningshändelse ersh2 = new Ersättningshändelse();
        ersh2.setID("123");
        retryBin.put(ersh2, now);
        
        Assert.assertEquals(ersh1, retryBin.old.get("123"));
        Assert.assertEquals(null, retryBin.nev.get("123"));
    }
   
    @Test
    public void testPutDoesNothingIfNewerInNew() throws Exception 
    {
        Date now = new Date();
        
        Ersättningshändelse ersh1 = new Ersättningshändelse();
        XMLGregorianCalendar xcal1 = RetryBin.dateToXmlCal(now);
        xcal1.add(DatatypeFactory.newInstance().newDuration(1));
        ersh1.setID("123");
        ersh1.setLastUpdated(xcal1);
        retryBin.nev.put("123", ersh1);
        
        Ersättningshändelse ersh2 = new Ersättningshändelse();
        ersh2.setID("123");
        retryBin.put(ersh2, now);
        
        Assert.assertEquals(null, retryBin.old.get("123"));
        Assert.assertEquals(ersh1, retryBin.nev.get("123"));
    }
    
    private Ersättningshändelse createMinimalErsh(String id, Date date) throws Exception
    {
        Ersättningshändelse ersh = new Ersättningshändelse();
        ersh.setID(id);
        ersh.setLastUpdated(RetryBin.dateToXmlCal(date));
        ersh.setStartdatum(DatatypeFactory.newInstance().newXMLGregorianCalendar(new GregorianCalendar()));
        ersh.setStartverksamhet("12345678901");
        ersh.setSlutverksamhet("12345678901");
        ersh.setHändelseklass(new Händelseklass());
        ersh.getHändelseklass().setVårdkontakt(new Vårdkontakt());
        ersh.getHändelseklass().getVårdkontakt().setHändelseform(Vkhform.ÖPPENVÅRDSKONTAKT);
        ersh.setPatient(new Patient());
        ersh.getPatient().setID("191212121212");
        return ersh;
    }
}
