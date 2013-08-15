/******************************************************************************
 * Recursive API
 * Copyright 2013 Integrated Communication Systems Group, TU Ilmenau.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *****************************************************************************/
package net.rapi.impl.base;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.Serializable;
import java.util.LinkedList;

import net.rapi.Connection;
import net.rapi.Name;
import net.rapi.NetworkException;
import net.rapi.events.DataAvailableEvent;
import net.rapi.events.ErrorEvent;


/**
 * This class implements the Connection interface.
 */
public abstract class BaseConnectionEndPoint extends BaseEventSource implements Connection
{
	/**
	 * Creates a new Connection end point for a successfully created
	 * connection or for a connection that is currently in the process
	 * of creation.
	 */
	public BaseConnectionEndPoint(Name bindingName)
	{
		this.bindingName = bindingName;
	}

	/**
	 * Create a CEP for a connection that can not be established.
	 */
	public BaseConnectionEndPoint(Exception error)
	{
		notifyObservers(new ErrorEvent(error, this));
	}

	@Override
	public Name getBindingName()
	{
		return bindingName;
	}

	/**
	 * Final due to call to write in internal anonymous class (see
	 * {@link #getOutputStream()}. The call is delegated to the method
	 * {@link #sendDataToPeer(Serializable)}.
	 */
	@Override
	public final void write(Serializable data) throws NetworkException
	{
		if(!isConnected()) throw new NetworkException("Connection is broken.");
		
		sendDataToPeer(data);
	}
	
	/**
	 * Called if data should be send to a peer or multiple peers.
	 */
	protected abstract void sendDataToPeer(Serializable data) throws NetworkException;

	@Override
	public OutputStream getOutputStream() throws IOException
	{
		if(toNetStream == null) {
			toNetStream = new OutputStream() {
				@Override
				public void write(int value) throws IOException
				{
					try {
						BaseConnectionEndPoint.this.write(new byte[] { (byte)value });
					}
					catch(NetworkException exc) {
						throw new IOException(exc);
					}
				}
				
				public synchronized void write(byte b[], int off, int len) throws IOException
				{
					if(b != null) {
						try {
							// Copy array since some apps will reuse b in order
							// to send the next data chunk! That copy can only be
							// avoided, if the calls behind "write" really do a
							// deep copy of the packet. However, the payload will
							// only be copied if the packet is send through a real
							// lower layer. In pure simulation scenarios that never
							// happens.
							byte[] copyB = new byte[len];
							System.arraycopy(b, off, copyB, 0, len);
							
							BaseConnectionEndPoint.this.write(copyB);
						}
						catch(NetworkException exc) {
							throw new IOException(exc);
						}
					}
				}
				
				@Override
				public void flush() throws IOException
				{
					// nothing to do
				}

			};
		}

		return toNetStream;
	}

	@Override
	public synchronized Object read() throws NetworkException
	{
		if(toAppStream != null) {
			throw new NetworkException(this, "Receiving is done via input stream. Do not call Connection.read."); 
		}
		
		if(toAppBuffer != null) {
			if(!toAppBuffer.isEmpty()) {
				// return data from buffer although the connection might be closed
				return toAppBuffer.removeFirst();
			}
		}
		
		if(!isConnected()) {
			throw new NetworkException("Connection is broken.");
		} else {
			// connection alive but no data available
			return null;
		}
	}
	
	@Override
	public synchronized InputStream getInputStream() throws IOException
	{
		if(toAppStream == null) {
			toAppStream = new ConnectionEndPointInputStream();
			
			// if there is already some data, copy it to stream
			// and delete buffer
			if(toAppBuffer != null) {
				for(Object obj : toAppBuffer) {
					toAppStream.addToBuffer(obj);
				}
				
				toAppBuffer = null;
			}
		}

		return toAppStream;
	}
	
	@Override
	public int available()
	{
		if(toAppStream != null) {
			return toAppStream.available();
		}
		
		if(toAppBuffer != null) {
			return toAppBuffer.size();
		}
		
		// no data or connection not open
		return 0;
	}
	
	/**
	 * Used to receive incoming packets from remote peer for local one
	 */
	public synchronized void storeDataForApp(Serializable data) throws IOException
	{
		if(toAppStream != null) {
			// deliver via stream
			toAppStream.addToBuffer(data);
		} else {
			// deliver via buffer
			if(toAppBuffer == null) {
				// lazy creation
				toAppBuffer = new LinkedList<Serializable>();
			}
			
			toAppBuffer.addLast(data);
		}
		
		// inform local app
		notifyObservers(new DataAvailableEvent(this));
	}
	
	/**
	 * Empties all buffers and removes all streams
	 */
	protected synchronized void cleanup()
	{
		try {
			if(toAppStream != null) toAppStream.close();
			if(toNetStream != null) toNetStream.close();
			
			toAppStream = null;
			toNetStream = null;
		} catch (IOException tExc) {
			// ignore exception
		}
		
		toAppBuffer = null;
	}
	
	/**
	 * Has to be called if a peer terminated the connection
	 * due to an error.
	 * 
	 * @param exc Description of error
	 */
	public void setError(Exception exc)
	{
		notifyObservers(new ErrorEvent(exc, this));
		
		close();
	}
	
	private Name bindingName;
	
	/* lazy created buffers/streams */
	private LinkedList<Serializable> toAppBuffer;
	private ConnectionEndPointInputStream toAppStream;
	private OutputStream toNetStream;
}
