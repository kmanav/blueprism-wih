/*
 * Copyright 2018 Red Hat, Inc. and/or its affiliates.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.redhat.demo;

import java.util.concurrent.ConcurrentHashMap;
import java.util.List;
import java.util.Map;

import javax.xml.namespace.QName;

import org.apache.cxf.configuration.security.AuthorizationPolicy;
import org.apache.cxf.endpoint.Client;
import org.apache.cxf.transport.http.HTTPConduit;
import org.apache.cxf.transports.http.configuration.HTTPClientPolicy;
import org.drools.core.process.instance.impl.WorkItemImpl;
import org.jbpm.bpmn2.core.Bpmn2Import;
import org.jbpm.process.workitem.core.AbstractLogOrThrowWorkItemHandler;
import org.jbpm.process.workitem.core.util.RequiredParameterValidator;
import org.kie.api.runtime.KieSession;
import org.kie.api.runtime.process.WorkItem;
import org.kie.api.runtime.process.WorkItemManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.jbpm.process.workitem.core.util.Wid;
import org.jbpm.process.workitem.core.util.WidParameter;
import org.jbpm.process.workitem.core.util.WidResult;
import org.jbpm.process.workitem.core.util.service.WidAction;
import org.jbpm.process.workitem.core.util.service.WidAuth;
import org.jbpm.process.workitem.core.util.service.WidService;
import org.jbpm.process.workitem.webservice.WebServiceWorkItemHandler;
import org.jbpm.workflow.core.impl.WorkflowProcessImpl;
import org.jbpm.process.workitem.core.util.WidMavenDepends;

import org.apache.cxf.service.model.BindingOperationInfo;
import java.util.Collection;

/**
 * Extension of WebServiceWorkItemHandler, a Custom WIH that can exchange POJOs
 * as input/output parameters with a SOAP WebService in a SYNC way.
 * 
 * When working with an element like:
 * 
 * <s:element name="ShipOrderResponse"> 
 * 	<s:complexType> 
 * 		<s:sequence>
 * 			<s:element name="ShippingId" type="s:string"/>
 * 			<s:element name="ShippingComment" type="s:string"/> 
 * 		</s:sequence>
 * 	</s:complexType> 
 * </s:element>
 * 
 * It may be expected by the client the automatic marshalling/unmarshalling to a
 * POJO (In this case, ShipOrderResponse with two attributes). By default CXF
 * cast this to String and returns two simple objects. If using a POJO as
 * parameter in WebServiceWorkItemHandler, this results in a ClassCastException
 * thrown in:
 * https://github.com/apache/cxf/blob/ab3df13b8d15aa36365eb2e10b5961d17330d9a2/core/src/main/java/org/apache/cxf/endpoint/ClientImpl.java#L646
 * 
 * @author kvarela
 */
public class BluePrismWSWorkItemHandler extends WebServiceWorkItemHandler {

	private static Logger logger = LoggerFactory.getLogger(BluePrismWSWorkItemHandler.class);

	public BluePrismWSWorkItemHandler(KieSession ksession, ClassLoader classLoader, String username, String password) {
		super(ksession, classLoader, username, password);
	}
	
	public BluePrismWSWorkItemHandler(KieSession ksession, ClassLoader classLoader, int timeout, String username, String password) {
	    super(ksession,timeout, username,password);
	    super.setClassLoader(classLoader);
	}

	protected Client getWSClient(WorkItem workItem, String interfaceRef) {
		logger.debug("Invoking  overriden method getWSClient for interface: "+interfaceRef);

		Client client = super.getWSClient(workItem, interfaceRef);

		String operationRef = (String) workItem.getParameter("Operation");

		Collection<BindingOperationInfo> operations = client.getEndpoint().getBinding().getBindingInfo()
				.getOperations();

		for (BindingOperationInfo nextOperation : operations) {
			System.out.println("nextOperation = " + nextOperation);

			if (nextOperation.getName().getLocalPart().equals(operationRef)) {
				logger.debug("Setting wrapped operation for operationRef: "+nextOperation.getName());

				// Avoiding ClassCastException with complex object as param.
				// Setting "UnwrappedOperation" to null so CXF properly marshall/unmarshall the
				// parameters for this specific Operation.

				nextOperation.getOperationInfo().setUnwrappedOperation(null);
			}
		}

		return client;
	}

}
