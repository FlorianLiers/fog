/*******************************************************************************
 * Forwarding on Gates Simulator/Emulator
 * Copyright (C) 2012, Integrated Communication Systems Group, TU Ilmenau.
 * 
 * This program and the accompanying materials are dual-licensed under either
 * the terms of the Eclipse Public License v1.0 as published by the Eclipse
 * Foundation
 *  
 *   or (per the licensee's choosing)
 *  
 * under the terms of the GNU General Public License version 2 as published
 * by the Free Software Foundation.
 ******************************************************************************/
package de.tuilmenau.ics.fog.transfer.manager;

import java.util.LinkedList;
import java.util.Random;

import net.rapi.Binding;
import net.rapi.Connection;
import net.rapi.Description;
import net.rapi.Identity;
import net.rapi.Layer;
import net.rapi.Name;
import net.rapi.Namespace;
import net.rapi.NeighborName;
import net.rapi.NetworkException;
import net.rapi.properties.DelayProperty;
import de.tuilmenau.ics.fog.Config;
import de.tuilmenau.ics.fog.FoGEntity;
import de.tuilmenau.ics.fog.IContinuation;
import de.tuilmenau.ics.fog.IEvent;
import de.tuilmenau.ics.fog.application.util.Service;
import de.tuilmenau.ics.fog.facade.DescriptionHelper;
import de.tuilmenau.ics.fog.transfer.DummyForwardingElement;
import de.tuilmenau.ics.fog.transfer.ForwardingElement;
import de.tuilmenau.ics.fog.transfer.Gate;
import de.tuilmenau.ics.fog.transfer.TransferPlaneObserver.NamingLevel;
import de.tuilmenau.ics.fog.transfer.forwardingNodes.GateContainer;
import de.tuilmenau.ics.fog.transfer.forwardingNodes.Multiplexer;
import de.tuilmenau.ics.fog.transfer.gates.DirectDownGate;
import de.tuilmenau.ics.fog.transfer.gates.GateIterator;
import de.tuilmenau.ics.fog.transfer.gates.ReroutingGate;
import de.tuilmenau.ics.fog.util.LayerObserver;
import de.tuilmenau.ics.fog.util.SimpleName;


public class LowerLayerObserver extends LayerObserver
{
	private static final double REATTACH_TIMER_SEC = 10.0d;

	private static final Namespace FOG_NAMESPACE = new Namespace("fog");
	
	private static final Name FOG_NEIGHBOR_FILTER = new SimpleName(FOG_NAMESPACE);
	
	
	public LowerLayerObserver(Layer lowerLayer, FoGEntity entity)
	{
		super(false, entity.getLogger(), null);
		
		this.mEntity = entity;
		this.lowerLayer = lowerLayer;
		this.attached = false;
		
		// For GUI purposes: Check if lower layer entity can be shown in GUI
		//                   as being the subject for this observer  
		if(lowerLayer instanceof ForwardingElement) {
			mProxyForLL = (ForwardingElement) lowerLayer;
		} else {
			mProxyForLL = new DummyForwardingElement(lowerLayer);
		}
	}
	
	/**
	 * Inits static variable (required due to exception handling of parse method)
	 */
	private void generateBindingName() throws NetworkException
	{
		int rounds = 0;
		
		// Does the forwarding node has a routing name?
		// Note: This way of generating a binding name
		//       is just useful for debugging purposes.
		//       It is not required for real systems.
		attachmentName = mEntity.getRoutingService().getNameFor(mMultiplexer);
		
		do {
			rounds++;
			
			if(attachmentName == null) {
				attachmentName = generateRandomName();
			}
			
			// check if name is already known
			if(lowerLayer.isKnown(attachmentName)) {
				// invalidate name and retry
				attachmentName = null;
				attachmentIdentity = null;
			} else {
				// ok, name seems to be unique
				attachmentIdentity = mEntity.getAuthenticationService().createIdentity(attachmentName.toString());
				return;
			}
			
			// prevent an infinite loop
			if(rounds > 10) {
				throw new NetworkException(this, "Can not generate a random attachment point name.");
			}
		}
		while(true);
	}
	
