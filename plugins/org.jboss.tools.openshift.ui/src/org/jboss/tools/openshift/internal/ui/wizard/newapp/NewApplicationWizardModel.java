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
package org.jboss.tools.openshift.internal.ui.wizard.newapp;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.commons.lang.ObjectUtils;
import org.jboss.tools.openshift.core.connection.Connection;
import org.jboss.tools.openshift.internal.ui.treeitem.ObservableTreeItem;
import org.jboss.tools.openshift.internal.ui.wizard.common.ResourceLabelsPageModel;

import com.openshift.restclient.IResourceFactory;
import com.openshift.restclient.OpenShiftException;
import com.openshift.restclient.ResourceFactoryException;
import com.openshift.restclient.model.IProject;
import com.openshift.restclient.model.IResource;
import com.openshift.restclient.model.template.IParameter;
import com.openshift.restclient.model.template.ITemplate;

/**
 * The New application wizard model that supports instantiation of
 * a template
 *
 * @author jeff.cantrill
 * @author Andre Dietisheim
 *
 */
public class NewApplicationWizardModel 
		extends ResourceLabelsPageModel 
		implements IResourceDetailsModel, ITemplateListPageModel, ITemplateParametersPageModel{

	private Connection connection;
	private IProject project;
	private List<ObservableTreeItem> projectItems = new ArrayList<>();
	private List<ObservableTreeItem> projectTemplates = new ArrayList<>();
	private ITemplate template;
	private ITemplate uploadedTemplate;
	private List<IParameter> parameters = new ArrayList<IParameter>();
	private IParameter selectedParameter;
	private HashMap<String, String> originalValueMap;
	private Collection<IResource> items = new ArrayList<IResource>(); 
	private boolean uploadTemplate = true;
	private String templateFilename;
	private IResourceFactory resourceFactory;

	private void update(boolean useUploadTemplate, IProject selectedProject, List<ObservableTreeItem> projectItems, ITemplate selectedTemplate) {
		firePropertyChange(PROPERTY_USE_UPLOAD_TEMPLATE, this.uploadTemplate, this.uploadTemplate = useUploadTemplate);
		updateProjectItems(projectItems);
		firePropertyChange(PROPERTY_PROJECT, this.project, this.project = getProjectOrDefault(selectedProject, projectItems));
		firePropertyChange(PROPERTY_TEMPLATES, this.projectTemplates, this.projectTemplates = getProjectTemplates(project, projectItems) );
		firePropertyChange(PROPERTY_TEMPLATE, this.template, this.template = selectedTemplate);
		initTemplateParameters(selectedTemplate);
	}
	
	private List<ObservableTreeItem> getProjectTemplates(IProject selectedProject,
			List<ObservableTreeItem> allProjects) {
		if (allProjects == null) {
			return null;
		}
		for (ObservableTreeItem item : allProjects) {
			if (item.getModel().equals(selectedProject)) {
				return item.getChildren();
			}
		}
		return allProjects;
	}

	private void updateProjectItems(List<ObservableTreeItem> projectItems) {
		List<ObservableTreeItem> oldItems = new ArrayList<>(this.projectItems);
		// ensure we're not operating on the same list
		List<ObservableTreeItem> newItems = new ArrayList<>();
		if (projectItems != null) {
			newItems.addAll(projectItems);
		}
		this.projectItems.clear();
		this.projectItems.addAll(newItems);
		firePropertyChange(PROPERTY_PROJECT_ITEMS, oldItems, this.projectItems);
	}
	
	@Override
	public Collection<IResource> getItems() {
		return items;
	}

	private void setItems(Collection<IResource> items) {
		firePropertyChange(PROPERTY_ITEMS, this.items, this.items = items);
	}

	@Override
	public void setTemplate(ITemplate template) {
		update(this.uploadTemplate, this.project, this.projectItems, template);
	}

	private void initTemplateParameters(ITemplate template) {
		if (template == null) {
			return;
		}

		setParameters(new ArrayList<IParameter>(template.getParameters().values()));
		setItems(template.getItems());
		setLabels(template.getObjectLabels());
	}

	private IProject getProjectOrDefault(IProject project, List<ObservableTreeItem> projects) {
		if (project == null) {
			project = getDefaultProject(projects);
		}
		return project;
	}

	@Override
	public void setProject(IProject project) {
		update(this.uploadTemplate, project, this.projectItems, this.template);
	}

	@Override
	public IProject getProject() {
		return project;
	}

	@Override
	public ITemplate getTemplate() {
		return template;
	}

	@Override
	public void loadResources() {
		if (connection == null) {
			return;
		}
		ObservableTreeItem connectionItem = TemplateTreeItems.INSTANCE.create(connection);
		connectionItem.load();
		List<ObservableTreeItem> projects = connectionItem.getChildren();
		setProjectItems(projects);
	}

	@Override
	public List<IParameter> getParameters() {
		return parameters;
	}

	@Override
	public void setParameters(List<IParameter> parameters) {
		firePropertyChange(PROPERTY_PARAMETERS, this.parameters, this.parameters = parameters);
		this.originalValueMap = new HashMap<String, String>(parameters.size());
		for (IParameter param : parameters) {
			originalValueMap.put(param.getName(), param.getValue());
		}
	}

	@Override
	public IParameter getSelectedParameter() {
		return this.selectedParameter;
	}

	@Override
	public void setSelectedParameter(IParameter parameter) {
		firePropertyChange(PROPERTY_SELECTED_PARAMETER, this.selectedParameter, this.selectedParameter = parameter);
	}

	@Override
	public void updateParameterValue(IParameter param, String value) {
		param.setValue(value);
	}

	@Override
	public void resetParameter(IParameter param) {
		updateParameterValue(param, originalValueMap.get(param.getName()));
	}

	private void setLabels(Map<String, String> labelMap) {
		if(labelMap == null) return;
		List<Label> labels =  new ArrayList<Label>(labelMap.size());
		for (Entry<String,String> entry : labelMap.entrySet()) {
			labels.add(new Label(entry.getKey(), entry.getValue()));
		}
		setLabels(labels);
	}

	@Override
	public void setUseUploadTemplate(boolean uploadTemplate) {
		update(uploadTemplate, this.project, this.projectItems, this.template);
	}

	@Override
	public boolean isUseUploadTemplate() {
		return this.uploadTemplate;
	}

	@Override
	public void setTemplateFileName(String name) {
		try {
			uploadedTemplate = resourceFactory.create(createInputStream(name));
			setTemplate(uploadedTemplate);
		} catch (FileNotFoundException e) {
			name = "";
			setTemplate(null);
			throw new OpenShiftException(e, "Unable to find the file to upload");
		} catch (ResourceFactoryException | ClassCastException e) {
			name = "";
			setTemplate(null);
			throw e;
		} finally {
			firePropertyChange(PROPERTY_TEMPLATE_FILENAME, this.templateFilename, this.templateFilename = name);
		}
	}
	
	public InputStream createInputStream(String fileName) throws FileNotFoundException {
		return new FileInputStream(fileName);
	}

	@Override
	public String getTemplateFileName() {
		return this.templateFilename;
	}

	@Override
	public Connection getConnection() {
		return connection;
	}

	@Override
	public boolean hasConnection() {
		return connection != null;
	}

	@Override
	public Connection setConnection(Connection connection) {
		if (ObjectUtils.equals(connection, this.connection)) {
			return this.connection;
		}

		setResourceFactory(connection);
		reset();
		firePropertyChange(PROPERTY_CONNECTION, this.connection, this.connection = connection);
		return connection;
	}

	private void reset() {
		update(this.uploadTemplate, null, null, null);
	}

	private IProject getDefaultProject(List<ObservableTreeItem> projects) {
		if (projects == null 
				|| projects.size() == 0) {
			return null;
		}
		return (IProject) projects.get(0).getModel();
	}

	private void setResourceFactory(Connection connection) {
		if (connection != null) {
			this.resourceFactory = connection.getResourceFactory();
		}
	}

	public void setResourceFactory(IResourceFactory factory) {
		this.resourceFactory = factory;
	}
	
	private void setProjectItems(List<ObservableTreeItem> projects) {
		update(this.uploadTemplate, null, projects, this.template);
	}

	@Override
	public List<ObservableTreeItem> getProjectItems() {
		return this.projectItems;
	}

	@Override
	public List<ObservableTreeItem> getTemplates() {
		return this.projectTemplates;
	}
	
	@Override
	public boolean hasProjects() {
		return projectItems != null 
				&& projectItems.size() > 0;
	}

	@Override
	public Object getContext() {
		// TODO Auto-generated method stub
		return null;
	}


}
