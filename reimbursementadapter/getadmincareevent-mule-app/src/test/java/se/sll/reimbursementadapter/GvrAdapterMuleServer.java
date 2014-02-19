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
package se.sll.reimbursementadapter;

 
import org.mockftpserver.stub.StubFtpServer;
import org.soitoolkit.commons.mule.test.StandaloneMuleServer;
import org.soitoolkit.commons.mule.util.RecursiveResourceBundle;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class GvrAdapterMuleServer {


	public static final String MULE_SERVER_ID   = "codeserveradapter";
 

	private static final Logger logger = LoggerFactory.getLogger(GvrAdapterMuleServer.class);
    private static final RecursiveResourceBundle rb = new RecursiveResourceBundle("codeserveradapter-config");

	public static void main(String[] args) throws Exception {
	
 
	    StubFtpServer stubFtpServer = new StubFtpServer();
	    
	    stubFtpServer.setServerControlPort(1221);
        //
        stubFtpServer.start();
       // Configure the mule-server:
        //
        // Arg #1: The name of the Mule Server
        //
        // Arg #2: Start teststub-services if true
        //         Note: Actually enables the spring-beans-profile "soitoolkit-teststubs" in the file "src/main/app/codeserveradapter-common.xml"
        //
        // Arg #3: Start services if true 
        //         Note: Actually loads all *-service.xml files that are specified in the file "src/main/app/mule-deploy.properties"
        //
        StandaloneMuleServer muleServer = new StandaloneMuleServer(MULE_SERVER_ID, true, true);
         
        // Start the server
		muleServer.run();
	}

    /**
     * Address based on usage of the servlet-transport and a config-property for the URI-part
     * 
     * @param serviceUrlPropertyName
     * @return
     */
    public static String getAddress(String serviceUrlPropertyName) {

        String url = rb.getString(serviceUrlPropertyName);

	    logger.info("URL: {}", url);
    	return url;
 
    }	
}