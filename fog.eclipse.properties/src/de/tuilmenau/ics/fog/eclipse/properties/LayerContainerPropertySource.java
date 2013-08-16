/*******************************************************************************
 * Forwarding on Gates Simulator/Emulator - Eclipse Properties
 * Copyright (c) 2012, Integrated Communication Systems Group, TU Ilmenau.
 * 
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html.
 ******************************************************************************/
package de.tuilmenau.ics.fog.eclipse.properties;

import net.rapi.Layer;
import net.rapi.LayerContainer;

import org.eclipse.ui.views.properties.IPropertyDescriptor;
import org.eclipse.ui.views.properties.IPropertySource;
import org.eclipse.ui.views.properties.TextPropertyDescriptor;


public class LayerContainerPropertySource implements IPropertySource
{
	public LayerContainerPropertySource(LayerContainer container)
	{
		this.container = container;
	}

	public IPropertyDescriptor[] getPropertyDescriptors()
	{
		if (propertyDescriptors == null) {
			// get all layer elements
			Layer[] layers = container.getLayers(null);
			
			// construct GUI elements for each layer
			propertyDescriptors = new IPropertyDescriptor[layers.length];
			
			for(int i=0; i<layers.length; i++) {
				propertyDescriptors[i] = propertyDescriptors[i] = new TextPropertyDescriptor(layers[i], layers[i].toString());
			}
		}
		return propertyDescriptors;
	}

	@Override
	public Object getEditableValue()
	{
		return null;
	}

	@Override
	public Object getPropertyValue(Object name)
	{
		return name;
	}

	@Override
	public boolean isPropertySet(Object id)
	{
		return false;
	}

	@Override
	public void resetPropertyValue(Object id)
	{
		// ignore it
	}

	@Override
	public void setPropertyValue(Object name, Object value)
	{
		// ignore it
	}

	
	private LayerContainer container;
	private IPropertyDescriptor[] propertyDescriptors;
}

