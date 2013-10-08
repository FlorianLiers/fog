package de.tuilmenau.ics.fog.emulator.packets;

import net.rapi.Name;

/**
 * Signaling message informing about a name binding.
 */
public class BindingReply extends BindingRequest
{
	public BindingReply(Name bindingName)
	{
		super(bindingName);
	}
	
	@Override
	public String toString()
	{
		return "answ:" +getBindingName().toString();
	}
}
