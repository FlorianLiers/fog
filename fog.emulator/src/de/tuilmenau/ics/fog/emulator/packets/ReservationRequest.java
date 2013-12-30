package de.tuilmenau.ics.fog.emulator.packets;

import java.io.Serializable;

import net.rapi.Description;


public class ReservationRequest implements Serializable
{
	public ReservationRequest(Description requirements)
	{
		this.requ = requirements;
	}
	
	public Description getRequirements()
	{
		return requ;
	}
	
	private Description requ;
}
