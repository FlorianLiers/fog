package de.tuilmenau.ics.fog.emulator.packets;

import java.io.Serializable;

import de.tuilmenau.ics.fog.emulator.Address;
import de.tuilmenau.ics.fog.emulator.PortID;

/**
 * Base for packets with common relaying protocol control information
 * and identification of the transport connection
 */
public class Packet implements Serializable
{
	/**
	 * Creates broadcast packet
	 */
	public Packet(int senderPort, int destinationPort, Serializable data)
	{
		this.senderAddress = null;
		this.senderPortNumber = senderPort;
		this.destination = new PortID(null, destinationPort);
		
		this.data = data;
	}
	
	public Packet(int senderPort, PortID destination, Serializable data)
	{
		this.senderAddress = null;
		this.senderPortNumber = senderPort;
		this.destination = destination;
		
		this.data = data;
	}
	
	/**
	 * Sets sender address after packet was constructed.
	 * Thus, the constructor of a packet does not have to
	 * know the address of a sending node.
	 */
	public void setSender(Address sender)
	{
		if(this.senderAddress == null) {
			this.senderAddress = sender;
		} else {
			// debug check
			if(!this.senderAddress.equals(sender)) {
				// do not allow to change this entry of an "const" object
				throw new RuntimeException(this +" - Can not change sender from " +this.senderAddress +" to " +sender);
			}
		}
	}
	
	public PortID getSender()
	{
		return new PortID(senderAddress, senderPortNumber);
	}
	
	public PortID getDestination()
	{
		return destination;
	}
	
	public Serializable getData()
	{
		return data;
	}
	
	@Override
	public String toString()
	{
		return "Packet:" +senderAddress +":" +senderPortNumber +"->" +destination +"(" +data +")";
	}
	
	private Address senderAddress;
	private int senderPortNumber;
	private PortID destination;
	
	private Serializable data;
}
