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
package se.sll.reimbursementadapter.mule;

import org.apache.commons.net.ftp.FTPClient;
import org.apache.commons.net.ftp.FTPFile;
import org.mule.api.MuleMessage;
import org.mule.api.construct.FlowConstruct;
import org.mule.api.endpoint.InboundEndpoint;
import org.mule.api.lifecycle.CreateException;
import org.mule.api.transport.Connector;
import org.mule.transport.ftp.FtpMessageReceiver;

import java.io.IOException;
import java.text.MessageFormat;

/**
 * Makes sure remote files are not deleted. <p>
 * 
 * The default receiver deletes remote files upon successful completion. 
 * The remote file deletion part has been removed 
 * in the overridden {@link #postProcess(org.apache.commons.net.ftp.FTPClient, org.apache.commons.net.ftp.FTPFile, org.mule.api.MuleMessage)} method, otherwise
 * it's kept as is.
 * 
 * @author Peter
 */
public class NonDeletingFtpMessageReceiver extends FtpMessageReceiver {

    public NonDeletingFtpMessageReceiver(Connector connector, FlowConstruct flowConstruct, InboundEndpoint endpoint, long frequency)
            throws CreateException {
        super(connector, flowConstruct, endpoint, frequency);
    }

    @Override
    protected void postProcess(FTPClient client, FTPFile file, MuleMessage message) throws Exception {
        if (connector.isStreaming()) {
            if (!client.completePendingCommand()) {
                throw new IOException(MessageFormat.format("Failed to complete a pending command. Retrieveing file {0}. Ftp error: {1}",
                        file.getName(), client.getReplyCode()));
            }
        }
    }
}
