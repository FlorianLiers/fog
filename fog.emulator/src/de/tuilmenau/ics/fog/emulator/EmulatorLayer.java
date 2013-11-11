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
import java.util.Iterator;
import java.util.LinkedList;

import net.rapi.Binding;
import net.rapi.Connection;
import net.rapi.Description;
import net.rapi.Identity;
import net.rapi.Layer;
import net.rapi.Name;
import net.rapi.NeighborName;
import net.rapi.NetworkException;
import net.rapi.impl.base.BaseEventSource;
import de.tuilmenau.ics.fog.emulator.packets.Packet;
import de.tuilmenau.ics.fog.routing.naming.NameMappingEntry;
import de.tuilmenau.ics.fog.topology.Simulation;
import de.tuilmenau.ics.fog.ui.Viewable;
import de.tuilmenau.ics.fog.util.Logger;


/**
 * TODO
 */
public class EmulatorLayer extends BaseEventSource implements Layer
{
	public static final int CONTROL_PORT = 0;
	

	public EmulatorLayer(Address address, EmulatorMedium medium)
	{	
		this.address = address;
		this.medium = medium;
		this.logger = medium.getLogger();
		this.control = new ControlPort(this);
		
		// register default port
		ports.put(CONTROL_PORT, control);
	}
	
	public Simulation getSim()
	{
		if(medium != null) {
			return medium.getSim();
		} else {
			return null;
		}
	}
	
	public Logger getLogger()
	{
		return logger;
	}

	@Override
	public LayerStatus getStatus()
	{
		return null;
	}

	@Override
	public synchronized Binding bind(Connection parentSocket, Name name,	Description requirements, Identity identity)
	{
		EmulatorBinding binding = new EmulatorBinding(logger, name, nextPortNumber, requirements, identity, this);
		
		nextPortNumber++;
		
		// TODO remove them
		ports.put(binding.getPortNumber(), binding);
		return binding;
	}

	@Override
	public synchronized Connection connect(Name name, Description requirements, Identity requester)
	{
		PortID destination = null;
		
		// does the name point to a specific binding?
		if(name instanceof EmulatorNeighborName) {
			destination = ((EmulatorNeighborName) name).getDestination();
		} else {
			NameMappingEntry<PortID>[] entries = control.getAddresses(name);
			if(entries.length > 0) {
				destination = entries[0].getAddress();
			}
			// else: no binding for this name known!
		}
		
		EmulatorConnectionEndPoint cep = new EmulatorConnectionEndPoint(this, name, nextPortNumber, destination);
		nextPortNumber++;
		if(destination != null) {
			ports.put(cep.getPortNumber(), cep);
			
			cep.connect();
		} else {
			cep.failed(new NetworkException(this, "No binding for name '" +name +"' known."));
		}
		
		return cep;
	}

	@Override
	public boolean isKnown(Name name)
	{
		NameMappingEntry<PortID>[] addrs = control.getAddresses(name);
		return (addrs.length > 0);
	}

	@Override
	public Description getCapabilities(Name name, Description requirements) throws NetworkException
	{
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Iterable<NeighborName> getNeighbors(Name namePrefix) throws NetworkException
	{
		LinkedList<NeighborName> res = null;
		
		// is layer still valid?
		if(medium != null) {
			res = new LinkedList<NeighborName>();
			
			for(Name name : control.getAllNames()) {
				NameMappingEntry<PortID>[] addresses = control.getAddresses(name);
				
				for(NameMappingEntry<PortID> entry : addresses) {
					res.addLast(new EmulatorNeighborName(name, entry.getAddress()));
				}
			}
		}
		
		return res;
	}

	@Override
	protected void notifyFailure(Throwable failure, EventListener listener)
	{
		// TODO Auto-generated method stub
		
	}
	
	/**
	 * Sends a packet via a medium to a peer.
	 * Method adds the sender address to a packet.
	 */
	public void sendPacket(Packet packet) throws NetworkException
	{
		if(packet != null) {
			if(medium != null) {
				packet.setSender(address);
				
				logger.debug(this, "Send packet " +packet);
				medium.sendPacket(packet);
			} else {
				throw new NetworkException(this, "Can not send packet since layer entity is not connected to medium.");
			}
		}
	}
	
	/**
	 * Receives a single packet from Ethernet and distributes it to
	 * the destination port.
	 */
	public void handleReceivedPacket(Packet packet)
	{
		int destPort = packet.getDestination().getPortNumber();
		Port port = ports.get(destPort);
		if(port != null) {
			logger.log(this, "Packet '" +packet +"' for port '" +port +"' received");
			
			port.handlePacket(packet);
		} else {
			logger.warn(this, "Port " +destPort + " of packet '" +packet +"' unknown. Packet dropped.");
		}
	}	

	/**
	 * Callback from a port that informs the layer about
	 * the closing of the port (by user)
	 */
	public void closed(Port port)
	{
		// remove port from list
		if(ports.remove(port.getPortNumber()) == null) {
			logger.err(this, "Can not close port " +port +", because it is not known.");
		}
	}
	
	/**
	 * Called if medium is removed from simulation
	 */
	public void detached()
	{
		medium = null;
		logger.log(this, "Closing " +ports.size() +" ports due to detaching operation.");
		
		while(!ports.isEmpty()) {
			Integer portNo = ports.keySet().iterator().next();
			Port port = ports.get(portNo);
			
			if(port instanceof EmulatorConnectionEndPoint) {
				((EmulatorConnectionEndPoint) port).close();
			}
			else if(port instanceof ControlPort) {
				((ControlPort) port).close();
			} else {
				((EmulatorBinding) port).closeAllConnections();
			}
			
			ports.remove(portNo);
		}
	}
	
	@Override
	public String toString()
	{
		return address +"@" +medium;
	}
	
	/**
	 * @deprecated For GUI use only!
	 */
	public Iterable<Port> getPorts()
	{
		return ports.values();
	}

	private Address address;
	private Logger logger;
	private EmulatorMedium medium;
	private ControlPort control;
	
	@Viewable("Next port number")
	private int nextPortNumber = CONTROL_PORT +1;
	
	@Viewable("Ports")
	private HashMap<Integer, Port> ports = new HashMap<Integer, Port>();
}
