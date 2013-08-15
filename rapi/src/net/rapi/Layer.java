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
 * A layer offers the possibility to announce own services to other peers of
 * a layer and to access services from others. Moreover, the class provides
 * methods for retrieving neighbor and capability information that may guide
 * the usage of a layer.
 * 
 * All internal issues of the layer, like addresses, protocols, and routes,
 * are hidden. Users of a layer are not allowed to get knowledge about such
 * issues in order to preserve the encapsulation of a layer.
 */
public interface Layer extends EventSource
{
	/**
	 * Registers an entity with a given name at the layer. Afterwards,
	 * clients can connect to this service by using the same name.
	 * This method does not block. Errors are indicated via events
	 * of the {@link Binding} object.
	 * 
	 * @param parentSocket Optional parent connection (optional; might be {@code null} if no)
	 * @param name Name for the service
	 * @param requirements Description of the service requirements. These requirements are enforced for all connections to this binding.
	 * @param identity Optional identity of the requester of the registration
	 * @return Reference to the service registration ({@code != null})
	 * @throws NetworkException On error
	 */
	public Binding bind(Connection parentSocket, Name name, Description requirements, Identity identity);

	/**
	 * Connects to a {@link Binding} with the given name.
	 * The method does not block. The establishment of a connection is
	 * signaled with an event. All other feedbacks are given via events
	 * as well. In particular, the error exceptions are not triggered by the
	 * method but handed over via events of the {@link Connection} object.
	 * 
	 * @param name Name of the {@link Binding}, to which should be connected to 
	 * @param requirements Description of the requirements of the caller for the connection
	 * @param requester Optional identity of the caller. It is used for signing the connect request.
	 * @return Reference for the connection ({@code != null})
	 */
	public Connection connect(Name name, Description requirements, Identity requester);

	/**
	 * Checks whether or not a {@link Binding} with this name is known by the layer.
	 * That does not imply that {@link #connect} can construct a connection to this name.
	 * 
	 * @param name Name to search for
	 * @return {@code true}, if name is known; {@code false} otherwise
	 */
	public boolean isKnown(Name name);
	
	/**
	 * Determines the capabilities of this layer. Since the whole set of capabilities may be
	 * too large, the request can be filtered. Possible filters are the destination name and some
	 * test requirements. If such filters are present, the method just determines the capabilities
	 * regarding this destination and these test requirements. 
	 * 
	 * @param name Optional destination name to focus the capability analysis
	 * @param requirements Optional test requirements (if, e.g., maximum bandwidth is included in the test requirements, the method will determine the possible bandwidth) 
	 * @return Capabilities of the layer ({@code != null})
	 * @throws NetworkException On error (e.g. filter invalid)
	 */
	public Description getCapabilities(Name name, Description requirements) throws NetworkException;
	
	/**
	 * Determines neighbor information about the {@link Binding}s reachable
	 * via this layer.
	 * 
	 * @param namePrefix Optional filter for the request. If present, only neighbors with a name having this prefix will be listed.
	 * @return List of reachable neighbors or null if lower layer is broken
	 */
	public Iterable<NeighborName> getNeighbors(Name namePrefix) throws NetworkException;

}