	private Name generateRandomName()
	{
		Random randGen = new Random(System.currentTimeMillis());
		
		// Note: The base part of the name (entity name) is not required in real systems.
		//       It is just useful for debugging purposes.
		return new SimpleName(FOG_NAMESPACE, mEntity.getNode().getName() +"_" +Long.toString(randGen.nextLong()));
	}
	
	/**
	 * Attaches network interface to lower layer and
	 * creates all needed gates.
	 * 
	 * @return if attach operation was successfully or not
	 */
	public boolean attach()
	{
		// use receive gate as indicator, whether or not we are connected 
		if(!attached) {
			attached = true;
			
			// start event handling from lower layer
			start(lowerLayer);
			
			// create forwarding node if it does not exist from previous attach operations
			// and perform a (re-)open.
			if(mMultiplexer == null) {
				Name tFNName = null;
				if(!Config.Routing.REDUCE_NUMBER_FNS) {
					tFNName = Controller.generateRoutingServiceName();
				}
				mMultiplexer = new Multiplexer(mEntity, tFNName, NamingLevel.NAMES, false, getEntity().getIdentity(), mEntity.getController());
			}
			mMultiplexer.open();
			
			try {
				// chose name for FoG attachment point to lower layer
				generateBindingName();
				
				// bind FoG layer to lower layer
				Binding tBinding = lowerLayer.bind(null, attachmentName, null, mEntity.getIdentity());
				mLowerLayerBinding = new FoGService();
				
				// start handling of binding events
				mLowerLayerBinding.start(tBinding);
				
				// link central multiplexer with multiplexer of interface
				mMultiplexer.connectMultiplexer(mEntity.getCentralFN());
			
				// look for already known neighbors
				neighborCheck();
			}
			catch(Exception tExc) {
				mEntity.getLogger().err(this, "Can not attach to lower layer " +lowerLayer, tExc);
				detach();
				return false;
			}
		}
		
		return true;
	}
	
	@Override
	public void neighborDiscovered(NeighborName newNeighbor)
	{
		Name newNeighborName = newNeighbor.getBindingName();
		
		// check preconditions
		if(attached && (attachmentName != null)) {
			// is it not my own binding?
			if(!attachmentName.equals(newNeighborName)) {
				boolean alreadyDelayed = false;
				if(delayedDiscovery != null) {
					alreadyDelayed = delayedDiscovery.remove(newNeighborName);
				}
				
				// avoid two peers starting at the same time to connect to each other
				if((attachmentName.toString().compareTo(newNeighborName.toString()) < 0) || alreadyDelayed) {
					synchronized(mMultiplexer) {
						ReroutingGate[] backup = new ReroutingGate[1];
						DirectDownGate downGate = checkDownGateAvailable(newNeighborName, null);
						
						// check, if DownGate already available
						if(downGate == null) {
							// if there is a backup for the best-effort gate, there might be more gates,
							// which can be repaired?
							if(backup[0] != null) {
								getLogger().trace(this, "BE DownGate to neighbor available as rerouting gate. Maybe other gates can be repaired, too? Schedule event.");
								
								mEntity.getTimeBase().scheduleIn(1.0d, new RepairEvent(newNeighborName));
							}

							//
							// Option (A):
							// Request bidirectional connection without previous setup on node itself
							//
							// Assumes that the neighbor will try to setup a reverse gate for its gate.
		
							//
							// Option (B):
							// Setup one gate immediately and request reverse gate, only
							//
							try {
								createGateTo(newNeighborName, DescriptionHelper.createBE(false), null, null);
							}
							catch (NetworkException tExc) {
								getLogger().warn(this, "Can not add down gate to neighbor " +newNeighborName +". Ignoring neighbor.", tExc);
							}
						} else {
							// if it was no delayed try to setup relationship, trigger refresh
							if(!alreadyDelayed) {
								getLogger().log(this, "DownGate to neighbor " +newNeighborName +" already available.");
								
								getLogger().log(this, "Refreshing DownGate " +downGate);
								downGate.refresh();
								
								if(!downGate.isOperational()) {
									getLogger().warn(this, "Refreshed gate reports error. Setting up a new one.");
									// deleting old and restart discovery
									downGate.shutdown();
									neighborDiscovered(newNeighbor);
								}
							} else {
								getLogger().trace(this, "Delayed discovery skipped due to already existing process.");
							}
						}
					}
				} else {
					getLogger().log(this, "Wait for peer " +newNeighborName +" to start first.");
					
					if(delayedDiscovery == null) delayedDiscovery = new LinkedList<Name>();
					delayedDiscovery.add(newNeighborName);
					
					mEntity.getTimeBase().scheduleIn(getDefaultTimeout(), new DelayedDiscovery(newNeighbor));
				}
			}
			// else: ignore own binding
		} else {
			// maybe we forgot to unregister the observer of the lower layer? 
			mEntity.getLogger().err(this, "Neighbor discovered was called, but interface is not connected. Ignoring call.");
		}
	}
	
