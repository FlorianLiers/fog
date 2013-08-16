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

import java.util.LinkedList;

import net.rapi.Layer;
import net.rapi.NeighborName;
import net.rapi.NetworkException;

import org.eclipse.ui.views.properties.IPropertyDescriptor;
import org.eclipse.ui.views.properties.TextPropertyDescriptor;

import de.tuilmenau.ics.fog.ui.PacketLogger;


public class LayerPropertySource extends AnnotationPropertySource
{
	public LayerPropertySource(Layer layer)
	{
		this.layer = layer;
	}

	@Override
	protected void extendPropertyList(LinkedList<IPropertyDescriptor> list)
	{
		list.addLast(new TextPropertyDescriptor(PROPERTY_NEIGHBORS, "Neighbors"));
		list.addLast(new TextPropertyDescriptor(PROPERTY_PACKETS, "Last Packets"));
		
		extendPropertyListBasedOnAnnotations(list, layer);
	}
	
	@Override
	public Object getPropertyValue(Object name)
	{
		try {
			if(PROPERTY_NEIGHBORS.equals(name)) {
				Iterable<NeighborName> iter = layer.getNeighbors(null);
				
				if(iter != null) {
					LinkedList<NeighborName> neighbors = new LinkedList<NeighborName>();
					for(NeighborName neighb : iter) {
						neighbors.addLast(neighb);
					}
					return neighbors;
				} else {
					return "n.a.";
				}
				
			}
			else if(PROPERTY_PACKETS.equals(name)) {
				return PacketLogger.getLogger(layer);
			}
			else {
				return getPropertyValueBasedOnAnnotation(name, layer);
			}
		}
		catch(NetworkException exc) {
			return exc.getLocalizedMessage();
		}
	}

	private Layer layer;
	
	private static final String PROPERTY_NEIGHBORS = "Layer.Neighbors";
	private static final String PROPERTY_PACKETS   = "Layer.Packets";
}

