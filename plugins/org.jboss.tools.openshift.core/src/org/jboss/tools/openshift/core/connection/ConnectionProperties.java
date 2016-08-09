/*******************************************************************************
 * Copyright (c) 2015 Red Hat, Inc.
 * Distributed under license by Red Hat, Inc. All rights reserved.
 * This program is made available under the terms of the
 * Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Red Hat, Inc. - initial API and implementation
 ******************************************************************************/
package org.jboss.tools.openshift.core.connection;

/**
 * Property IDs for handling connection change events
 * 
 * @author jeff.cantrill
 *
 */
public interface ConnectionProperties {
	
	static final String PROPERTY_RESOURCE = "openshift.resource";
	static final String PROPERTY_PROJECTS = "openshift.projects";
	static final String PROPERTY_REFRESH = "openshift.resource.refresh";
	
	static final int MAX_REQUESTS = 1000;
}