	/**
	 * Event to start a delayed discovery for a peer.
	 * If a peer is expected to start, the discovery
	 * is delayed. The delay is required as backup,
	 * if the peer does not start.
	 */
	private class DelayedDiscovery implements IEvent
	{
		public DelayedDiscovery(NeighborName neighbor)
		{
			this.neighbor = neighbor;
		}
		
		@Override
		public void fire()
		{
			neighborDiscovered(neighbor);
		}
		
		private NeighborName neighbor;
	}
	
	/**
	 * 
	 * @author florian
	 *
	 */
	private class RepairEvent implements IEvent
	{
		public RepairEvent(Name pNeighborLLID)
		{
			mNeighborLLID = pNeighborLLID;
		}
		
		@Override
		public void fire()
		{
			getLogger().log(this, "Repair gates for interface " +LowerLayerObserver.this);
			
			synchronized(mEntity) {
				while(true) {
					// Search for rerouting gates and try to repair them
					GateIterator tIter = getMultiplexerGate().getIterator(ReroutingGate.class);
					
					if(tIter.hasNext()) {
						// type cast is valid, due to filter for iterator
/* TODO move fix to gate class
						ReroutingGate tGate = (ReroutingGate) tIter.next();
						
						if(tGate.match(mNeighborLLID, null, null)) {
							try {
								createNewDownGate(mNeighborLLID, tGate.getDescription(), tGate, true, tGate.getOwner());
							}
							catch (NetworkException tExc) {
								getLogger().warn(this, "Failed to repair " +tGate +".", tExc);
							}
						}
						*/
					} else {
						// terminate loop
						break;
					}
				}
			}
		}
		
		private Name mNeighborLLID;
	}
	
	@Override
	public void neighborDisappeared(NeighborName oldNeighbor)
	{
		Name oldNeighborName = oldNeighbor.getBindingName();
		
		if(attached && (attachmentName != null)) {
			// is it my own binding?
			if(!attachmentName.equals(oldNeighborName)) {
				synchronized(mMultiplexer) {
					getLogger().log(this, "Deleting DirectDownGate to neighbor " +oldNeighborName);
					
					DirectDownGate gate;
					do {
						gate = checkDownGateAvailable(oldNeighborName, null);
						if(gate != null) {
							gate.shutdown();
						}
					}
					while(gate != null);
				}
			}
			// else: ignore own binding
		} else {
			// maybe we forgot to unregister the observer of the lower layer? 
			mEntity.getLogger().err(this, "Neighbor disappeared was called, but interface is not connected. Ignoring call.");
		}
	}
	
	/**
	 * Check if there are already known neighbors available.
	 * It asks the lower layer for a list of known neighbors
	 * and informs the controller about each.
	 * 
	 * @return If it was able to retrieve neighbors from LL
	 */
	@Override
	public boolean neighborCheck()
	{
		try {
			Iterable<NeighborName> neighbors = lowerLayer.getNeighbors(FOG_NEIGHBOR_FILTER);
			
			// go through list and run discover
			if(neighbors != null) {
				for(NeighborName neighbor : neighbors) {
					neighborDiscovered(neighbor);
				}
				
				return true;
			}
		}
		catch(Exception exc) {
			mEntity.getLogger().err(this, "Ignoring remote exception during updating of lower layer neighbors from '" +lowerLayer +"'.", exc);
		}
		
		return false;
	}
	
