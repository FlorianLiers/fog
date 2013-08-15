/*******************************************************************************
 * Forwarding on Gates Simulator/Emulator
 * Copyright (C) 2012, Integrated Communication Systems Group, TU Ilmenau.
 * 
 * This program and the accompanying materials are dual-licensed under either
 * the terms of the Eclipse Public License v1.0 as published by the Eclipse
 * Foundation
 *  
 *   or (per the licensee's choosing)
 *  
 * under the terms of the GNU General Public License version 2 as published
 * by the Free Software Foundation.
 ******************************************************************************/
package net.rapi.events;

import net.rapi.Binding;


public class NewConnectionEvent extends Event
{
	public NewConnectionEvent(Binding binding)
	{
		super(binding);
	}
	
	public Binding getBinding()
	{
		return (Binding) getSource();
	}
}
