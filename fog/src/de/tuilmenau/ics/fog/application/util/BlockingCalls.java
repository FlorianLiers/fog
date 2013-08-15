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
import net.rapi.Description;
import net.rapi.Identity;
import net.rapi.Layer;
import net.rapi.Name;
import net.rapi.NetworkException;
import net.rapi.events.ConnectedEvent;
import net.rapi.events.ErrorEvent;
import net.rapi.events.Event;
import de.tuilmenau.ics.fog.util.BlockingEventHandling;

/**
 * Implements blocking calls for {@link Layer}.
 */
public class BlockingCalls
{
	/**
	 * Connects to a service with the given name such as {@link Layer#connect}.
	 * Method blocks until the connection has been set up.
	 */
	public static Connection connect(Layer pLayer, Name pName, Description pDescription, Identity pIdentity) throws NetworkException
	{
		Connection conn = pLayer.connect(pName, pDescription, pIdentity);
		BlockingEventHandling block = new BlockingEventHandling(conn, 1);
		
		// wait for the first event
		Event event = block.waitForEvent();
		
		if(event instanceof ConnectedEvent) {
			if(!conn.isConnected()) {
				throw new NetworkException(BlockingCalls.class, "Connected event but connection is not connected.");
			} else {
				return conn;
			}
		}
		else if(event instanceof ErrorEvent) {
			Exception exc = ((ErrorEvent) event).getException();
			
			if(exc instanceof NetworkException) {
				throw (NetworkException) exc;
			} else {
				throw new NetworkException(pLayer, "Can not connect to " +pName +".", exc);
			}
		}
		else {
			throw new NetworkException(pLayer, "Can not connect to " +pName +" due to " +event);
		}
	}
}
