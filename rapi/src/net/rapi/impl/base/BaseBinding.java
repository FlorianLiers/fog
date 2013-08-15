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

import java.util.LinkedList;

import net.rapi.Binding;
import net.rapi.Connection;
import net.rapi.Description;
import net.rapi.Identity;
import net.rapi.Name;
import net.rapi.events.ErrorEvent;
import net.rapi.events.NewConnectionEvent;

/**
 * This class provides a base implementation for the {@link Binding} interface.
 */
public abstract class BaseBinding extends BaseEventSource implements Binding
{
	public BaseBinding(Name name, Description requirements, Identity identity)
	{
		this.name = name;
		this.requ = requirements;
		this.identity = identity;
	}

	public BaseBinding(Name name, Exception error)
	{
		this.name = name;

		notifyObservers(new ErrorEvent(error, this));
	}
	
	@Override
	public synchronized Connection getIncomingConnection()
	{
		if(newConns != null) {
			if(!newConns.isEmpty()) {
				return newConns.removeFirst();
			}
		}

		return null;
	}
	
	@Override
	public synchronized int getNumberWaitingConnections()
	{
		if(newConns != null) {
			return newConns.size();
		} else {
			return 0;
		}
	}

	public synchronized boolean addIncomingConnection(Connection conn)
	{
		if(isActive()) {
			if(newConns == null) newConns = new LinkedList<Connection>();
			newConns.addLast(conn);
			
			// inform app about new connection
			notifyObservers(new NewConnectionEvent(this));
			return true;
		} else {
			return false;
		}
	}

	@Override
	public Name getName()
	{
		return name;
	}

	@Override
	public Description getRequirements()
	{
		return requ;
	}
	
	@Override
	public Identity getIdentity()
	{
		return identity;
	}
	
	@Override
	public synchronized void close()
	{
		newConns = null;
	}
	
	/**
	 * Has to be called if the binding is no longer
	 * actively supported by a layer.
	 * 
	 * @param exc Error description
	 */
	public void setError(Exception exc)
	{
		close();
		notifyObservers(new ErrorEvent(exc, this));
	}
	
	private Name name;
	private Description requ;
	private Identity identity;
	
	private LinkedList<Connection> newConns = null; /* lazy creation */
}
