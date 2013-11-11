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
package de.tuilmenau.ics.fog.topology;

import java.rmi.RemoteException;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;

import net.rapi.Layer;
import net.rapi.Namespace;

import de.tuilmenau.ics.fog.EventHandler;
import de.tuilmenau.ics.fog.routing.naming.HierarchicalNameMappingService;
import de.tuilmenau.ics.fog.routing.naming.NameMappingEntry;
import de.tuilmenau.ics.fog.routing.naming.NameMappingService;
import de.tuilmenau.ics.fog.transfer.TransferPlaneObserver.NamingLevel;
import de.tuilmenau.ics.fog.util.Logger;
import de.tuilmenau.ics.fog.util.SimpleName;
import de.tuilmenau.ics.graph.GraphProvider;
import de.tuilmenau.ics.graph.RoutableGraph;
import de.tuilmenau.ics.middleware.JiniHelper;


/**
 * Container for collecting "physical" nodes and links somehow belonging together.
 * In addition this container provides a graph representation for drawing the GUI. 
 */
public class Network implements GraphProvider
{
	private static final String MEDIUM_TO_NETWORK_MAPPING = "Medium name to network name mapping";
	private static final Namespace MEDIUM_NAMESPACE = new Namespace("medium");
	
	
	public Network(String pName, Logger pLogger, EventHandler pTimeBase)
	{
		mName = pName;
		mLogger = pLogger;
		mTimeBase = pTimeBase;
	}
	
	public synchronized boolean addNode(Node newNode)
	{
		String name = newNode.getName();
		boolean tOk = false;
		
		if(!containsNode(name) && (name != null)) {
			nodelist.put(name, newNode);
		
			mScenario.add(newNode);
			tOk = true;
		}			
		
		return tOk;
	}
	
	public boolean containsNode(String name)
	{
		return nodelist.containsKey(name);
	}
	
	public boolean removeNode(String name)
	{
		return removeNode(name, true);
	}

	private synchronized boolean removeNode(String name, boolean informElement)
	{
		boolean tOk = false;
		
		Node tNode = nodelist.remove(name);
		
		if(tNode != null) {
			mScenario.remove(tNode);
			
			if(informElement) tNode.deleted();
			tOk = true;
		} else {
			mLogger.log(this, "Can not remove node. " +name +" not known.");
		}
		
		return tOk;
	}

	public synchronized boolean setNodeBroken(String pNode, boolean pBroken, boolean pErrorTypeVisible)
	{
		boolean tOk = false;
		Node tNode = getNodeByName(pNode);
		
		if (tNode != null) {
			tNode.setBroken(pBroken, pErrorTypeVisible);
			tOk = true;
		} else {
			mLogger.log(this, "Can not " + (pBroken ? "break" : "repair") +
				" node. " + pNode +" not known.");
		}
		return tOk;
	}
	
	public synchronized boolean setBusBroken(String pBus, boolean pBroken, boolean pErrorTypeVisible)
	{
		boolean tOk = false;
		Medium tBus = getMediumByName(pBus, false);
		
		if (tBus != null) {
			try {
				tBus.setBroken(pBroken, pErrorTypeVisible);
				tOk = true;
			}
			catch(RemoteException tExc) {
				mLogger.err(this, "Can not " + (pBroken ? "break" : "repair") +
						" bus. " + pBus +" due to exception " +tExc);
			}
		} else {
			mLogger.err(this, "Can not " + (pBroken ? "break" : "repair") +
				" bus. " + pBus +" not known.");
		}
		
		return tOk;
	}
	
	public String getName()
	{
		return mName;
	}
	
	public synchronized boolean addBus(Medium newBus)
	{
		boolean tOk = false;
		
		if(newBus != null) {
			String name = newBus.getName();
			
			if(name != null) {
				if(!containsBus(name)) {
					buslist.put(name, newBus);
					
					mScenario.add(newBus);

					// make it available remotely
					RemoteMedium proxy = newBus.getProxy();
					// is it the original object?
					if(proxy != null) {
						// register medium proxy in JINI
						JiniHelper.registerService(RemoteMedium.class, proxy, name);
						
						mLogger.debug(this, "Registered medium with " + JiniHelper.getService(RemoteMedium.class, name));
					}
					
					// register network name for medium
					try {
						if(remoteMediumToNetworkName == null) {
							remoteMediumToNetworkName = HierarchicalNameMappingService.createNameMappingService(mLogger, MEDIUM_TO_NETWORK_MAPPING);
						}
						
						remoteMediumToNetworkName.registerName(new SimpleName(MEDIUM_NAMESPACE, name), getName(), NamingLevel.NAMES);
					}
					catch (RemoteException exc) {
						mLogger.warn(this, "Can not register network name for " +newBus, exc);
					}
					
					tOk = true;
				}
				// else: already added
			}
		}
		
		return tOk;
	}

	public boolean containsBus(String name)
	{
		return buslist.containsKey(name);
	}
	
	public boolean removeBus(String pName)
	{
		return removeBus(pName, true);
	}
	
	private synchronized boolean removeBus(String pName, boolean pInformElement)
	{
		boolean tRes = false;
		
		Medium tBus = buslist.remove(pName);
		
		if(tBus != null) {
			tRes = true;
			
			RemoteMedium proxy = tBus.getProxy();
			if(proxy != null) {
				// unregister medium from JINI
				JiniHelper.unregisterService(RemoteMedium.class, proxy);
				
				// remove medium from name mapping
				if(remoteMediumToNetworkName != null) {
					try {
						remoteMediumToNetworkName.unregisterName(new SimpleName(MEDIUM_NAMESPACE, tBus.getName()), getName());
					}
					catch (RemoteException exc) {
						mLogger.warn(this, "Can not remove " +tBus +" from medium to network mapping.", exc);
					}
				}
			}
			
			mScenario.remove(tBus);
			
			if(pInformElement) {
				tBus.deleted();
			}
		} else {
			mLogger.log(this, "Can not remove bus. " +pName +" not known or not local.");
		}
		
		return tRes;
	}

