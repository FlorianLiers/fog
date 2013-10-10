package de.tuilmenau.ics.fog.emulator;

import java.io.IOException;
import java.io.Serializable;
import java.util.LinkedList;

import net.rapi.Description;
import net.rapi.Name;
import net.rapi.NetworkException;
import net.rapi.Signature;
import net.rapi.events.ConnectedEvent;
import net.rapi.events.ErrorEvent;
import net.rapi.impl.base.BaseConnectionEndPoint;
import de.tuilmenau.ics.fog.emulator.packets.BindingReply;
import de.tuilmenau.ics.fog.emulator.packets.BindingRequest;
import de.tuilmenau.ics.fog.emulator.packets.Packet;


/**
 * TODO authentication of packets
 * TODO use approp. state machine
 *
 */
public class EmulatorConnectionEndPoint extends BaseConnectionEndPoint implements Port
{
	private static final int MAX_NUMBER_TRIES = 4;
	private static final double MAX_WAIT_TIME_SEC = 10.0d;
	
	
	public EmulatorConnectionEndPoint(EmulatorLayer layer, Name bindingName, int portNumber, PortID peer)
	{
		super(bindingName);
		
		this.layer = layer;
		
		// From a security standpoint the non-random numbers are not suitable.
		// However, if we use signatures for packets, guessing numbers seems
		// not to be of any benefit.
		this.ownPortNumber = portNumber;
		
		this.peer = peer;
	}
	
	/**
	 * Connections belongs to a local binding.
	 */
	public EmulatorConnectionEndPoint(EmulatorLayer layer, EmulatorBinding binding, PortID peer)
	{
		this(layer, binding.getName(), binding.getPortNumber(), peer);
		
		this.binding = binding;
	}
	
	@Override
	public void connect()
	{
		if(peer == null) {
			// we do not know address and port number of peer!
			// -> request it!
			
			Packet packet = new Packet(ownPortNumber, EmulatorLayer.CONTROL_PORT, new BindingRequest(getBindingName()));
			new RetryEvent(layer, packet, MAX_NUMBER_TRIES, MAX_WAIT_TIME_SEC) {
				@Override
				public boolean answerReceived()
				{
					// did we received an answer from peer?
					return peer != null;
				}
				
				public void timeout()
				{
					// game over: connect failed
					failed(new NetworkException(this, "Can not open connection at peer (" +MAX_NUMBER_TRIES +" tries failed)."));
				}
			}.fire();
		} else {
			layer.getLogger().log(this, "Connection established");
			
			// inform higher layer about the establishment of the connection
			notifyObservers(new ConnectedEvent(this));
		}
	}

	@Override
	public boolean isConnected()
	{
		return (peer != null);
	}
	
	public boolean isClosed()
	{
		return ownPortNumber < 0;
	}

	@Override
	public LinkedList<Signature> getAuthentications()
	{
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Description getRequirements()
	{
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void close()
	{
		layer.getLogger().log(this, "Closing");
		
		// report close event to container object
		if(binding != null) {
			binding.closed(this);
		} else {
			layer.closed(this);
		}
		
		// *now* we can invalidate the object 
		ownPortNumber = -1;
		peer = null;
		
		super.cleanup();
	}
	
	@Override
	public int getPortNumber()
	{
		return ownPortNumber;
	}

	@Override
	protected void sendDataToPeer(Serializable data) throws NetworkException
	{
		if(isConnected()) {
			Packet packet = new Packet(getPortNumber(), peer, data);
			
			numberSendPackets++;
			layer.sendPacket(packet);
		} else {
			throw new NetworkException(this, "Can not send data since connection is not connected.");
		}
	}
	
	@Override
	public void handlePacket(Packet packet)
	{
		Serializable data = packet.getData();
		if(data instanceof BindingReply) {
			BindingReply msg = (BindingReply) data;
			Name bindingName = getBindingName();
			boolean acceptNewPeer = true;
			
			// check binding name only if we known about it
			if(bindingName != null) {
				acceptNewPeer = bindingName.equals(msg.getBindingName());
			}
			
			// TODO check authentication of sender
			if(acceptNewPeer) {
				peer = packet.getSender();
				
				if(peer != null) {
					// call connect in order to inform higher layer about est. connection
					connect();
				}
			}
		} else {
			if(isConnected()) {
				try {
					storeDataForApp(data);
				}
				catch(IOException exc) {
					failed(exc);
				}
			} else {
				layer.getLogger().warn(this, "Received packet " +packet +" before CEP is connected.");
			}
		}
	}
	
	@Override
	protected void notifyFailure(Throwable failure, EventListener listener)
	{
		// TODO Auto-generated method stub
		
		
	}
	
	public PortID getPeer()
	{
		return peer;
	}
	
	public int getNumberSendPackets()
	{
		return numberSendPackets;
	}
	
	/**
	 * Report some communication problem and close connection
	 */
	public void failed(Throwable exc)
	{
		notifyObservers(new ErrorEvent(exc, this));
		close();
	}

	private EmulatorLayer layer;
	private EmulatorBinding binding = null;

	private int ownPortNumber  = -1;
	
	/**
	 * Packet counter
	 */
	private int numberSendPackets = 0;

	/**
	 * Current knowledge about the state of the peer
	 */
	private PortID peer = null;
}
