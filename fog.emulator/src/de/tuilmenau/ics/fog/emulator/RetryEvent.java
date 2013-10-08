package de.tuilmenau.ics.fog.emulator;

import net.rapi.NetworkException;
import de.tuilmenau.ics.fog.IEvent;
import de.tuilmenau.ics.fog.emulator.packets.Packet;


public abstract class RetryEvent implements IEvent
{
	public RetryEvent(EmulatorLayer layer, Packet packet, int numberTries, double maxWaitTimeSec)
	{
		this.layer = layer;
		this.packet = packet;
		this.tries = numberTries;
		this.waitTime = maxWaitTimeSec / (double) numberTries;
	}
	
	@Override
	public void fire()
	{
		layer.getLogger().debug(this, "Timeout for resending signaling message (tries=" +tries +")");
		
		// still required?
		if(!answerReceived()) {
			// are we allowed to retry again?
			if(tries > 0) {
				try {
					layer.sendPacket(packet);
				}
				catch(NetworkException exc) {
					// ignore it; just count it as try
				}
				tries--;
				layer.getSim().getTimeBase().scheduleIn(waitTime, this);
			} else {
				timeout();
			}
		}

	}
	
	protected abstract boolean answerReceived();
	protected abstract void timeout();

	private EmulatorLayer layer;
	private Packet packet;
	private int tries;
	private double waitTime;
}
