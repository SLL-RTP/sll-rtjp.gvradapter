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
package se.sll.reimbursementadapter.app.listener;

import java.util.Arrays;
import java.util.List;

import javax.servlet.ServletContext;
import javax.servlet.ServletContextEvent;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.context.ContextLoaderListener;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.context.support.WebApplicationContextUtils;

import se.sll.reimbursementadapter.admincareevent.service.CodeServerMEKCacheManagerService;

/**
 * Initializes context when starting up Web application (see WEB-INF/web.xml).
 *  
 * @author Peter
 *
 */
public class ApplicationContextLoaderListener extends ContextLoaderListener {
    private static final Logger LOG = LoggerFactory.getLogger(ApplicationContextLoaderListener.class);

    
    @Override
    public void contextInitialized(ServletContextEvent event) {
        super.contextInitialized(event);
        try {
            final WebApplicationContext wc = getWebRequest(event.getServletContext());
            final CodeServerMEKCacheManagerService codeServerMekCacheService = wc.getBean(CodeServerMEKCacheManagerService.class);
            if (codeServerMekCacheService.getCurrentIndex() == null) {
                LOG.info("Index needs to be revalidated, takes some time please be patient");
                codeServerMekCacheService.revalidate();
            }
        } catch (Exception e) {
            LOG.error("Exception while initializing the WebApplicationContext.", e);
        }
        LOG.info("======== GVRAdapter Application :: Started ========");
    }

    @Override
    public void contextDestroyed(ServletContextEvent event) {
        super.contextDestroyed(event);
        LOG.info("======== GVRAdapter Application :: Stopped ========");
    }


    public static final WebApplicationContext getWebRequest(final ServletContext sc) {
        return WebApplicationContextUtils.getWebApplicationContext(sc);
    }

    public static List<String> getActiveProfiles(final ServletContext sc) {
        return Arrays.asList(getWebRequest(sc).getEnvironment().getActiveProfiles());
    }

    public static final boolean isProfileActive(final ServletContext sc, final String name) {
        final List<String> profiles = getActiveProfiles(sc);
        for (final String s : profiles) {
            if (s.equals(name)) {
                return true;
            }
        }

        return false;
    }
}
