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

import java.util.NoSuchElementException;

import net.rapi.Identity;
import net.rapi.Layer.LayerStatus;
import net.rapi.Name;
import net.rapi.NetworkException;
import de.tuilmenau.ics.fog.FoGEntity;
import de.tuilmenau.ics.fog.packets.Packet;
import de.tuilmenau.ics.fog.routing.RouteSegmentPath;
import de.tuilmenau.ics.fog.transfer.ForwardingElement;
import de.tuilmenau.ics.fog.transfer.manager.Controller.BrokenType;
import de.tuilmenau.ics.fog.transfer.manager.LowerLayerObserver;
import de.tuilmenau.ics.fog.transfer.manager.LowerLayerSession;
import de.tuilmenau.ics.fog.ui.Viewable;

/**
 * Represents gate which forwards messages to another node by using a lower
 * layer ID (e.g. a MAC address for a bus).
 */
public class DirectDownGate extends AbstractGate
{
	public DirectDownGate(FoGEntity node, LowerLayerSession session, Identity owner)
	{
		super(node, session.getConnection().getRequirements(), owner);

		this.session = session;
	}

	/**
	 * @return Name of peer FoG binding (or null if not known)
	 */
	public Name getPeerBindingName()
	{
		return session.getPeerBindingName();
	}
	
	/**
	 * @return Observer of lower layer the gate belongs to
	 */
	public LowerLayerObserver getLowerLayer()
	{
		return session.getLowerLayer();
	}
	
	/*
	 * knowing the next hop is not too important for most DownGates, but it is of great help for the GUI.
	 */
	public ForwardingElement getNextNode()
	{
		return session.getLowerLayer().getLowerLayerGUIRepresentation();
	}
	
	@Override
	protected void init()
	{
		if(session == null) {
			setState(GateState.ERROR);
		} /*else {
			if(!mConnection.isConnected()) {
				setState(GateState.ERROR);
			}
		}*/
	}
	
	@Override
	protected void setLocalPartnerGateID(GateID pReverseGateID)
	{
		super.setLocalPartnerGateID(pReverseGateID);
		
		if(pReverseGateID != null) {
			switchToState(GateState.OPERATE);
		} else {
			switchToState(GateState.ERROR);
		}
	}
	
	public void handlePacket(Packet packet, ForwardingElement lastHop)
	{
		packet.addToDownRoute(getGateID());
		if (packet.traceBackwardRoute()) {
			if (isReverseGateAvailable()) {
				packet.addReturnRoute(getReverseGateID());
			} else {
				packet.returnRouteBroken();
			}
		}
		
		boolean invisible = packet.isInvisible();

		// send packet to connection through lower layer
		if(session != null) {
			if(!invisible) incMessageCounter();
			
			try {
				session.getConnection().write(packet);
			}
			catch(NetworkException exc) {
				// Error during transmission?
				// Do not do any recovery for invisible packets.
				if(!invisible) {
					mEntity.getLogger().warn(this, "Cannot send packet " +packet +" through " +session, exc);
					
					// maybe gate already closed during error recovery? 
					if((getState() != GateState.SHUTDOWN) && !isDeleted()) {
						switchToState(GateState.ERROR);
					}
					
					// determine error problem and inform controller about it
					// TODO BrokenType.NODE?
					BrokenType errorType = BrokenType.UNKNOWN;
					if(getLowerLayer().getLayer().getStatus() == LayerStatus.DISCONNECTED) {
						errorType = BrokenType.BUS;
					}
					
					// fix reverse route since it is too long // TODO make it with nice code!
					try {
						if (packet.getReturnRoute().getFirst() instanceof RouteSegmentPath) {
							((RouteSegmentPath)packet.getReturnRoute().getFirst()).removeFirst();
							((RouteSegmentPath)packet.getReturnRoute().getFirst()).removeFirst();
						}
						mEntity.getController().handleBrokenElement(errorType, getLowerLayer(), packet, this);
					}
					catch (NoSuchElementException e) {
						mEntity.getLogger().err(this, "Could not modify return route", e);
					}
				}
			}
		} else {
			if(!invisible) {
				// gate maybe closed
				mLogger.err(this, "No connection through lower layer given. Dropping packet " +packet);
			}
			// else: ignore error due to invisible packet
			packet.dropped(this);
		}
	}
		
	@Override
	public void refresh()
	{
		if(session != null) {
			if(session.getConnection().isConnected()) {
				return;
			}
		}
		
		setState(GateState.ERROR);
	}
	
	@Override
	protected void close() throws NetworkException
	{
		super.close();
		
		if(session != null) {
			session.stop();
			session = null;
		}
	}

	@Viewable("Session for lower layer connection")
	private LowerLayerSession session;
	
	@Viewable("Peer FN routing name")
	private Name mPeerRoutingName;
}
