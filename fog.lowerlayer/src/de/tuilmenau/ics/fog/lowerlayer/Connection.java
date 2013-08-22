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

import de.tuilmenau.ics.fog.authentication.IdentityManagement;
import de.tuilmenau.ics.fog.exceptions.AuthenticationException;
import de.tuilmenau.ics.fog.topology.Breakable.Status;

import net.rapi.Description;
import net.rapi.Identity;
import net.rapi.Name;
import net.rapi.NetworkException;
import net.rapi.Signature;

/**
 * Represents connection through bus layer.
 * Contains two connection end points and the meta-informations (requ., authn.). 
 */
public class Connection
{
	public Connection(BusEntity entity, BindingImpl binding, BusMedium medium, Identity requester, Name setupName, Description requ)
	{
		this.binding = binding;
		this.medium = medium;
		this.requester = requester;
		this.requ = requ; // parameter is already a copy
		this.auth = entity.getNode().getAuthenticationService();
		
		peer1 = new ConnectionEndPoint(medium.getLogger(), this, setupName);
		peer2 = new ConnectionEndPoint(medium.getLogger(), this, null);
	}
	
	public ConnectionEndPoint getPeer1()
	{
		return peer1;
	}
	
	public ConnectionEndPoint getPeer2()
	{
		return peer2;
	}

	/**
	 * Returns the other peer entry from connection.
	 * 
	 * @param peer The peer asking for the remote one
	 * @return The other peer (!= null)
	 */
	public ConnectionEndPoint getPeerFor(ConnectionEndPoint peer)
	{
		if(peer == peer1) return peer2;
		if(peer == peer2) return peer1;
		
		throw new RuntimeException(this +" - Wrong peer " +peer +" for connection.");
	}
	
	public BindingImpl getBinding()
	{
		return binding;
	}
	
	public LinkedList<Signature> getAuthentications()
	{
		if((requester != null) && (auth != null)) {
			try {
				LinkedList<Signature> res = new LinkedList<Signature>();
				Signature sig = auth.createSignature(this.toString(), requester);
				
				res.add(sig);				
				return res;
			}
			catch(AuthenticationException exc) {
				return null;
			}
		} else {
			return null;
		}
	}
	
	public Description getRequirements()
	{
		return requ;
	}
	
	public boolean isEstablished()
	{
		return peer1.isLocallyConnected() && peer2.isLocallyConnected();
	}
	
	public boolean isBroken()
	{
		if(medium != null) {
			return medium.isBroken() != Status.OK;
		} else {
			return true;
		}
	}
	
	public boolean hasPacket()
	{
		return peer1.hasPacket() || peer2.hasPacket();
	}

	public boolean packetAvailable(ConnectionEndPoint peer) throws NetworkException
	{
		// if no peer is given, try to find a peer of the
		// connection, which has packets to send
		if(peer == null) {
			if(peer1.hasPacket()) peer = peer1;
			else {
				if(peer2.hasPacket()) peer = peer2;
			}
			
			if(peer == null) return false;
		}
		
		if((peer == peer1) || (peer == peer2)) {
			if(medium != null) {
				Status mediumStatus = medium.isBroken();
				if(mediumStatus == Status.OK) {
					return medium.packetAvailable(this, peer);
				} else {
					throw new NetworkException(this, "Layer is broken (status=" +mediumStatus +")");
				}
			} else {
				throw new NetworkException(this, "Can not signal available packet since connection is already closed.");
			}
		} else {
			throw new NetworkException(this, "Invalid peer " +peer +" for connection.");
		}
	}
	
	public String toString()
	{
		return "Conn:" +peer1 +"<->" +peer2;
	}
	
	public synchronized void closed(ConnectionEndPoint connectionEndPoint)
	{
		if(!isEstablished()) {
			if(medium != null) {
				medium.getLayer().connectionClosed(this);
				
				medium = null;
			}
		}
	}
	
	private IdentityManagement auth;
	
	private BindingImpl binding;
	private BusMedium medium;
	private Identity requester;
	private Description requ;
	private ConnectionEndPoint peer1;
	private ConnectionEndPoint peer2;	
}