	/**
	 * Detaches network interface from the lower layer and
	 * removes all gates attached to it.
	 */
	public void detach()
	{
		if(attached) {
			attached = false;
			
			// stop event handling from lower layer
			stop();
			
			// Remove binding
			mLowerLayerBinding.stop();
			
			// remove all delayed contacts
			delayedDiscovery = null;

			// unlink central multiplexer with multiplexer of interface
			mEntity.getCentralFN().unregisterGatesTo(mMultiplexer);
			mMultiplexer.unregisterGatesTo(mEntity.getCentralFN());
			mMultiplexer.close();
			
			mLowerLayerBinding = null;
			
			attachmentName = null;
		}
	}
		
	/**
	 * Is used to re-install the network interface in the routing service
	 * after a broken time of the node (NOT the link!). Furthermore, it
	 * tries to re-establish the gates to its neighbors. Both might have
	 * been deleted during the broken time of the node.
	 */
	public void repair()
	{
		if(attached) {
			// previous attach operation failed? 
			if(mLowerLayerBinding == null) {
				attach();
			} else {
				// just reinstall routing service stuff
				mMultiplexer.open();
				
				// re-link central multiplexer with multiplexer of interface
				mEntity.getCentralFN().unregisterGatesTo(mMultiplexer);
				mMultiplexer.unregisterGatesTo(mEntity.getCentralFN());

				mMultiplexer.connectMultiplexer(mEntity.getCentralFN());
				
				// refresh neighbors
				neighborCheck();
			}
			// else: not attached, nothing to repair
		}
	}
	
	/**
	 * Checks if a DirectDownGate starting at the FN of the interface
	 * encapsulates a connection to a specific binding name of a peer
	 * FoG entity.
	 * 
	 * @param neighborBindingName ID of the lower layer the gate is connected to
	 * @param pDescription QoS description (== null, if no filtering for description)
	 * @return Gate fitting the parameters
	 */
	public DirectDownGate checkDownGateAvailable(Name neighborBindingName, Description capabilities)
	{
		if((mMultiplexer != null) && (neighborBindingName != null)) {
			//
			// Search for gate
			// Maybe there was a down gate and the node has some
			// gates from repairing the down gate.
			//
			GateIterator tIter = mMultiplexer.getIterator(DirectDownGate.class);
			
			while(tIter.hasNext()) {
				// type cast is valid, due to filter for iterator
				DirectDownGate tGate = (DirectDownGate) tIter.next();
				
				if(neighborBindingName.equals(tGate.getPeerBindingName())) {
					// do we have to filter according to caps?
					if(capabilities != null) {
						if(capabilities.equals(tGate.getDescription())) {
							return tGate;
						}
					} else {
						return tGate;
					}
				}
			}
		}
		
		return null;
	}
	
	public void enableReattach()
	{
		mEntity.getTimeBase().scheduleIn(REATTACH_TIMER_SEC, new IEvent() {
			@Override
			public void fire()
			{
				if(attached) {
					// we are attached => check neighbors
					if(!neighborCheck()) {
						mEntity.getTimeBase().scheduleIn(REATTACH_TIMER_SEC, this);
					}
				} else {
					// we are not attached => try to attach again
					if(!attach()) {
						mEntity.getTimeBase().scheduleIn(REATTACH_TIMER_SEC, this);
					}
				}
			}
		});
	}
	
