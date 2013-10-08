package de.tuilmenau.ics.fog.emulator;

import java.io.Serializable;

public class PortID implements Serializable
{
	public PortID(Address addr, int portNumber)
	{
		this.address = addr;
		this.portNumber = portNumber;
	}
	
	public Address getAddress()
	{
		return address;
	}
	
	public int getPortNumber()
	{
		return portNumber;
	}
	
	@Override
	public boolean equals(Object obj)
	{
		if(obj instanceof PortID) {
			return (address.equals(((PortID) obj).getAddress()) && (portNumber == ((PortID) obj).getPortNumber()));
		} else {
			return false;
		}
	}
	
	@Override
	public int hashCode()
	{
		return address.hashCode() +portNumber;
	}
	
	@Override
	public String toString()
	{
		return address +":" +portNumber;
	}
	
	private Address address;
	private int portNumber;
}
