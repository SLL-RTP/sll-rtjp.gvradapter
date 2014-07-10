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
package se.sll.reimbursementadapter.getadmincareevent.gvr;

import java.util.Date;

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
        Date now = new Date();
        retryBin.put(ersh , now);
        
        Assert.assertEquals(null, retryBin.old.get("123"));
        Assert.assertEquals(ersh, retryBin.nev.get("123"));
    }
 
   
}
