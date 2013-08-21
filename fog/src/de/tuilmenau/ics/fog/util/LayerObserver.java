package de.tuilmenau.ics.fog.util;

import java.rmi.RemoteException;

import net.rapi.Layer;
import net.rapi.NeighborName;
import net.rapi.events.ConnectedEvent;
import net.rapi.events.DisconnectedEvent;
import net.rapi.events.Event;
import net.rapi.events.PeerInformationEvent;
import de.tuilmenau.ics.fog.application.ApplicationEventHandler;
import de.tuilmenau.ics.fog.application.util.LayerObserverCallback;
import de.tuilmenau.ics.fog.ui.Logging;


public class LayerObserver extends ApplicationEventHandler<Layer> implements LayerObserverCallback
{
	public LayerObserver(boolean ownThread, Logger logger, LayerObserverCallback callback)
	{
		super(ownThread);
	
		if(logger == null) {
			Logging.getInstance().warn(this, "No logger specified; using global logger.");
			this.logger = Logging.getInstance();
		} else {
			this.logger = logger;
		}
		
		this.callback = callback;
	}
	
	@Override
	protected void handleEvent(Event event) throws Exception
	{
		if(event instanceof PeerInformationEvent) {
			PeerInformationEvent neighborEvent = (PeerInformationEvent) event;
			
			if(neighborEvent.isAppeared()) {
				neighborDiscovered(neighborEvent.getPeerName());
			} else {
				neighborDisappeared(neighborEvent.getPeerName());
			}
		}
		else if(event instanceof ConnectedEvent) {
			// layer has (re-)connected -> check status
			neighborCheck();
		}
		else {
			getLogger().warn(this, "Received unknown event: " +event);
		}
	}
	
	@Override
	public void neighborDiscovered(NeighborName newNeighbor) throws RemoteException
	{
		if(callback != null) callback.neighborDiscovered(newNeighbor);
	}

	@Override
	public void neighborDisappeared(NeighborName oldNeighbor) throws RemoteException
	{
		if(callback != null) callback.neighborDisappeared(oldNeighbor);
	}

	@Override
	public boolean neighborCheck() throws RemoteException
	{
		if(callback != null) return callback.neighborCheck();
		return true;
	}

	/**
	 * A helper method for avoiding the method name <code>getEventSource()</code>, which
	 * is not meaningful in this context.
	 */
	public Layer getLayer()
	{
		return getEventSource();
	}
	
	public Logger getLogger()
	{
		return logger;
	}

	private Logger logger;
	private LayerObserverCallback callback;
}
