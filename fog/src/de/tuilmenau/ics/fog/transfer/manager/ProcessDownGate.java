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
package de.tuilmenau.ics.fog.transfer.manager;

import net.rapi.Connection;
import net.rapi.Description;
import net.rapi.Identity;
import net.rapi.Name;
import net.rapi.NetworkException;
import de.tuilmenau.ics.fog.Config;
import de.tuilmenau.ics.fog.FoGEntity;
import de.tuilmenau.ics.fog.IEvent;
import de.tuilmenau.ics.fog.packets.OpenGateResponse;
import de.tuilmenau.ics.fog.packets.PleaseOpenDownGate;
import de.tuilmenau.ics.fog.packets.Signalling;
import de.tuilmenau.ics.fog.routing.RoutingService;
import de.tuilmenau.ics.fog.transfer.gates.AbstractGate;
import de.tuilmenau.ics.fog.transfer.gates.DirectDownGate;
import de.tuilmenau.ics.fog.transfer.gates.GateID;
import de.tuilmenau.ics.fog.transfer.gates.ReroutingGate;


/**
 * This process is responsible for creating, checking and de-constructing
 * a DownGate on a host. The DownGate represents a connection via a lower
 * layer to some other transfer service instance.
 */
public class ProcessDownGate extends ProcessGateConstruction
{
	private static final double MAX_NUMBER_RETRIES_SIGNALING = 4; // TODO required?
	
	/**
	 * Constructor for active opening a connection to a peer.
	 * @throws NetworkException On error
	 */
	public ProcessDownGate(LowerLayerObserver netInf, Name destination, Description requirements, Identity pOwner, ReroutingGate backup) throws NetworkException
	{
		super(netInf.getMultiplexerGate(), backup, pOwner);
		
		mInterface = netInf;
		
		mSession = mInterface.getConnectionTo(destination, requirements);
	}

	/**
	 * Constructor for passive handling of an incoming connection from a peer.
	 */
	public ProcessDownGate(LowerLayerObserver netInf, LowerLayerSession pSession, Identity pOwner)
	{
		super(netInf.getMultiplexerGate(), null, pOwner);
		
		mInterface = netInf;
		
		mSession = pSession;
	}
	
	@Override
	public void start() throws NetworkException
	{
		super.start();
		
		mSession.setObserver(this);
	}
	
	public void connected()
	{
		// create gate representing the connectivity to peer
		NetworkException error = null;
		try {
			create();
		}
		catch (NetworkException exc) {
			getLogger().err(this, "Can not create gate.", exc);
			
			error = exc;
		}

		// Determining message for peer
		Signalling answer;
		IEvent timer = null;
		
		// in which state are we?
		if(hasRequester()) {
			// create answer message
			if(error != null) {
				answer = new OpenGateResponse(this, error);
			} else {
				RoutingService rs = mInterface.getMultiplexerGate().getEntity().getRoutingService();
				answer = new OpenGateResponse(getRequesterID(), mGate.getGateID(), rs.getNameFor(mInterface.getMultiplexerGate()));
			}
		} else {
			if(error == null) {
				// request reverse gate from peer
				answer = new PleaseOpenDownGate(getID(), mGate.getGateID(), mInterface.getMultiplexerGate(), mInterface.getAttachmentName());
				
				// re-send signaling message
				timer = new IEvent() {
					@Override
					public void fire()
					{
						if(!isFinished()) {
							if(!isOperational()) {
								getLogger().warn(this, "Timeout for re-sending the signaling message.");
								
								connected();
							}
						}
					}
				};
			} else {
				// no message
				answer = null;
			}
		}

		if(answer != null) {
			sendToPeer(answer);
		}
		
		// do it after sending the message. If we are not executed in the event loop, we would risk to be executed immediately otherwise.
		if(timer != null) {
			getTimeBase().scheduleIn(Config.PROCESS_STD_TIMEOUT_SEC / 4.0d, timer);
		}
		
		if(error != null) {
			terminate(error);
		}
	}

	@Override
	protected void finished()
	{
		if(mSession != null) {
			mSession.setObserver(null);
			mSession.stop();
			
			mSession = null;
		}
		
		super.finished();
	}
	
	/**
	 * Determines if the connection belongs to process.
	 */
	public boolean hasConnection(Connection connection)
	{
		if(mSession != null) {
			return mSession.getConnection() == connection;
		} else {
			return false;
		}
	}
	
	/**
	 * @return Name of peer binding of session or null if not connected
	 */
	public Name getNeighborName()
	{
		if(mSession != null) {
			return mSession.getBindingName();
		} else {
			return null;
		}
	}
	
	public GateID getGateNumber()
	{
		if(mGate != null) {
			return mGate.getGateID();
		} else {
			return null;
		}
	}

	protected AbstractGate newGate(FoGEntity pNode) throws NetworkException
	{
		DirectDownGate tGate = new DirectDownGate(pNode, mInterface, mSession.getConnection(), getOwner());
		Description tRequ = mSession.getConnection().getRequirements();
		
		if(Config.Connection.TERMINATE_WHEN_IDLE) {
			if(tRequ != null) {
				if(!tRequ.isBestEffort()) {
					tGate.startCheckForIdle();
				}
			}
		}
		
		return tGate;
	}
	
	private LowerLayerObserver mInterface;
	
	private LowerLayerSession mSession;
}
