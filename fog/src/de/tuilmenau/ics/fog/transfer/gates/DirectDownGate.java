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

import net.rapi.Connection;
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
import de.tuilmenau.ics.fog.ui.Viewable;

/**
 * Represents gate which forwards messages to another node by using a lower
 * layer ID (e.g. a MAC address for a bus).
 */
public class DirectDownGate extends AbstractGate
{
	public DirectDownGate(FoGEntity node, LowerLayerObserver netInf, Connection connection, Identity owner)
	{
		super(node, connection.getRequirements(), owner);

		mConnection = connection;
		mNetInf = netInf;
	}

	public Name getPeerName()
	{
		Name res = null;
		
		if(mConnection != null) {
			res = mConnection.getBindingName();
			
			if(res == null) {
				// name not known due to passive establishment
				// try to get name of peer via LowerLayerSession
				return mNetInf.getPeerName(mConnection);
			}
		}
		// else: not connected; no peer name
		
		return res;
	}
	
	public LowerLayerObserver getLowerLayer()
	{
		return mNetInf;
	}
	
	/*
	 * knowing the next hop is not too important for most DownGates, but it is of great help for the GUI.
	 */
	public ForwardingElement getNextNode()
	{
		return mNetInf.getLowerLayerGUIRepresentation();
	}
	
	@Override
	protected void init()
	{
		if(mConnection == null) {
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
		if(mConnection != null) {
			if(!invisible) incMessageCounter();
			
			try {
				mConnection.write(packet);
			}
			catch(NetworkException exc) {
				// Error during transmission?
				// Do not do any recovery for invisible packets.
				if(!invisible) {
					mEntity.getLogger().warn(this, "Cannot send packet " +packet +" through " +mConnection, exc);
					
					// maybe gate already closed during error recovery? 
					if((getState() != GateState.SHUTDOWN) && (getState() != GateState.DELETED)) {
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
		init();
	}
	
	@Override
	public void close() throws NetworkException
	{
		super.close();
		
		if(mConnection != null) {
			mConnection.close();
			mConnection = null;
		}
	}

	@Viewable("Network interface")
	private LowerLayerObserver mNetInf;
	
	@Viewable("Connection")
	private Connection mConnection;
	
}
