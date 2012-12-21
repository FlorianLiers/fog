/*******************************************************************************
 * Forwarding on Gates Simulator/Emulator - Hierarchical Routing Management
 * Copyright (c) 2012, Integrated Communication Systems Group, TU Ilmenau.
 * 
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html.
 ******************************************************************************/
package de.tuilmenau.ics.fog.routing.hierarchical.clusters;

import de.tuilmenau.ics.fog.facade.Name;
import de.tuilmenau.ics.fog.routing.naming.hierarchical.HRMID;


public interface VirtualNode extends Name {
	public HRMID retrieveAddress();
	
	public Name retrieveName();
}