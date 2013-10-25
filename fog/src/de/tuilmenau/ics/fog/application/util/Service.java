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

import java.util.LinkedList;

import net.rapi.Binding;
import net.rapi.Connection;
import net.rapi.Description;
import net.rapi.Name;
import net.rapi.Signature;
import net.rapi.events.ErrorEvent;
import net.rapi.events.Event;
import net.rapi.events.NewConnectionEvent;

import de.tuilmenau.ics.fog.application.ApplicationEventHandler;


/**
 * Represents the part of an application handling the issues of a binding.
 * It processes the events from a binding and handles incoming connections.
 */
public class Service extends ApplicationEventHandler<Binding> implements ServerCallback
{
	public Service(boolean ownThread, ServerCallback callback)
	{
		super(ownThread);
		
		this.callback = callback;
	}
	
	@Override
	protected void handleEvent(Event event) throws Exception
	{
		if(event instanceof NewConnectionEvent) {
			NewConnectionEvent newConnEvent = (NewConnectionEvent) event;
			Connection cep;
			do {
				cep = newConnEvent.getBinding().getIncomingConnection();
				if(cep != null) {
					// ask if service accepts this connection
					if(openAck(cep.getAuthentications(), cep.getRequirements(), newConnEvent.getBinding().getName())) {
						cep.connect();
						
						// inform service about new connection
						newConnection(cep);
					} else {
						cep.close();
					}
				}
			}
			while(cep != null);
		}
		else if(event instanceof ErrorEvent) {
			error((ErrorEvent) event);
		}
		// else: ignore unknown event 
	}

	@Override
	public boolean openAck(LinkedList<Signature> pAuths, Description pDescription, Name pTargetName)
	{
		if(callback != null) {
			return callback.openAck(pAuths, pDescription, pTargetName);
		} else {
			// default behavior if children do not overwrite function
			return true;
		}
	}
	
	@Override
	public void newConnection(Connection pConnection)
	{
		if(callback != null) {
			callback.newConnection(pConnection);
		} else {
			// We do not know how to handle it. Child classes
			// have to override this function if required.
			// Therefore, we close the connection.
			pConnection.close();
		}
	}
	
	@Override
	public void error(ErrorEvent pCause)
	{
		if(callback != null) {
			callback.error(pCause);
		}
		// else: do nothing
	}
	
	@Override
	public void stop()
	{
		getEventSource().close();
		super.stop();
	}
	
	private ServerCallback callback;
}
