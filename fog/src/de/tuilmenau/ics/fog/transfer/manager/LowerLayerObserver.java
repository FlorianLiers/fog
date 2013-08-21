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
import net.rapi.Signature;
import net.rapi.properties.DelayProperty;

import de.tuilmenau.ics.fog.Config;
import de.tuilmenau.ics.fog.FoGEntity;
import de.tuilmenau.ics.fog.IEvent;
import de.tuilmenau.ics.fog.application.util.Service;
import de.tuilmenau.ics.fog.exceptions.InvalidParameterException;
import de.tuilmenau.ics.fog.exceptions.TransferServiceException;
import de.tuilmenau.ics.fog.facade.DescriptionHelper;
import de.tuilmenau.ics.fog.transfer.DummyForwardingElement;
import de.tuilmenau.ics.fog.transfer.ForwardingElement;
import de.tuilmenau.ics.fog.transfer.TransferPlaneObserver.NamingLevel;
import de.tuilmenau.ics.fog.transfer.forwardingNodes.GateContainer;
import de.tuilmenau.ics.fog.transfer.forwardingNodes.Multiplexer;
import de.tuilmenau.ics.fog.transfer.gates.AbstractGate;
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
		
		// first try: Use same name as node in order to simplify debugging
		//            Note: Using this name as base is not required in real networks!
		attachmentName = new SimpleName(FOG_NAMESPACE, mEntity.getNode().getName() +"_1");
		
		do {
			rounds++;
			
			if(attachmentName == null) {
				Random randGen = new Random(System.currentTimeMillis());
	
				attachmentName = new SimpleName(FOG_NAMESPACE, mEntity.getNode().getName() +"_" +Long.toString(randGen.nextLong()));
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
			
				// Look for already known neighbors
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
						ProcessDownGate downGate = checkDownGateAvailable(newNeighborName, backup);
						
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
								createNewDownGate(newNeighborName, DescriptionHelper.createBE(false), backup[0], false, mEntity.getIdentity());
							}
							catch (NetworkException tExc) {
								getLogger().warn(this, "Can not add down gate to neighbor " +newNeighborName +". Ignoring neighbor.", tExc);
							}
						} else {
							// if it was no delayed try to setup relationship, trigger refresh
							if(!alreadyDelayed) {
								getLogger().log(this, "DownGate to neighbor " +newNeighborName +" already available.");
								
								getLogger().log(this, "Refreshing DownGate " +downGate);
								if(!downGate.check()) {
									getLogger().log(this, "Refreshed gate reports error. Setting up a new one.");
									// deleting old and restart discovery
									downGate.terminate(null);
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
						ReroutingGate tGate = (ReroutingGate) tIter.next();
						
						if(tGate.match(mNeighborLLID, null, null)) {
							try {
								createNewDownGate(mNeighborLLID, tGate.getDescription(), tGate, true, tGate.getOwner());
							}
							catch (NetworkException tExc) {
								getLogger().warn(this, "Failed to repair " +tGate +".", tExc);
							}
						}
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
					
					ProcessDownGate process;
					do {
						process = checkDownGateAvailable(oldNeighborName, null);
						if(process != null) {
							process.terminate(null);
						}
					}
					while(process != null);
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
	 * Is called if DownGates to neighbors with the given parameters should be tested.
	 * 
	 * @param peerName FoG peer name
	 */
	public void checkDownGates(Name peerName)
	{
		ProcessDownGate tBEGate = checkDownGateAvailable(peerName, null);
		
		if(tBEGate != null) {
			tBEGate.check();
			
			// TODO integrate old code
			//tBEGate.handlePacket(new Packet(null), null);
		}
	}
	
	/**
	 * Checks if a DownGate starting at the FN of the interface is going to pNeighborLLID.
	 * 
	 * @param pNeighborLLID ID of the lower layer the gate is connected to
	 * @param pDescription QoS description (== null, if no filtering for description)
	 * @return Gate fitting the parameters
	 */
	public ProcessDownGate checkDownGateAvailable(Name pNeighborLLID, ReroutingGate[] pBackupGate)
	{
		if((mMultiplexer != null) && (pNeighborLLID != null)) {
			//
			// Search for process responsible for gate
			//
			ProcessList processes = mEntity.getProcessRegister().getProcesses(mMultiplexer);
			
			if(processes != null) {
				for(Process process : processes) {
					if(process instanceof ProcessDownGate) {
						ProcessDownGate res = (ProcessDownGate) process; 
						Name peer = res.getNeighborName();
						
						if(pNeighborLLID.equals(peer)) {
							return res;
						}
					}
				}
			}
			
			//
			// Search for old gate
			// Maybe there was a down gate and the node has some
			// gates from repairing the down gate.
			//
 			if(pBackupGate != null) {
				if(pBackupGate.length > 0) {
					pBackupGate[0] = null;
					
					GateIterator tIter = mMultiplexer.getIterator(ReroutingGate.class);
					
					while(tIter.hasNext()) {
						// type cast is valid, due to filter for iterator
						ReroutingGate tGate = (ReroutingGate) tIter.next();
						
						if(tGate.match(pNeighborLLID, null, null)) {
							pBackupGate[0] = tGate;
							return null;
						}
					}
				}
			}
			
			return null;
		}
		
		return null;
	}
	
	/**
	 * Creates new down gate to neighbor and requests the reverse gate from it. 
	 * 
	 * @param pFN FN to connect the gate to
	 * @param pInterface Lower layer
	 * @param pNeighborLLID Lower layer name of peer
	 * @param pRequirements Requirements for down gate
	 * @throws NetworkException on error
	 */
	public AbstractGate createNewDownGate(Name pPeerName, Description pRequirements, ReroutingGate pBackup, boolean pCreateImmediately, Identity pRequester) throws NetworkException
	{
		getLogger().log(this, "Creating DownGate to neighbor " +pPeerName +" with " +pRequirements);
		
		ProcessDownGate tProcess = null;
		try {
			tProcess = new ProcessDownGate(this, pPeerName, pRequirements, pRequester, pBackup);
			tProcess.start();
			
			// tProcess.start may have already created the gate, if the
			// connection is already established. However, a gate can
			// be created for a connection that is in the process of
			// being set up.
			if(pCreateImmediately) {
				return tProcess.create();
			} else {
				return null;
			}
		}
		catch (NetworkException tExc) {
			tExc = new TransferServiceException(this, "Opening down gate to neighbor " +pPeerName +" failed.", tExc);
			
			if(tProcess != null) {
				tProcess.terminate(tExc);
			}
			throw tExc;
		}
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
	 * Determines name of peer for a connection via the responsible LowerLayerSession
	 */
	public Name getPeerName(Connection connection)
	{
		ProcessList processes = mEntity.getProcessRegister().getProcesses(mMultiplexer);
		if(processes != null) {
			for(Process process : processes) {
				if(process instanceof ProcessDownGate) {
					ProcessDownGate processDG = ((ProcessDownGate) process);
					
					// does the process controls the connection?
					if(processDG.hasConnection(connection)) {
						return processDG.getNeighborName();
					}
				}
			}
		}
		
		return null;
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
	
	public synchronized LowerLayerSession getConnectionTo(Name destination, Description requirements) throws NetworkException
	{
		// request capabilities of lower layer and derive description of gate
		Description connDescription = requirements;
		Description capabilities = lowerLayer.getCapabilities(destination, requirements);
		if(capabilities != null) {
			connDescription = DescriptionHelper.deriveRequirements(capabilities, requirements);
		}
		
		// establish connection
		Connection conn = lowerLayer.connect(destination, connDescription, attachmentIdentity);
		
		LowerLayerSession session = new LowerLayerSession(mMultiplexer, null, getLogger());
		session.start(conn);
		
		return session;
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

	private class FoGService extends Service
	{
		public FoGService()
		{
			super(false, null);
		}
		
		/**
		 * Accept only connection with source authentications.
		 */
		@Override
		public boolean openAck(LinkedList<Signature> pAuths, Description pDescription, Name pTargetName)
		{
			if(pAuths != null) {
				if(pAuths.size() > 0) {
					getLogger().log(this, "Accept new connection from " +pAuths + " to " +pTargetName);
					return true;
				}
			}
			
			getLogger().warn(this, "Reject connection due to missing authentications.");
			return false;
		}
		
		@Override
		public void newConnection(Connection pConnection)
		{
			Signature originator = pConnection.getAuthentications().getFirst();
			Name sourceName;
			try {
				sourceName = SimpleName.parse(originator.getIdentity().getName());
			
				// check if there is a binding for the source
				if(lowerLayer.isKnown(sourceName)) {
					LowerLayerSession session = new LowerLayerSession(mMultiplexer, sourceName, getLogger());
					session.start(pConnection);
				} else {
					getLogger().warn(this, "Lower layer does not know binding for " +sourceName);
					pConnection.close();
				}
			}
			catch (InvalidParameterException exc) {
				getLogger().warn(this, "Name of signature " +originator +" does not seem to be a FoG name.", exc);
				pConnection.close();
			}
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