	/**
	 * @param name Name of requested node
	 * @return Reference to node with name or null, if no such node exists
	 */
	public Node getNodeByName(String name)
	{
		return nodelist.get(name);
	}
	
	/**
	 * @return a random node from the network
	 */
	public Node getRandomNode()
	{
		Collection<Node> nodes = nodelist.values();
		Node node = null;
		
		// get random entry index starting from 1...size
		// details: random [0...1) * size => 0...(size-1)
		int randomEntry = 1+ (int) (Math.random()*nodes.size());
		
		// go through list until random position was reached 
		Iterator<Node> it = nodes.iterator();
		while(it.hasNext()) {
			randomEntry--;
			node = it.next();
			
			if(randomEntry <= 0) {
				break;
			}
		}
		
		return node;
	}

	/**
	 * Searches a medium by its name
	 * 
	 * @param name Name of the medium
	 * @param activateRemote If not available locally, try to activate an adapter from remote
	 * @return Medium or null if none available
	 */
	protected synchronized Medium getMediumByName(String name, boolean activateRemote)
	{
		Medium tRes = buslist.get(name);
		
		// locally not available? => try it via RMI
		if((tRes == null) && activateRemote) {
			RemoteMedium tProxy = (RemoteMedium) JiniHelper.getService(RemoteMedium.class, name);

			if(tProxy != null) {
				tRes = tProxy.activate(mTimeBase, mLogger);
	
				if(tRes != null) {
					buslist.put(name, tRes);
				}
			}
			// else: nothing available via Jini => bus not known
		}
		
		return tRes;
	}
	
	public static synchronized String getNetworkNameOfMedium(Logger logger, String mediumName)
	{
		if(remoteMediumToNetworkName == null) {
			remoteMediumToNetworkName = HierarchicalNameMappingService.createNameMappingService(logger, MEDIUM_TO_NETWORK_MAPPING);
		}
		
		try {
			NameMappingEntry<String>[] networkNames = remoteMediumToNetworkName.getAddresses(new SimpleName(MEDIUM_NAMESPACE, mediumName));
			if(networkNames.length > 0) {
				return networkNames[0].getAddress();
			}
		}
		catch (RemoteException exc) {
			logger.err(Network.class, "Can not access mapping from medium to network names for " +mediumName, exc);
		}
		
		return null;
	}
	
	public boolean attach(Node node, Medium medium)
	{
		if(medium != null) {
			Layer layer = medium.attach(node);
			
			if(layer != null) {
				// inform node
				node.attach(layer);
				
				// draw it in GUI
				mScenario.link(node, medium, layer);
				return true;
			}
		}
		
		return false;
	}
	
	public boolean detach(Node node, Medium medium)
	{
		Object link = mScenario.getEdge(node, medium, null);
		
		if(link instanceof Layer) {
			Layer layer = (Layer) link;
			
			// inform medium and node
			node.detach(layer);
			medium.detach(node);
			
			// update GUI although detach had failed (since there is not link anymore) 
			mScenario.unlink(layer);
			return true;
		} else {
			mLogger.err(this, "Node " +node +" is not attached to medium " +medium);
		}
		
		return false;
	}
	
	public int numberOfNodes()
	{
		return nodelist.size();
	}
	
	public int numberOfBuses()
	{
		return buslist.size();
	}
	
	/**
	 * Removes all elements of the network as if they had
	 * been deleted one by one.
	 */
	public void removeAll()
	{
		removeAll(true);
	}
	
	/**
	 * Empties network without informing the elements about that.
	 * It is used to terminate a simulation without needing the
	 * simulated elements to shutdown properly.
	 */
	public void cleanup()
	{
		removeAll(false);
	}

	private synchronized void removeAll(boolean informElements)
	{
		mLogger.trace(this, "Removing whole network (inform elements = " +informElements +")");
		
		// 1. inform nodes that they should shut down
		if(informElements) {
			for(Node node : nodelist.values()) {
				node.shutdown(true);
			}
		}

		// 2. remove them from network
		while(!nodelist.isEmpty()) {
			String tNodeName = nodelist.keySet().iterator().next();
			
			removeNode(tNodeName, informElements);
		}
		
		// 3. delete buses
		while(!buslist.isEmpty()) {
			String tBusName = buslist.keySet().iterator().next();
			
			removeBus(tBusName, informElements);
		}
	}
	
	public Logger getLogger()
	{
		return mLogger;
	}
	
	/**
	 * For GUI purposes, only!
	 */
	@Override
	public RoutableGraph<Object, Object> getGraph()
	{
		return mScenario;
	}
	
	protected Logger mLogger;
	
	private String mName = null;
	private RoutableGraph<Object, Object> mScenario = new RoutableGraph<Object, Object>();
	private EventHandler mTimeBase;
	
	private HashMap<String, Node> nodelist  = new HashMap<String, Node>();
	private HashMap<String, Medium> buslist = new HashMap<String, Medium>();

	/**
	 * Element required for simulation purposes. It stores the network names for remote mediums.
	 */
	private static NameMappingService<String> remoteMediumToNetworkName = null;
}
