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

import java.io.Serializable;


/**
 * Namespaces for application names.
 */
public class Namespace implements Serializable
{
	private static final long serialVersionUID = 5923391131934016194L;
	
	
	/**
	 * Creates a name space object based on a string, which
	 * is defining the prefix for all names of this name space.
	 * 
	 * @param pName non-null name of the name space
	 */
	public Namespace(String pName)
	{
		mName = pName.toLowerCase();
		mIsAppNamespace = false;
	}
	
	public Namespace(String pName, boolean pIsAppNamespace)
	{
		this(pName);
		
		mIsAppNamespace = pIsAppNamespace;
	}
	
	@Override
	public int hashCode()
	{
		return mName.hashCode();
	}
	
	@Override
	public boolean equals(Object pObj)
	{
		if(pObj != null) {
			if(pObj instanceof String) {
				return mName.equalsIgnoreCase((String) pObj);
			}
			
			if(pObj instanceof Namespace) {
				return mName.equals(pObj.toString());
			}
		}
		
		return false;
	}
	
	@Override
	public String toString()
	{
		return mName;
	}
	
	/**
	 * @return Indicates if the namespace is an application one; if no, it is a namespace used by FoG internal functions (e.g. routing).
	 */
	public boolean isAppNamespace()
	{
		return mIsAppNamespace;
	}
	
	private String mName;
	private boolean mIsAppNamespace;
}
