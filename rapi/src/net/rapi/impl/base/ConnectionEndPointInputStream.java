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

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;

/**
 * Input stream that reads the serialized byte array of objects.
 */
public class ConnectionEndPointInputStream extends ByteArrayInputStream
{
	public ConnectionEndPointInputStream()
	{
		super(new byte[0]);
	}

	@Override
	public synchronized int read()
	{
		int res = super.read();
		
		// current buffer empty?
		if(res < 0) {
			// blocks until buffers changed
			res = flipBuffers();
			
			// if no error occurred, read again
			if(res >= 0) res = read();
		}
		
		return res;
	}
	
	@Override
	public synchronized int read(byte recBuffer[], int offset, int length)
	{
		int res = super.read(recBuffer, offset, length);
		
		// current buffer empty?
		if(res < 0) {
			// blocks until buffers changed
			res = flipBuffers();
			
			// if no error occurred, read again
			if(res >= 0) res = read(recBuffer, offset, length);
		}
		
		return res;
	}
	
	@Override
	public void close() throws IOException
	{
		super.close();
		
		closed = true;
		
		synchronized (buffer) {
			buffer.close();
			buffer.notifyAll();
		}
	}
	
	/**
	 * Replaces the current buffer of the input stream (which is empty)
	 * with the current buffer of the output stream, which contains
	 * the remaining data received via the connection.
	 * 
	 * @return number of new bytes; -1 on error
	 */
	private synchronized int flipBuffers()
	{
		if(!closed) {
			synchronized (buffer) {
				// wait until 
				while(buffer.size() <= 0) {
					try {
						buffer.wait();
					}
					catch (InterruptedException exc) {
						// ignore it
					}
					
					if(!closed) return -1;
				}
				
				// reset read buffer with buffer from output stream
				this.count = buffer.size();
				this.buf = buffer.replaceBuffer();
				this.pos = 0;
				this.mark = 0;
			}
			return this.count;
		} else {
			return -1;
		}
	}
	
	public void addToBuffer(Object data) throws IOException
	{
		if(data != null) {
			if(data instanceof byte[]) {
				buffer.write((byte[]) data);
			} else {
				buffer.write(data.toString().getBytes());
			}
			
			synchronized (buffer) {
				buffer.notify();
			}
		}
	}
	
	private class CEPByteArrayOutputStream extends ByteArrayOutputStream
	{
		/**
		 * Extracts the current puffer from the output stream and replaces it
		 * with a new empty one.
		 * 
		 * @return current buffer
		 */
		public synchronized byte[] replaceBuffer()
		{
			byte[] oldBuf = buf;
			buf = new byte[Math.max(32, count)];
			count = 0;
			
			return oldBuf;
		}
	}
	
	private boolean closed = false;
	private CEPByteArrayOutputStream buffer = new CEPByteArrayOutputStream();
}
