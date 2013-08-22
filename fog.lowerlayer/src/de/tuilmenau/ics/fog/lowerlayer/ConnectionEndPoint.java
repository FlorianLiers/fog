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

import java.io.Serializable;
import java.util.LinkedList;

import net.rapi.Description;
import net.rapi.Name;
import net.rapi.NetworkException;
import net.rapi.Signature;
import net.rapi.events.ClosedEvent;
import net.rapi.events.ConnectedEvent;
import net.rapi.events.ErrorEvent;
import net.rapi.impl.base.BaseConnectionEndPoint;
import de.tuilmenau.ics.fog.util.Logger;



/**
 * This class implements the Connection interface.
 */
public class ConnectionEndPoint extends BaseConnectionEndPoint
{
	/**
	 * Creates a new Connection end point. Every ConnectionImpl object
	 * must be given a destination ConnectionImpl object before being used.
	 */
	public ConnectionEndPoint(Logger logger, Connection conn, Name setupName)
	{
		super(setupName);
		
		fromApp = new LinkedList<Serializable>();
		
		this.connection = conn;
	}

	public ConnectionEndPoint(Logger logger, Exception error)
	{
		super(error);
	}
	
	@Override
	public void connect()
	{
		if((isLocallyConnected == false) && (connection != null)) {
			isLocallyConnected = true;
			
			// inform peer about accepted connection
			if(!connection.isBroken()) {
				connection.getPeerFor(this).accepted();
			}
			// else: broken; we can not inform peer
			
			// inform app about connection
			accepted();
		}
	}
	
	/**
	 * Called if connection had been accepted by peer.
	 */
	private void accepted()
	{
		// inform listener only if the connection has been approved by both sides
		if(isConnected()) {
			notifyObservers(new ConnectedEvent(this));
		}
	}

	/**
	 * @returns Returns true if this object is still connected, otherwise false
	 */
	@Override
	public boolean isConnected()
	{
		if(isLocallyConnected()) {
			// check if peer is connected, too
			return connection.isEstablished();
		} else {
			return false;
		}
	}

	@Override
	public LinkedList<Signature> getAuthentications()
	{
		if(connection != null) return connection.getAuthentications();
		else return null;
	}

	/**
	 * @return Any requirements for this connection.
	 */
	@Override
	public Description getRequirements()
	{
		if(connection != null) return connection.getRequirements();
		return null;
	}
	
	/**
	 * Closes the connection and notifies the other side that
	 * the connection has been terminated.
	 */
	@Override
	public void close()
	{
		if(isLocallyConnected) {
			isLocallyConnected = false;
			
			// inform local app
			notifyObservers(new ClosedEvent(this));
			
			connection.closed(this);
		}
		
		// inform peer about closing if it is connected
		// in special this situation appears if a new connection
		// is not accepted by its peer
		if(connection != null) {
			if(!connection.isBroken()) {
				ConnectionEndPoint peer = connection.getPeerFor(this);
				if(peer != null) {
					if(peer.isLocallyConnected) peer.close();
				}
			}
			// else: broken; we can not inform peer
		}
	}
	
	@Override
	protected void sendDataToPeer(Serializable data) throws NetworkException
	{
		fromApp.add(data);
		
		connection.packetAvailable(this);
	}

	/**
	 * Used to retrieve buffered packets ready for sending to remote peer
	 * 
	 * @return The first packet in the send buffer or null if no available
	 */
	public Serializable getOutboundPacket()
	{
		if(!fromApp.isEmpty()) return fromApp.removeFirst();
		else return null;
	}
	
	/**
	 * @return true if there are packets waiting to be sent, otherwise false.
	 */
	public boolean hasPacket()
	{
		return !fromApp.isEmpty();
	}
	
	public boolean isLocallyConnected()
	{
		return isLocallyConnected;
	}
	
	public void setError(Exception exc)
	{
		notifyObservers(new ErrorEvent(exc, this));
		
		close();
	}
	
	public String toString()
	{
		return "BusCEP" +hashCode();
	}
	
	@Override
	protected void notifyFailure(Throwable failure, EventListener listener)
	{
		// TODO Auto-generated method stub
		
	}
	
	private Connection connection;
	private LinkedList<Serializable> fromApp;
	private boolean isLocallyConnected = false;
}
