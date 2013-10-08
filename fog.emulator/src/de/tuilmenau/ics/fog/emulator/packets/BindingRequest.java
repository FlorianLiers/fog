package de.tuilmenau.ics.fog.emulator.packets;

import java.io.Serializable;

import net.rapi.Name;

/**
 * Signaling message asking for information about a binding.
 */
public class BindingRequest implements Serializable
{
	public BindingRequest(Name bindingName)
	{
		this.bindingName = bindingName;
	}
	
	public Name getBindingName()
	{
		return bindingName;
	}
	
	@Override
	public String toString()
	{
		return "requ:" +bindingName.toString();
	}
	
	private Name bindingName;
}
