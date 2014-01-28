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
package se.sll.gvradapter;

import java.util.GregorianCalendar;

import org.apache.cxf.jaxws.JaxWsProxyFactoryBean;

import javax.xml.datatype.DatatypeConfigurationException;
import javax.xml.datatype.DatatypeFactory;
import javax.xml.datatype.XMLGregorianCalendar;

public abstract class AbstractTestConsumer<T> {

    public static final String SAMPLE_ORIGINAL_CONSUMER_HSAID = "sample-original-consumer-hsaid";

    protected T _service = null;

    private Class<T> _serviceType;

    /**
     * Constructs a test consumer with a web service proxy setup for communication using HTTPS with Mutual Authentication
     *
     * @param serviceType, required to be able to get the generic class at runtime, see http://stackoverflow.com/questions/3403909/get-generic-type-of-class-at-runtime
     * @param serviceAddress
     */
    public AbstractTestConsumer(Class<T> serviceType, String serviceAddress) {

        _serviceType = serviceType;

        JaxWsProxyFactoryBean proxyFactory = new JaxWsProxyFactoryBean();
        proxyFactory.setServiceClass(getServiceType());
        proxyFactory.setAddress(serviceAddress);

        // Used for HTTPS
        /*
            SpringBusFactory bf = new SpringBusFactory();
            URL cxfConfig = this.getClass().getClassLoader().getResource("agp-cxf-test-consumer-config.xml");
            if (cxfConfig != null) {
                    proxyFactory.setBus(bf.createBus(cxfConfig));
            }
         */

        _service = proxyFactory.create(getServiceType());
    }

    protected Class<T> getServiceType() {
        return _serviceType;
    }
    
    protected XMLGregorianCalendar now() {
        try {
            return DatatypeFactory.newInstance().newXMLGregorianCalendar(new GregorianCalendar());
        } catch (DatatypeConfigurationException e) {
            throw new RuntimeException(e);
        }
    }
    
}