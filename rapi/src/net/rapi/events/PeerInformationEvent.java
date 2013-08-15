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
package net.rapi.events;

import net.rapi.EventSource;
import net.rapi.Name;

public class PeerInformationEvent extends Event
{
	public PeerInformationEvent(EventSource source, Name peer, boolean appeared)
	{
		super(source);
		
		this.peer = peer;
		this.appeared = appeared;
	}
	
	public Name getPeer()
	{
		return peer;
	}
	
	public boolean isAppeared()
	{
		return appeared;
	}
	
	private Name peer;
	private boolean appeared;
}
