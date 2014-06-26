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

import org.mule.api.context.notification.MuleContextNotificationListener;
import org.mule.context.notification.MuleContextNotification;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
//import se.sll.reimbursementadapter.admincareevent.service.CodeServerCacheManagerService;

/**
 * Ensures an index is loaded upon startup of application.
 * 
 * @author Peter
 *
 */
public class AppContextNotificationListener implements MuleContextNotificationListener<MuleContextNotification> {
    private final static Logger log = LoggerFactory.getLogger(AppContextNotificationListener.class);
    

    @Override
    public void onNotification(MuleContextNotification notification) {
        if (notification.getAction() == MuleContextNotification.CONTEXT_STARTED) {
            log.debug("Context started.");
            //if (CodeServerCacheManagerService.getInstance().getCurrentIndex() == null) {
            //    CodeServerCacheManagerService.getInstance().revalidate();
            //}
        }
    }
}
