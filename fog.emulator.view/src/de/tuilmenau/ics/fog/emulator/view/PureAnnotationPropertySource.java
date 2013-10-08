/*******************************************************************************
 * Forwarding on Gates Simulator/Emulator - Emulator view
 * Copyright (c) 2013, Integrated Communication Systems Group, TU Ilmenau.
 * 
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html.
 ******************************************************************************/
package de.tuilmenau.ics.fog.emulator.view;

import java.util.LinkedList;

import org.eclipse.ui.views.properties.IPropertyDescriptor;

import de.tuilmenau.ics.fog.eclipse.properties.AnnotationPropertySource;

/**
 * Shows only the annotated attributes of an object.
 */
public class PureAnnotationPropertySource extends AnnotationPropertySource
{
	public PureAnnotationPropertySource(Object obj)
	{
		this.obj = obj;
	}
	
	@Override
	public Object getPropertyValue(Object id) {
		return getPropertyValueBasedOnAnnotation(id, obj);
	}
	
	@Override
	protected void extendPropertyList(LinkedList<IPropertyDescriptor> list)
	{
		extendPropertyListBasedOnAnnotations(list, obj);
	}

	private Object obj;
}
