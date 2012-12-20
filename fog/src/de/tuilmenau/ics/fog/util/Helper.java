/*******************************************************************************
 * Forwarding on Gates Simulator/Emulator
 * Copyright (C) 2012, Integrated Communication Systems Group, TU Ilmenau.
 * 
 * This program and the accompanying materials are dual-licensed under either
 * the terms of the Eclipse Public License v1.0 as published by the Eclipse
 * Foundation
 *  
 *   or (per the licensee's choosing)
 *  
 * under the terms of the GNU General Public License version 2 as published
 * by the Free Software Foundation.
 ******************************************************************************/
package de.tuilmenau.ics.fog.util;

import java.util.HashMap;


public class Helper
{
	/**
	 * HashMap.remove accepts keys as parameter only. But this helper function
	 * searches the hash for a value and deletes it with its key. Comparison
	 * of objects are done by reference.
	 *  
	 * @param pHash Reference to a hash map
	 * @param pValue Reference to the object, which should be deleted from the hash map
	 */
	public static <Key, Value> Key removeValueFromHashMap(HashMap<Key, Value> pHash, Value pValue)
	{
		for(Key tIndex : pHash.keySet()) {
			if(pHash.get(tIndex) == pValue) {
				pHash.remove(tIndex);
				return tIndex;
			}
		}
		
		return null;
	}
	
	public static <Key, Value> Key getKeyFromHashMap(HashMap<Key, Value> pHash, Value pValue)
	{
		for(Key tIndex : pHash.keySet()) {
			if(pHash.get(tIndex).equals(pValue)) {
				return tIndex;
			}
		}
		
		return null;
	}
}
