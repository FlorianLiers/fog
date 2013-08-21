package de.tuilmenau.ics.fog.transfer.manager;

import net.rapi.Name;
import net.rapi.events.Event;
import de.tuilmenau.ics.fog.application.util.Session;
import de.tuilmenau.ics.fog.packets.Packet;
import de.tuilmenau.ics.fog.packets.PleaseOpenDownGate;
import de.tuilmenau.ics.fog.transfer.ForwardingElement;
import de.tuilmenau.ics.fog.util.Logger;


/**
 * Bridge between connection through lower layer and FoG.
 * Forwards lower layer data to FoG and informs ProcessDownGate about events.
 * 
 * TODO events lost in case of multithreading?
 */
public class LowerLayerSession extends Session
{
	public LowerLayerSession(ForwardingElement next, Name bindingName, Logger logger)
	{
		super(false, logger, null);
		
		this.next = next;
		this.bindingName = bindingName;
	}
	
	@Override
	protected void handleEvent(Event event) throws Exception
	{
		super.handleEvent(event);
	}
	
	@Override
	public boolean receiveData(Object data)
	{
		if(data instanceof Packet) {
			Packet packet = (Packet) data;
			
			// clear overhead data
			packet.clearDownRoute();
			
			if(next != null) {
				// do we have to track the receiving session for DownGate creation?
				if(packet.isSignalling() && packet.getRoute().isEmpty()) {
					Object payload = packet.getData();
					if(payload instanceof PleaseOpenDownGate) {
						((PleaseOpenDownGate) payload).setReceiveSession(this);
					}
				}
				
				next.handlePacket(packet, null);
			} else {
				getLogger().err(this, "No receive gate specified. Packet " +data +" dropped.");
			}
		} else {
			getLogger().err(this, "Unknown data type of received content " +data);
		}
		
		return true;
	}

	@Override
	public void connected()
	{
		if(observer != null) observer.connected();
	}
	
	@Override
	public void error(Exception pExc)
	{
		if(observer != null) observer.terminate(pExc);
		else error = pExc;
	}
	
	@Override
	public void stop()
	{
		super.stop();
		
		if(observer != null) observer.terminate(null);
	}
	
	public void setObserver(ProcessDownGate newObserver)
	{
		if((newObserver != null) && (observer != null)) {
			throw new RuntimeException(this +" - Can not set observer " +newObserver +" since " +observer +" is already set.");
		}
		
		observer = newObserver;
		
		if(observer != null) {
			if(isConnected()) {
				observer.connected();
			} else {
				// already terminated with or without error?
				if(error != null) observer.terminate(error);
				else if(getConnection() == null) {
					observer.terminate(null);
				}
			}
		}
		// else: the observer was deleted
	}
	
	/**
	 * @return Name of binding even if connection was establish passively. Null on error. 
	 */
	public Name getBindingName()
	{
		if(bindingName != null) return bindingName;
		else {
			return getConnection().getBindingName();
		}
	}
	
	private Name bindingName;
	private ForwardingElement next;
	
	private ProcessDownGate observer;
	private Exception error = null;
}
