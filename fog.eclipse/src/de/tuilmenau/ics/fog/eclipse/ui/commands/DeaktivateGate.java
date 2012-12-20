/*******************************************************************************
 * Forwarding on Gates Simulator/Emulator - Eclipse
 * Copyright (c) 2012, Integrated Communication Systems Group, TU Ilmenau.
 * 
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html.
 ******************************************************************************/
package de.tuilmenau.ics.fog.eclipse.ui.commands;

import org.eclipse.ui.IWorkbenchPartSite;

import de.tuilmenau.ics.fog.transfer.gates.AbstractGate;

public class DeaktivateGate extends Command
{
	public DeaktivateGate()
	{
	}

	@Override
	public void init(IWorkbenchPartSite site, Object object)
	{
		if(object instanceof AbstractGate) {
			gate = (AbstractGate) object;
		}
	}

	@Override
	public void main() throws Exception
	{
		if(gate != null) {
			gate.shutdown();
		}
	}

	private AbstractGate gate;
}
