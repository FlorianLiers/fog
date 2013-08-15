/*******************************************************************************
 * Forwarding on Gates Simulator/Emulator - Hierarchical Routing Management
 * Copyright (c) 2012, Integrated Communication Systems Group, TU Ilmenau.
 * 
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html.
 ******************************************************************************/
package de.tuilmenau.ics.fog.routing.hierarchical;

import net.rapi.Identity;
import net.rapi.Signature;
import de.tuilmenau.ics.fog.authentication.SimpleSignature;
import de.tuilmenau.ics.fog.exceptions.AuthenticationException;

public class HierarchicalSignature extends SimpleSignature
{
	private static final long serialVersionUID = 4847037702247056096L;
	private int mLevel = 0;
	
	public HierarchicalSignature(Identity pIdentity, Object pOrigin, byte[] pSignature, int pLevel) throws AuthenticationException
	{
		super(pIdentity);
		mLevel = pLevel;
		if(pOrigin != null) {
			mOrigin = pOrigin;
		} else {
			throw new AuthenticationException(this, "Unable to create signature ");
		}
	}
	
	public int getLevel()
	{
		return mLevel;
	}
	
	public String toString()
	{
		return mOrigin + "@" + mLevel;
	}
	
	public boolean equals(Object pObj)
	{
		if(pObj instanceof HierarchicalSignature) {
			return ((HierarchicalSignature)pObj).getLevel() == getLevel() && ((HierarchicalSignature)pObj).getIdentity().equals(getIdentity());
		} else if(pObj instanceof Signature) {
			return ((Signature)pObj).getIdentity().equals(getIdentity());
		}
		return false;
	}

	private Object mOrigin;
}
