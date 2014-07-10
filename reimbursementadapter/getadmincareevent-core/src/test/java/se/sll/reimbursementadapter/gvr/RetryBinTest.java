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

import java.util.Date;

import javax.xml.datatype.DatatypeFactory;
import javax.xml.datatype.XMLGregorianCalendar;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import se.sll.ersmo.xml.indata.ERSMOIndata.Ersättningshändelse;
import se.sll.reimbursementadapter.gvr.RetryBin;

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
   
}
