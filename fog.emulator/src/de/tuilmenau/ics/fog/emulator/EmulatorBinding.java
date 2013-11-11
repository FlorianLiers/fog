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

import java.util.HashMap;

import net.rapi.Description;
import net.rapi.Identity;
import net.rapi.Name;
import net.rapi.NetworkException;
import net.rapi.impl.base.BaseBinding;
import de.tuilmenau.ics.fog.IEvent;
import de.tuilmenau.ics.fog.emulator.packets.BindingReply;
import de.tuilmenau.ics.fog.emulator.packets.Packet;
import de.tuilmenau.ics.fog.util.Logger;


/**
 * This class implements the Binding for emulators and broadcasts
 * its existence periodically.
 */
public class EmulatorBinding extends BaseBinding implements IEvent, Port
{
	private static final double BROADCAST_INTERVAL_SEC = 5.0d;
	
	
	public EmulatorBinding(Logger logger, Name name, int portNumber, Description requirements, Identity identity, EmulatorLayer layer)
	{
		super(name, requirements, identity);
		
		this.portNumber = portNumber;
		this.logger = logger;
		this.layer = layer;
		
		fire();
	}

	@Override
	public synchronized void close()
	{
		activated = false;
		
		// are there no open connections?
		if(connections.size() <= 0) {
			if(layer != null) {
				logger.log(this, "Removing binding");

				layer.closed(this);	
				layer = null;
			}
			
			// *now* we can invalidate this object
			super.close();
		} else {
			// still open connections -> wait until they are closed
			logger.log(this, "Closing (" +connections.size() +" connections still open)");
		}
	}
	
	@Override
	public boolean isActive()
	{
		return activated && (layer != null);
	}
	
	@Override
	protected void notifyFailure(Throwable failure, EventListener listener)
	{
		logger.warn(this, "Ignoring event listerner failure from " +listener, failure);
	}
	
	private boolean broadcastBinding()
	{
		boolean res = false;
		
		if(isActive()) {
			// create packet object only once and reuse it afterwards
			if(cachedBroadcastPacket == null) {
				BindingReply msg = new BindingReply(getName());
				
				cachedBroadcastPacket = new Packet(getPortNumber(), EmulatorLayer.CONTROL_PORT, msg);
			}
			
			try {
				layer.sendPacket(cachedBroadcastPacket);
			}
			catch (NetworkException exc) {
				res = false;
			}
		}
		
		return res;
	}

	@Override
	public int getPortNumber()
	{
		return portNumber;
	}

	@Override
	public void handlePacket(Packet packet)
	{
		PortID sender = packet.getSender();
		EmulatorConnectionEndPoint conn = null;
		HashMap<Integer, EmulatorConnectionEndPoint> conns = connections.get(sender.getAddress());
		// sender unknown?
		if(conns == null) {
			// prepare to open connection implicitly
			conns = new HashMap<Integer, EmulatorConnectionEndPoint>();
			
			connections.put(sender.getAddress(), conns);
		}
		
		conn = conns.get(sender.getPortNumber());
		// is port of sender unknown?
		if(conn == null) {
			if(isActive()) {
				conn = new EmulatorConnectionEndPoint(layer, this, sender);
				
				logger.log(this, "Creating port implicitly: " +conn);
				conns.put(sender.getPortNumber(), conn);
				
				// notify higher layer about new connection
				addIncomingConnection(conn);
			} else {
				logger.warn(this, "Binding closed. Rejecting new connection from " +sender);
				
				// exit method to avoid relaying of packet
				return;
			}
		}
		
		// relay packet to connection
		conn.handlePacket(packet);
	}

	
	@Override
	public void fire()
	{
		// is binding still valid?
		if(layer != null) {
			broadcastBinding();
			
			// re-schedule event
			layer.getSim().getTimeBase().scheduleIn(BROADCAST_INTERVAL_SEC, this);
		}
	}
	
	/**
	 * @return Number of open connections to this binding
	 */
	public int getNumberConnections()
	{
		return connections.size();
	}
	
	/**
	 * Closes all connections established to this binding.
	 */
	public void closeAllConnections()
	{
		// iterate all peers
		while(!connections.isEmpty()) {
			HashMap<Integer, EmulatorConnectionEndPoint> conns = connections.values().iterator().next();
			
			// iterate all connections with a peer
			while(!conns.isEmpty()) {
				EmulatorConnectionEndPoint conn = conns.values().iterator().next();
				
				conn.close();
			}
		}
	}
	
	/**
	 * Called by connections of this binding, in order to inform
	 * binding about their closing.
	 * 
	 * @param emulatorConnectionEndPoint CEP that was closed
	 */
	public void closed(EmulatorConnectionEndPoint cep)
	{
		boolean removed = false;
		
		if(cep != null) {
			// remove entry
			PortID peer = cep.getPeer();
			HashMap<Integer, EmulatorConnectionEndPoint> conns = connections.get(peer.getAddress());
			
			if(conns != null) {
				removed = conns.remove(peer.getPortNumber()) != null;
				
				// was it the last entry?
				if(conns.size() <= 0) {
					connections.remove(peer.getAddress());
				}
			}
		}
		
		if(removed) {
			// binding might be terminated and waiting for the last connection to close
			if(!isActive()) {
				close();
			}
		} else {
			logger.err(this, "Can not remove CEP " +cep +" from list since it is not in.");
		}
	}

	private Logger logger;

	/**
	 * It indicates if the binding accept new connections
	 */
	private boolean activated = true;
	
	private EmulatorLayer layer;
	private int portNumber;
	
	private Packet cachedBroadcastPacket = null;
	
	/**
	 * Stores all connections to this binding.
	 * Key includes the sender address and the sender port number.
	 */
	private HashMap<Address, HashMap<Integer, EmulatorConnectionEndPoint>> connections = new HashMap<Address, HashMap<Integer,EmulatorConnectionEndPoint>>();

}
