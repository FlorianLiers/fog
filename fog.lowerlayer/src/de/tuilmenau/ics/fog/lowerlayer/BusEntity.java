/*******************************************************************************
 * Forwarding on Gates Simulator/Emulator - Lower layer
 * Copyright (C) 2013, Integrated Communication Systems Group, TU Ilmenau.
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
package de.tuilmenau.ics.fog.lowerlayer;

import java.util.LinkedList;

import de.tuilmenau.ics.fog.packets.Packet;
import de.tuilmenau.ics.fog.topology.Node;
import de.tuilmenau.ics.fog.transfer.ForwardingElement;

import net.rapi.Binding;
import net.rapi.Connection;
import net.rapi.Description;
import net.rapi.Identity;
import net.rapi.Name;
import net.rapi.Layer;
import net.rapi.NeighborName;
import net.rapi.NetworkException;
import net.rapi.events.ConnectedEvent;
import net.rapi.events.DisconnectedEvent;
import net.rapi.events.PeerInformationEvent;
import net.rapi.impl.base.BaseEventSource;


/**
 * Object represents the point of attachment of a node to a bus.
 * It is required in order to customize the requests and actions taken by a node on a bus.
 * 
 * Extends ForwardingElement just because of RoutingService and GUI reasons. Only ForwardingElements
 * can be stored in the routing service and only them can be drawn in the GUI.
 */
public class BusEntity extends BaseEventSource implements Layer, ForwardingElement
{
	public BusEntity(BusLayer central, Node node)
	{
		this.central = central;
		this.node = node;
	}

	@Override
	public LayerStatus getStatus()
	{
		return central.getStatus();
	}

	@Override
	public Binding bind(Connection parentSocket, Name name, Description requ, Identity identity)
	{
		return central.bind(this, name, requ, identity);
	}
	
	public LinkedList<Binding> getBindings()
	{
		return central.getBindings(this);
	}
	
	public Node getNode()
	{
		return node;
	}

	@Override
	public Connection connect(Name name, Description requirements, Identity identity)
	{
		return central.connect(this, name, requirements, identity);
	}

	@Override
	public boolean isKnown(Name name)
	{
		return central.getBinding(name) != null;
	}

	@Override
	public Description getCapabilities(Name name, Description requirements) throws NetworkException
	{
		// TODO
		return null;
	}

	@Override
	public Iterable<NeighborName> getNeighbors(Name namePrefix) throws NetworkException
	{
		return central.getNeighbors(namePrefix);
	}

	public void deleted()
	{
		neighborPrefix = null;
		central = null;
		node = null;
	}
	
	/**
	 * Just implemented for RoutingService and GUI reasons.
	 * Method MUST NOT be used at all.
	 */
	@Override
	public void handlePacket(Packet pPacket, ForwardingElement pLastHop)
	{
		throw new RuntimeException("Method " +BusEntity.class +".handlePacket MUST NOT be used.");
	}
	
	public void informAboutNewNeighbor(NeighborName peer, boolean appeared)
	{
		if(neighborPrefix != null) {
			for(Name name : neighborPrefix) {
				if(name != null) {
					if(name.equals(peer)) {
						notifyObservers(new PeerInformationEvent(this, peer, appeared));
					}
				} else {
					// if filter name not given, notify observer about everything
					notifyObservers(new PeerInformationEvent(this, peer, appeared));
				}
			}
		}
	}
	
	public void connected()
	{
		notifyObservers(new ConnectedEvent(this));
	}

	public void disconnected()
	{
		notifyObservers(new DisconnectedEvent(this));
	}
	
	public String toString()
	{
		return node +"@" +central;
	}
	
	@Override
	protected void notifyFailure(Throwable failure, EventListener listener)
	{
		// TODO Auto-generated method stub
		
	}

	private BusLayer central;
	private Node node;
	private LinkedList<Name> neighborPrefix; /* lazy creation */
}

