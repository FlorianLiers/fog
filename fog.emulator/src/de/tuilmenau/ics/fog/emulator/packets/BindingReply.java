package de.tuilmenau.ics.fog.emulator.packets;

import net.rapi.Description;
import net.rapi.Name;

/**
 * Signaling message informing about a name binding.
 */
public class BindingReply extends BindingRequest
{
	public BindingReply(Name bindingName, Description requirements)
	{
		super(bindingName);
		
		this.requirements = requirements;
	}
	
	/**
	 * @return Additional requirements of binding for all connections; null if there are no additional requirements
	 */
	public Description getRequirements()
	{
		return requirements;
	}
	
	@Override
	public String toString()
	{
		return "answ:" +getBindingName().toString() +" (requ=" +requirements +")";
	}
	
	private Description requirements;
}
