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
import java.util.Iterator;
import java.util.LinkedList;

import net.rapi.properties.NonFunctionalRequirementsProperty;
import net.rapi.properties.OrderedProperty;
import net.rapi.properties.Property;
import net.rapi.properties.PropertyException;


public class Description implements Iterable<Property>, Serializable
{
	private static final long serialVersionUID = -8096508525836787147L;
	private static final Iterator<Property> nullIterator = new Iterator<Property>() {
		@Override
		public boolean hasNext()
		{
			return false;
		}

		@Override
		public Property next()
		{
			return null;
		}

		@Override
		public void remove()
		{
		}
	};

	public Description()
	{
		mProperties = null;
	}
	
	public Description(Description original)
	{
		mProperties = new LinkedList<Property>(original.mProperties);
	}
	
	/**
	 * Appends a description to an existing one 
	 * 
	 * @param pToAppend The description which has to be appended
	 * @return none
	 */
	public void append(Description pToAppend) throws PropertyException
	{
		if (pToAppend != null)
		{
			for(Property tProperty: pToAppend)
			{
				add(tProperty);
			}
		}
	}
	
	public void set(Property pProperty)
	{
		if(pProperty != null) {
			if(mProperties == null) {
				mProperties = new LinkedList<Property>();
			} else {
				Property tExisting = get(pProperty.getClass());
				
				if(tExisting != null) {
					mProperties.remove(tExisting);
				}
			}
			
			mProperties.add(pProperty);
		}
	}
	
	public void add(Property pProperty) throws PropertyException
	{
		if(pProperty != null) {
			Property tExisting = null;
			
			if(mProperties == null) {
				mProperties = new LinkedList<Property>();
			} else {
				tExisting = get(pProperty.getClass());
			}
			
			if(tExisting == null) {
				mProperties.add(pProperty);
			} else {
				tExisting.fuse(pProperty);
			}
		}
	}
	
	public boolean remove(Property pProperty)
	{
		if(mProperties != null)
			return mProperties.remove(pProperty);
		else
			return true;
	}
	
	public boolean isEmpty()
	{
		if(mProperties != null)
			return mProperties.isEmpty();
		else
			return true;
	}
	
	public boolean isBestEffort()
	{
		if(mProperties != null) {
			for(Property prop : mProperties) {
				if(prop instanceof NonFunctionalRequirementsProperty) {
					if(!((NonFunctionalRequirementsProperty) prop).isBE()) {
						return false;
					}
				}
				else if(prop instanceof OrderedProperty) {
					if(((OrderedProperty) prop).getActivation()) {
						return false;
					}
				}
			}
		}
		
		return true;
	}
	
	public int size()
	{
		if(mProperties != null)
			return mProperties.size();
		else
			return 0;
	}
	
	@Override
	public Iterator<Property> iterator()
	{
		if(mProperties != null)
			return mProperties.iterator();
		else
			return nullIterator;
	}
	
	/**
	 * Returns the first property in the description, which class equals
	 * the given class.
	 * 
	 * @param pClassFilter Class of the desired property
	 * @return Reference to property or null, if no such property exists
	 */
	public Property get(Class<?> pClassFilter)
	{
		if(pClassFilter != null) {
			for(Property tProperty : this) {
				if(tProperty != null) {
					if(tProperty.getClass().equals(pClassFilter)) {
						return tProperty;
					}
				}
			}
		}
		
		return null;
	}

	/**
	 * Returns the first property in the description, which type name equals
	 * the given string.
	 * 
	 * @param pPropertyTypeName Type name of the property
	 * @return Reference to property or null, if no such property exists
	 */
	public Property get(String pPropertyTypeName)
	{
		if(pPropertyTypeName != null) {
			for(Property tProperty : this) {
				if(tProperty != null) {
					if(pPropertyTypeName.equals(tProperty.getTypeName())) {
						return tProperty;
					}
				}
			}
		}
		
		return null;
	}
	
	/**
	 * Searches for non-functional properties in the description.
	 * 
	 * @return Description with the references (!= null)
	 */
	public Description getNonFunctional()
	{
		Description tResDesc = new Description();
		
		for(Property tProperty : this) {
			if (tProperty instanceof NonFunctionalRequirementsProperty) {				
				tResDesc.set(tProperty);
			}
		}

		return tResDesc;
	}

	/**
	 * Searches for functional properties in the description
	 * 
	 * @return List with functional properties (!= null)
	 */
	public Description getFunctional()
	{
		Description tRequirements = new Description();
		
		for(Property tProp : this) {
			if(tProp != null) {
				if(!(tProp instanceof NonFunctionalRequirementsProperty)) {
					tRequirements.set(tProp);
				}
			}
		}
		
		return tRequirements;
	}

	
	/**
	 * TODO Method does not work for descriptions with same elements but in different order!
	 */
	public boolean equals(Object obj)
	{
		if(obj == this) return true;
		
		// empty list is equal to no description (both best effort)
		if(obj == null) return isBestEffort();
		
		if(obj instanceof Description) {			
			Description descr = (Description) obj;
			Iterator<Property> tIterator = iterator();
			Iterator<Property> tNewIterator = descr.iterator();
			
			while(tIterator.hasNext() && tNewIterator.hasNext()) {
				Property tReq    = tIterator.next();
				Property tNewReq = tNewIterator.next();
				
				if(tReq != null) {
					if(!tReq.equals(tNewReq)) {
						return false;
					}
				}
			}
			
			// both lists ended?
			if(tIterator.hasNext() || tNewIterator.hasNext()) {
				return false;
			}
			
			return true;
		} else {
			// not a description object
			return false;
		}
	}

	public Description clone()
	{
		Description tDescr = new Description();

		for(Property tProp : this) {
			tDescr.set(tProp.clone());
		}
		
		return tDescr;
	}
	
	public String toString()
	{
		String tResult = new String();
		for(Property tProperty : this)
			tResult += tProperty.toString()+ " ";
		
		return tResult;
	}
	
	private LinkedList<Property> mProperties;

}
