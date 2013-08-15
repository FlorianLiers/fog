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
package net.rapi;

import net.rapi.events.Event;


/**
 * Base class for objects that informs others about asynchronous events.
 */
public interface EventSource
{
	/**
	 * Registers observer for the event source.
	 * 
	 * @param observer entity, which will be informed about event
	 */
	public void registerListener(EventListener observer);
	
	/**
	 * Unregisters observer for the event source.
	 * 
	 * @param observer entity, which should be removed from the observer list
	 * @return true, if observer had been successfully unregistered; false otherwise
	 */
	public boolean unregisterListener(EventListener observer);
	
	/**
	 * Inferface for observer of the event source
	 */
	public interface EventListener
	{
		/**
		 * Called if an event is occuring at the event source.
		 * This callback method is NOT allowed to block.
		 * It MUST return as fast as possible since it it
		 * executed in the thread of the event source.
		 * 
		 * @param source Source of the event
		 * @param event Event itself
		 * @throws Exception On error; Exceptions are ignored by the caller. 
		 */
		public void eventOccured(Event event) throws Exception;
	}

}