	/**
	 * @return Returns a timeout for one RTT suitable for the lower layer in seconds.
	 */
	public double getDefaultTimeout()
	{
		double res = -1;
		Description requCapab = new Description();
		requCapab.set(new DelayProperty());
		
		try {
			Description capab = lowerLayer.getCapabilities(null, requCapab);
			if(capab != null) {
				DelayProperty delay = (DelayProperty) capab.get(DelayProperty.class);
				
				// maximum delay for both messages (to peer and back again)
				res = Math.max(delay.getMin(), delay.getMax()) *2;
			}
		}
		catch(NetworkException exc) {
			// ignore it and use default
		}
		
		// if delay not known or undefined, use some other default value
		if(res <= 0) {
			return REATTACH_TIMER_SEC;
		} else {
			return res;
		}
	}
	
	/**
	 * Called by the receive gate in order to signal the removing
	 * of the lower layer from the simulation. 
	 */
	public void remove()
	{
		if(attached) {
			if(mEntity != null) {
				mEntity.getNode().detach(lowerLayer);
			}
			
			// invalidate link to lower layer
			lowerLayer = null;
		}
		// otherwise: we are not attached or we are currently detaching
	}
	
	public void createGateTo(Name destination, Description requirements, Identity owner, IContinuation<Gate> callback) throws NetworkException
	{
		getLogger().log(this, "Start creating a new gate to " +destination +" with requirements " +requirements +" for " +owner);
		
		// request capabilities of lower layer and derive description of gate
		Description connDescription = requirements;
		Description capabilities = lowerLayer.getCapabilities(destination, requirements);
		if(capabilities != null) {
			connDescription = DescriptionHelper.deriveRequirements(capabilities, requirements);
		}
		
		// use observer as default owner of gates for lower layer
		if(owner == null) owner = attachmentIdentity;
		
		// establish connection
		Connection conn = lowerLayer.connect(destination, connDescription, attachmentIdentity);
		
		// start handling of signaling and creation of gate
		startSession(conn, callback);
	}
	
	public Name getAttachmentName()
	{
		return attachmentName;
	}
	
	/**
	 * @return multiplexer, where received packets will be forwarded to 
	 */
	public GateContainer getMultiplexerGate()
	{
		return mMultiplexer;
	}
	
	public FoGEntity getEntity()
	{
		return mEntity;
	}
	
	public Layer getBus()
	{
		return lowerLayer;
	}

	/**
	 * Method for GUI purposes! It returns a proxy object
	 * for the lower layer, which can be used for GUI
	 * purposes.
	 */
	public ForwardingElement getLowerLayerGUIRepresentation()
	{
		return mProxyForLL;
	}

	/**
	 * DownGates will automatically (in constructor) register at their
	 * interface. Registration is needed for backward route trace.
	 */
	public void attachDownGate(DirectDownGate gate)
	{
		mDownGates.add(gate);
	}
	
	@Override
	public String toString()
	{
		return "LowerLayerObserver:" +mLowerLayerBinding +"@" +lowerLayer;
	}

	private void startSession(Connection connection, IContinuation<Gate> callback)
	{
		LowerLayerSession session = new LowerLayerSession(mEntity, this, callback);
		session.start(connection);
	}
	
	/**
	 * Proxy class in order to avoid multiple inheritance of observer class.
	 */
	private class FoGService extends Service
	{
		public FoGService()
		{
			super(false, null);
		}
		
		@Override
		public void newConnection(Connection connection)
		{
			startSession(connection, null);
		}
	};

	private FoGEntity mEntity;
	private Layer lowerLayer;
	private Service mLowerLayerBinding;
	
	private ForwardingElement mProxyForLL;
	private Multiplexer mMultiplexer;
	private LinkedList<DirectDownGate> mDownGates = new LinkedList<DirectDownGate>();
	
	/**
	 * List of neighbors, which should be integrated after a timeout.
	 * Prevents two neighbors to start connecting to each others at the same time.
	 */
	private LinkedList<Name> delayedDiscovery;
	
	/**
	 * Attach is not checked by checking references, because references
	 * need to valid during detach operation. Explicit boolean prevents
	 * cascading detach operations. 
	 */
	private boolean attached;

	/**
	 * Name used by this FoG point of attachment to bind to lower layer
	 */
	private Name attachmentName;
	private Identity attachmentIdentity;
}

