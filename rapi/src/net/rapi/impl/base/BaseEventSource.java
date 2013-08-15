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

import net.rapi.EventSource;
import net.rapi.events.Event;


/**
 * Base class that provides storing and retrieving of events.
 */
public abstract class BaseEventSource implements EventSource
{
	@Override
	public synchronized void registerListener(EventListener observer)
	{
		if(observer != null) {
			if(observers == null) observers = new LinkedList<EventListener>();
			
			synchronized (observers) {
				observers.add(observer);
			}
			
			// relay events occurred previously to new listener
			if(events != null) {
				while(!events.isEmpty()) {
					notifyObservers(events.removeFirst());
				}
				
				events = null;
			}
		}
	}

	@Override
	public synchronized boolean unregisterListener(EventListener observer)
	{
		boolean res = false;
		
		if(observers != null) {
			synchronized (observers) {
				// are we currently in a list iteration process?
				if(loopCounter > 0) {
					// yes! -> remove observer later
					res = observers.contains(observer);
					if(res) {
						//Logging.getInstance().trace(this, "Store delayed removal " +observer);
						
						if(observersDeletion == null) observersDeletion = new LinkedList<EventSource.EventListener>();
						
						observersDeletion.add(observer);
					}
				} else {
					// no! -> remove it immediately
					res = observers.remove(observer);
				}
			}
		}

		return res;
	}

	public synchronized void notifyObservers(Event event)
	{
		if(observers != null) {
			synchronized (observers) {
				if(!observers.isEmpty()) {
					loopCounter++;
					
					for(EventListener obs : observers) {
						try {
							obs.eventOccured(event);
						}
						catch(Error err) {
							notifyFailure(err, obs);
						}
						catch(Exception exc) {
							notifyFailure(exc, obs);
						}
					}
					
					loopCounter--;
					
					// do we have to delete an observer after the iteration?
					if((loopCounter <= 0) && (observersDeletion != null)) {
						//Logging.getInstance().trace(this, "Delayed removal of " +observersDeletion.size() +" observers");
						
						for(EventListener obs : observersDeletion) {
							observers.remove(obs);
						}
						
						observersDeletion = null;
					}
				} else {
					storeEvent(event);
				}
			}
		} else {
			storeEvent(event);
		}
	}
	
	/**
	 * Method is called if an {@link EventListener} throws an exception or error.
	 * Method can be used by derived classes to react on the error.
	 * 
	 * @param failure Thrown exception or error
	 * @param listener Listener that throwed the error
	 */
	protected abstract void notifyFailure(Throwable failure, EventListener listener);
	
	/**
	 * Stores events until listener is registered
	 */
	private void storeEvent(Event event)
	{
		if(events == null) events = new LinkedList<Event>();
		
		events.addLast(event);
	}

	private LinkedList<EventListener> observers = null;
	private LinkedList<Event> events = null;
	private LinkedList<EventListener> observersDeletion = null;
	private int loopCounter = 0;
}
