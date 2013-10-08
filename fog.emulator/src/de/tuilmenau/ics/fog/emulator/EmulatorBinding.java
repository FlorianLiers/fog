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
import net.rapi.events.NewConnectionEvent;
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
		layer.closed(this);

		// *now* we can invalidate this object
		super.close();
		layer = null;
	}
	
	@Override
	public boolean isActive()
	{
		return layer != null;
	}
	
	@Override
	protected void notifyFailure(Throwable failure, EventListener listener)
	{
		logger.warn(this, "Ignoring event listerner failure from " +listener, failure);
	}
	
	private boolean broadcastBinding()
	{
		boolean res = false;
		
		if(layer != null) {
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
			conn = new EmulatorConnectionEndPoint(layer, getName(), portNumber, sender);
			
			logger.log(this, "Creating port implicitly: " +conn);
			conns.put(sender.getPortNumber(), conn);
			
			// notify higher layer about new connection
			addIncomingConnection(conn);
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
	
	private Logger logger;
	private EmulatorLayer layer;
	private int portNumber;
	
	private Packet cachedBroadcastPacket = null;
	
	/**
	 * Stores all connections to this binding.
	 * Key includes the sender address and the sender port number.
	 */
	private HashMap<Address, HashMap<Integer, EmulatorConnectionEndPoint>> connections = new HashMap<Address, HashMap<Integer,EmulatorConnectionEndPoint>>();
}
