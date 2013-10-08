/*******************************************************************************
 * Forwarding on Gates Simulator/Emulator - emulator interface
 * Copyright (c) 2012, Integrated Communication Systems Group, TU Ilmenau.
 * 
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html.
 ******************************************************************************/
package de.tuilmenau.ics.fog.emulator;

import net.rapi.Name;
import net.rapi.impl.base.SimpleNeighborName;
import de.tuilmenau.ics.fog.routing.naming.NameMappingEntry;

public class EmulatorNeighborName extends SimpleNeighborName
{
	public EmulatorNeighborName(Name bindingName, PortID entry)
	{
		super(bindingName);
		
		this.destination = entry;
	}

	public PortID getDestination()
	{
		return destination;
	}
	
	@Override
	public String toString()
	{
		return getBindingName() +"@" +destination;
	}
	
	private PortID destination;
}
