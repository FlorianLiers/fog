/*******************************************************************************
 * Forwarding on Gates Simulator/Emulator - Bus View
 * Copyright (c) 2012, Integrated Communication Systems Group, TU Ilmenau.
 * 
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html.
 ******************************************************************************/
package de.tuilmenau.ics.fog.bus.view;

import java.util.LinkedList;

import org.eclipse.ui.views.properties.IPropertyDescriptor;
import org.eclipse.ui.views.properties.TextPropertyDescriptor;

import de.tuilmenau.ics.fog.bus.Bus;
import de.tuilmenau.ics.fog.eclipse.properties.AnnotationPropertySource;


public class BusPropertySource extends AnnotationPropertySource
{
	public BusPropertySource(Bus bus)
	{
		this.bus = bus;
	}

	@Override
	protected void extendPropertyList(LinkedList<IPropertyDescriptor> list)
	{
		list.addLast(new TextPropertyDescriptor(PROPERTY_PACKETS_NUMBER, "Number Packets"));
		
		extendPropertyListBasedOnAnnotations(list, bus);
	}
	
	@Override
	public Object getPropertyValue(Object name)
	{
		if(PROPERTY_PACKETS_NUMBER.equals(name)) {
			return bus.getNumberPackets();
		} else {
			return super.getPropertyValueBasedOnAnnotation(name, bus);
		}
	}

	private Bus bus;
	
	private static final String PROPERTY_PACKETS_NUMBER = "Bus.Packets.Number";
}

