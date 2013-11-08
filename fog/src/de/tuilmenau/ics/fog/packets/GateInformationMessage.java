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
package de.tuilmenau.ics.fog.packets;

import java.io.Serializable;

import net.rapi.Name;
import de.tuilmenau.ics.fog.transfer.ForwardingNode;
import de.tuilmenau.ics.fog.transfer.gates.GateID;
import de.tuilmenau.ics.fog.ui.Viewable;


/**
 * TODO docu
 */
public class GateInformationMessage implements Serializable
{
	private static final long serialVersionUID = -7309033185606147609L;

	/**
	 * Constructor for requesting a reverse gate though the same connection.
	 */
	public GateInformationMessage(Name pLocalBindingName, GateID pLocalOutgoingGateNumber, ForwardingNode pLocalDestination)
	{
		mPeerBindingName = pLocalBindingName;
		mPeerGateNumber = pLocalOutgoingGateNumber;
		mPeerDestinationRoutingName = pLocalDestination.getEntity().getRoutingService().getNameFor(pLocalDestination);
	}

	public Name getPeerBindingName()
	{
		return mPeerBindingName;
	}

	public GateID getPeerGateNumber()
	{
		return mPeerGateNumber;
	}
	
	public Name getPeerRoutingName()
	{
		return mPeerDestinationRoutingName;
	}

	@Viewable("Peer binding name")
	private Name mPeerBindingName;
	
	@Viewable("Peer node gate number")
	private GateID mPeerGateNumber;
	
	@Viewable("Peer forwarding node routing name")
	private Name mPeerDestinationRoutingName;
}
