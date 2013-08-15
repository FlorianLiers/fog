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


/**
 * A binding is an service offering to all peers with access to a layer.
 * It can be created at a layer via the method {@link Layer#bind}.
 * Others can create a {@link Connection} to a binding via {@link Layer}
 * and with the name of the binding.
 * A binding provides methods for terminating the service offering and
 * for retrieving incoming {@link Connection}s for it.
 */
public interface Binding extends EventSource
{
	/**
	 * @return If the binding is still valid and supported by the {@link Layer}
	 */
	public boolean isActive();
	
	/**
	 * Requests the next new incoming connection for a binding.
	 * The method does not block and will return null, if no connection is available. 
	 * 
	 * @return Reference to a new incoming connection or null if none waiting
	 */
	public Connection getIncomingConnection();
	
	/**
	 * @return Number of new connections waiting in queue
	 */
	public int getNumberWaitingConnections();
	
	/**
	 * @return Name used for this binding
	 */
	public Name getName();
	
	/**
	 * @return Requirements for all {@link Connection}s to this binding (null if no additional requirements)
	 */
	public Description getRequirements();
	
	/**
	 * @return Identity of creator of this binding (null if not known)
	 */
	public Identity getIdentity();
	
	/**
	 * Closes registration and makes the binding unaccessible for peers.
	 * The method does not block.
	 */
	public void close();
}
