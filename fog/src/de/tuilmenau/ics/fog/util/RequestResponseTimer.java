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
package de.tuilmenau.ics.fog.util;

import de.tuilmenau.ics.fog.EventHandler;
import de.tuilmenau.ics.fog.IEvent;
import de.tuilmenau.ics.fog.packets.Packet;

/**
 * Sending a request and waiting for an answer.
 * If no answer is received within a time period, the request is send once again.
 * If no answer is received within the overall timeout, an exception handling is triggered.
 */
public abstract class RequestResponseTimer extends Timer implements IEvent
{
	public RequestResponseTimer(EventHandler timeBase, Packet packet, int retries, double timeoutSec)
	{
		super(timeBase, timeoutSec / (double)(retries +1));
		
		this.retries = retries;
		this.packet = packet;
	}
	
	/**
	 * Sends a packet for the first time and starts the timer for retries.
	 */
	public void start()
	{
		try {
			sendRequest();
		}
		catch(Exception exc) {
			// ignore it
		}
		
		super.start();
	}
	
	/**
	 * @return Indicates if a retry is required; if false, the request is already answered
	 */
	protected boolean retryRequired()
	{
		return true;
	}
	
	/**
	 * (Re-)Sends the packet
	 */
	protected abstract void sendRequest() throws Exception;
	
	/**
	 * Called once, if a timeout occures
	 */
	protected abstract void timeout();
	
	
	@Override
	public final void fire()
	{
		if(retryRequired()) {
			if(retries > 0) {
				retries--;
				
				try {
					sendRequest();
				}
				catch(Exception exc) {
					// ignore it
				}
				
				// do it after sending the message. If we are not executed in the event loop, we would risk to be executed immediately otherwise.
				super.start();
			} else {
				// no retries left!
				timeout();
			}
		}
		// else: answer received; do nothing
	}
	
	protected Packet getPacket()
	{
		return packet;
	}
	
	private int retries;
	private Packet packet;
}
