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
package de.tuilmenau.ics.fog.application.util;

import net.rapi.Connection;
import net.rapi.events.ClosedEvent;
import net.rapi.events.ConnectedEvent;
import net.rapi.events.DataAvailableEvent;
import net.rapi.events.ErrorEvent;
import net.rapi.events.Event;
import de.tuilmenau.ics.fog.application.ApplicationEventHandler;
import de.tuilmenau.ics.fog.ui.Logging;
import de.tuilmenau.ics.fog.util.Logger;


public class Session extends ApplicationEventHandler<Connection> implements ReceiveCallback
{
	public Session(boolean ownThread, Logger logger, ReceiveCallback callback)
	{
		super(ownThread);
	
		if(logger == null) {
			Logging.getInstance().warn(this, "No logger specified; using global logger.");
			this.logger = Logging.getInstance();
		} else {
			this.logger = logger;
		}
		
		this.callback = callback;
	}
	
	@Override
	protected void handleEvent(Event event) throws Exception
	{
		if(event instanceof ConnectedEvent) {
			connected();
		}
		else if(event instanceof ClosedEvent) {
			closed();
		}
		else if(event instanceof DataAvailableEvent) {
			Object tData = getConnection().read();
			receiveData(tData);
		}
		else if(event instanceof ErrorEvent) {
			error(((ErrorEvent) event).getException());
		}
		else {
			unknownEvent(event);
		}
	}
	
	public void connected()
	{
		if(callback != null) callback.connected();
	}
	
	@Override
	public boolean receiveData(Object pData)
	{
		if(callback != null) return callback.receiveData(pData);
		
		return false;
	}

	@Override
	public void closed()
	{
		if(callback != null) {
			callback.closed();
		} else {
			stop();
		}
	}
	
	@Override
	public void error(Throwable pCause)
	{
		if(callback != null) {
			callback.error(pCause);
		} else {
			stop();
		}
	}

	@Override
	public void stop()
	{
		Connection conn = getEventSource();
		if(conn != null) {
			conn.close();
		}
		
		super.stop();
	}
	
	/**
	 * For subclasses to handle events unknown by base class.
	 */
	protected void unknownEvent(Event event)
	{
		getLogger().trace(this, "Ignoring unknown event: " +event);
	}

	/**
	 * @return If the underlying connection is set up
	 */
	public boolean isConnected()
	{
		Connection conn = getConnection();
		if(conn != null) {
			return conn.isConnected();
		} else {
			return false;
		}
	}
	
	/**
	 * A helper method for avoiding the method name <code>getEventSource()</code>, which
	 * is not meaningful in this context.
	 */
	public Connection getConnection()
	{
		return getEventSource();
	}
	
	public Logger getLogger()
	{
		return logger;
	}

	protected Logger logger;
	private ReceiveCallback callback;
}
