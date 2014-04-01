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
package se.sll.reimbursementadapter.processreimbursement.rs;

import org.springframework.beans.factory.annotation.Autowired;
import se.sll.reimbursementadapter.processreimbursement.jmx.StatusBean;
import se.sll.reimbursementadapter.processreimbursement.service.CodeServerCacheManagerService;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Response;

/**
 * Admin service to trigger revalidation of index data.
 * 
 * @author Peter
 */
@Path("/")
public class AdminService {
    /** The code server cache instance. */
    @Autowired
    private CodeServerCacheManagerService codeServerCacheService;
    /** The Status Bean to use for reporting request status. */
    @Autowired
    private StatusBean statusBean;

    /**
     * Rebuilds the current code server index.
     * @return A {@link javax.ws.rs.core.Response} object with response information.
     */
    @GET
    @Produces("application/json")
    @Path("/revalidate-index")
    public Response rebuildIndex() {
        boolean success = false;
        statusBean.start("/revalidate-index");
        try {
            codeServerCacheService.revalidate();
            return Response.ok().build();
        } finally {
            statusBean.stop(success);
        }
    }
}
