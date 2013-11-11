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
package de.tuilmenau.ics.fog.transfer.gates;

import net.rapi.Description;
import net.rapi.Identity;
import net.rapi.Name;
import de.tuilmenau.ics.fog.FoGEntity;
import de.tuilmenau.ics.fog.packets.Packet;
import de.tuilmenau.ics.fog.transfer.ForwardingElement;
import de.tuilmenau.ics.fog.transfer.manager.Controller;
import de.tuilmenau.ics.fog.ui.Viewable;

/**
 * Class for defining backup routes to repair failures in the forwarding.
 */
public class ReroutingGate extends HorizontalGate
{
	public ReroutingGate(FoGEntity pEntity, DirectDownGate pInvalidGate, Identity pOwner, int pRemoveGatesFromRoute)
	{
		super(pEntity, pEntity.getCentralFN(), pOwner);
		
		mNeighborLLID = pInvalidGate.getPeerBindingName();
		mRemoveGatesFromRoute = pRemoveGatesFromRoute;
		
		setDescription(pInvalidGate.getDescription());
	}
	
	public void handlePacket(Packet pPacket, ForwardingElement pLastHop)
	{
		for(int i=0; i<mRemoveGatesFromRoute; i++) {
			if(pPacket.getRoute().getFirst(true) == null) {
				mLogger.warn(this, "Can not remove " +i +" from " +mRemoveGatesFromRoute +" gates from route of " +pPacket);
				break;
			}
		}
		
		if(mRemoveGatesFromRoute < 0) {
			pPacket.getRoute().clear();
		}
		
		super.handlePacket(pPacket, pLastHop);
	}

	/**
	 * Gate is used to repair forwarding and should not
	 * be used by the routing for subsequent routes.
	 */
	public boolean isPrivateToTransfer()
	{
		return true;
	}
	
	public boolean match(Name pNeighborLLID, GateID pReverseGateNumber, Description pRequirements)
	{
		if(pNeighborLLID.equals(mNeighborLLID)) {
			if(pReverseGateNumber != null) {
				if(pReverseGateNumber.equals(getReverseGateID())) {
					return Controller.checkGateDescr(this, pRequirements);
				}
			} else {
				return Controller.checkGateDescr(this, pRequirements);
			}
		}
		
		return false;
	}
	
	@Viewable("Neighbor name")
	private Name mNeighborLLID;
	
	@Viewable("Remove number of gates from route")
	private int mRemoveGatesFromRoute;
}
