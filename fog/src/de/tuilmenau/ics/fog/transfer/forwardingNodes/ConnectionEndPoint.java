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
package de.tuilmenau.ics.fog.transfer.forwardingNodes;

import java.io.Serializable;
import java.util.LinkedList;

import net.rapi.Description;
import net.rapi.Name;
import net.rapi.NetworkException;
import net.rapi.Signature;
import net.rapi.events.ClosedEvent;
import net.rapi.events.ConnectedEvent;
import net.rapi.events.ServiceDegradationEvent;
import net.rapi.impl.base.BaseConnectionEndPoint;
import de.tuilmenau.ics.fog.Config;
import de.tuilmenau.ics.fog.packets.Packet;
import de.tuilmenau.ics.fog.packets.PleaseCloseConnection;
import de.tuilmenau.ics.fog.packets.PleaseUpdateRoute;
import de.tuilmenau.ics.fog.util.Logger;


public class ConnectionEndPoint extends BaseConnectionEndPoint
{
	public ConnectionEndPoint(Name bindingName, Logger logger, LinkedList<Signature> authentications)
	{
		super(bindingName);
		
		this.logger = logger;
		this.authentications = authentications;
	}
	
	public ConnectionEndPoint(Exception error)
	{
		super(error);
	}
	
	@Override
	public void connect()
	{
		if(isConnected()) {
			notifyObservers(new ConnectedEvent(this));
		} else {
			// TODO
		}
	}
	
	public void setForwardingNode(ClientFN forwardingNode)
	{
		this.forwardingNode = forwardingNode;
	}
	
	public ClientFN getForwardingNode()
	{
		return forwardingNode;
	}
	
	@Override
	public boolean isConnected()
	{
		if(forwardingNode != null) {
			return forwardingNode.isConnected();
		} else {
			return false;
		}
	}
	
	@Override
	public LinkedList<Signature> getAuthentications()
	{
		return authentications;
	}
	
	@Override
	public Description getRequirements()
	{
		if(forwardingNode != null) {
			return forwardingNode.getDescription();
		} else {
			return null;
		}
	}
	
	@Override
	public void sendDataToPeer(Serializable data) throws NetworkException
	{
		if(data != null) {
			if(forwardingNode != null) {
				// just a method to test update route by manual command
				if(Config.Connection.ENABLE_UPDATE_ROUTE_BY_COMMAND) {
					if(data.equals(Config.Connection.UPDATE_ROUTE_COMMAND)) {
						data = new PleaseUpdateRoute(true);
					}
				}
				
				Packet packet = new Packet(data);
				forwardingNode.send(packet);
			} else {
				throw new NetworkException(this, "Connection end point is not connected. Write operation failed.");
			}
		}
	}
	
	/**
	 * Called by higher layer to close socket.
	 */
	@Override
	public void close()
	{
		logger.log(this, "Closing " + this);
		if(isConnected()) {
			// inform peer about closing operation
			try {
				write(new PleaseCloseConnection());
			}
			catch(NetworkException exc) {
				logger.err(this, "Can not send close gate message. Closing without it.", exc);
			}
			
			forwardingNode.closed();
		}else {
			logger.err(this, "CEP cannot be closed because it is not connected");
		}
			
		
		cleanup();
	}
	
	/**
	 * Called by forwarding node, if it was closed
	 */
	public void closed()
	{
		cleanup();
		
		// inform higher layer about closing
		notifyObservers(new ClosedEvent(this));
	}
	
	public void informAboutNetworkEvent()
	{
		notifyObservers(new ServiceDegradationEvent(this));
	}
	
	@Override
	public String toString()
	{
		if(forwardingNode != null) {
			return super.toString() +"@" +forwardingNode.getEntity();
		} else {
			return super.toString();
		}
	}
	
	@Override
	protected void notifyFailure(Throwable failure, EventListener listener)
	{
		logger.warn(this, "Ignoring failure from event listener " +listener, failure);
	}
	
	private Logger logger;
	private ClientFN forwardingNode;
	private LinkedList<Signature> authentications;
}
