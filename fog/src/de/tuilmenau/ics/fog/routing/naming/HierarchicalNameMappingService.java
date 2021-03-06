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
package de.tuilmenau.ics.fog.routing.naming;

import java.io.Serializable;
import java.rmi.RemoteException;
import java.util.HashMap;
import java.util.LinkedList;

import net.rapi.Name;

import de.tuilmenau.ics.fog.topology.Simulation;
import de.tuilmenau.ics.fog.transfer.TransferPlaneObserver.NamingLevel;
import de.tuilmenau.ics.fog.util.Logger;
import de.tuilmenau.ics.middleware.JiniHelper;


public class HierarchicalNameMappingService<Address extends Serializable> implements NameMappingService<Address>
{
	private final static NameMappingEntry[] EMPTY_ARRAY = new NameMappingEntry[0];
	private final static String GLOBAL_NAMEMAPPING_SERVICE_NAME = "Global Name Mapping";
	
	
	public synchronized static NameMappingService createNameMappingService(Logger pLogger, String pName)
	{
		NameMappingService<?> tNameMappingService = (NameMappingService<?>) JiniHelper.getService(NameMappingService.class, pName);
		
		// no Jini available or no RS registered?
		if(tNameMappingService == null) {
			pLogger.log(HierarchicalNameMappingService.class, "No " +NameMappingService.class +" available from JINI: Creating " +pName);

			// create new one and try to register it
			tNameMappingService = new HierarchicalNameMappingService(null, pLogger);
			
			JiniHelper.registerService(NameMappingService.class, tNameMappingService, pName);
		} else {
			pLogger.log(HierarchicalNameMappingService.class, "Using NameMappingService provided via Jini");
		}
		
		return tNameMappingService;
	}
	
	/**
	 * Returns global name mapping service object.
	 * This name mapping service is used by simulations by default.
	 * If no object available, it will be created.
	 * 
	 * @param pSim Current simulation
	 * @return Global name mapping service object from simulation
	 */
	public synchronized static NameMappingService getGlobalNameMappingService(Simulation pSim)
	{
		// try local NM first
		NameMappingService<?> tNameMappingService = (NameMappingService<?>) pSim.getGlobalObject(NameMappingService.class);
		if(tNameMappingService == null) {
			// create/get new one from JINI
			tNameMappingService = createNameMappingService(pSim.getLogger(), GLOBAL_NAMEMAPPING_SERVICE_NAME);
			
			// cache it locally
			pSim.setGlobalObject(NameMappingService.class, tNameMappingService);
			
			pSim.getLogger().log(HierarchicalNameMappingService.class, "Using new name mapping service " +tNameMappingService);
		} else {
			pSim.getLogger().log(HierarchicalNameMappingService.class, "Using existing name mapping service " +tNameMappingService);
		}
		
		return tNameMappingService;
	}
	
	/**
	 * Constructor, which can be used by derived classes.
	 * 
	 * @param pParentService If != null, this service is informed about all activities, too.
	 */
	public HierarchicalNameMappingService(NameMappingService<Address> pParentService, Logger pParentLogger)
	{
		mParentNameMappingService = pParentService;
		mLogger = new Logger(pParentLogger);
	}
	
	@Override
	public void registerName(Name name, Address address, NamingLevel level)
	{
		if((name != null) && (address != null)) {
			// check, if name already exists
			LinkedList<NameMappingEntry<Address>> entry = mDNS.get(name);
			
			if(entry == null) {
				entry = new LinkedList<NameMappingEntry<Address>>();
				mDNS.put(name, entry);
			}
			
			entry.add(new NameMappingEntry<Address>(address, level));
			mLogger.log(this, address +" registered with name '" +name +"' (" +level +")");
			
			if(mParentNameMappingService != null) {
				// do not forward all the helper stuff to the parent
				// because that is just for local use by the routing service
				if(name.getNamespace().isAppNamespace()) {
					try {
						mParentNameMappingService.registerName(name, address, level);
					}
					catch(Exception exc) {
						// catch it here, since the parent service is more an optional issue
						mLogger.err(this, "Can not inform parent name mapping service " +mParentNameMappingService +" about new name " +name, exc);
					}
				}
			}
		}
	}

	@Override
	public boolean unregisterName(Name name, Address address)
	{
		LinkedList<NameMappingEntry<Address>> addrList = mDNS.get(name);
		boolean parentRes = false;
		
		if(mParentNameMappingService != null) {
			try {
				parentRes = mParentNameMappingService.unregisterName(name, address);
			}
			catch(Exception exc) {
				// catch it here, since the parent service is more an optional issue
				mLogger.err(this, "Can not inform parent name mapping service " +mParentNameMappingService +" about name deletion " +name, exc);
			}
		}

		if(addrList != null) {
			boolean res = addrList.remove(new NameMappingEntry<Address>(address));
			
			// no entries for a name any more?
			// -> remove empty list for this name
			if(addrList.size() <= 0) {
				mDNS.remove(name);
			}
			
			return res || parentRes;
		} else {
			// not even name was registered; but maybe parent
			// was able to delete it
			return parentRes;
		}
	}
	
	@Override
	public boolean unregisterNames(Address address)
	{
		int del = 0;
		boolean parentRes = false;
		
		for(LinkedList<NameMappingEntry<Address>> entryList : mDNS.values()) {
			LinkedList<NameMappingEntry<Address>> delList = null;
			
			for(NameMappingEntry<Address> entry : entryList) {
				if(entry.equals(address)) {
					if(delList == null) delList = new LinkedList<NameMappingEntry<Address>>();
					delList.add(entry);
				}
			}
			
			if(delList != null) {
				for(NameMappingEntry<Address> entry : delList) {
					if(entryList.remove(entry)) del++;
				}
			}
		}
		
		if(mParentNameMappingService != null) {
			try {
				parentRes = mParentNameMappingService.unregisterNames(address);
			}
			catch(Exception exc) {
				// catch it here, since the parent service is more an optional issue
				mLogger.err(this, "Can not inform parent name mapping service " +mParentNameMappingService +" about node deletion " +address, exc);
			}
		}

		return (del > 0) || parentRes;
	}
	
	/**
	 * Removes all entries in the data base.
	 */
	public void clear()
	{
		// iterate all entries in order to inform hierarchical service about deletion
		while(!mDNS.isEmpty()) {
			Name name = mDNS.keySet().iterator().next();
			
			LinkedList<NameMappingEntry<Address>> entry = mDNS.get(name);
			if(!entry.isEmpty()) {
				unregisterName(name, entry.getFirst().getAddress());
			} else {
				mDNS.remove(name);
			}
		}
		
	}
	
	/**
	 * Checks, if a given name-to-address entry is already available
	 * 
	 * @param name Name to check
	 * @param address Optional address (if null, any address listed for a name leads to a positive result)
	 * @return true, if name-to-address mapping already available
	 */
	public boolean contains(Name name, Address address)
	{
		NameMappingEntry<Address>[] addresses = getAddresses(name);
		
		if(address != null) {
			for(NameMappingEntry<Address> entry : addresses) {
				if(address.equals(entry.getAddress())) {
					return true;
				}
			}
			
			return false;
		} else {
			return addresses.length > 0;
		}
	}
	
	@Override
	public Name[] getNames(Address pAddress)
	{
		NameMappingEntry<Address> searchDummy = new NameMappingEntry<Address>(pAddress);
		LinkedList<Name> result = new LinkedList<Name>();
		
		// iterate all names and check, if the address is listed
		for(Name name : mDNS.keySet()) {
			LinkedList<NameMappingEntry<Address>> addrList = mDNS.get(name);
			
			if(addrList != null) {
				if(addrList.contains(searchDummy)) {
					result.add(name);
				}
			}
			// else: should not happen, since we iterate the keys
		}

		// convert list to suitable output format
		Name[] retVal = new Name[result.size()];
		int i = 0;
		for(Name name : result) {
			retVal[i] = name;
			i++;
		}
		return retVal;
	}

	@Override
	public NameMappingEntry<Address>[] getAddresses(Name name)
	{
		LinkedList<NameMappingEntry<Address>> addrList = mDNS.get(name);
		
		if(addrList == null) {
			if(mParentNameMappingService != null) {
				try {
					return mParentNameMappingService.getAddresses(name);
				}
				catch(Exception exc) {
					mLogger.err(this, "Can not get address for " +name +" from " +mParentNameMappingService, exc);
				}
			}
			
			return EMPTY_ARRAY;
		} else {
			return addrList.toArray(EMPTY_ARRAY);
		}
	}
	
	public Iterable<Name> getAllNames()
	{
		return mDNS.keySet();
	}
	
	/**
	 * @Deprecated Refactor remote name handling of nodes according to handling of media
	 */
	@Deprecated
	public String getASNameByNode(String node) throws RemoteException
	{
		return mASToNode.get(node);
	}
	
	@Deprecated 
	public boolean setNodeASName(String rAddress, String ASName)throws RemoteException
	{
		mASToNode.put(rAddress, ASName);
		return mASToNode.containsValue(rAddress);
	}


	private HashMap<Name, LinkedList<NameMappingEntry<Address>>> mDNS = new HashMap<Name, LinkedList<NameMappingEntry<Address>>>();	
	private HashMap<String, String> mASToNode = new HashMap<String,String>();
	protected NameMappingService<Address> mParentNameMappingService = null;
	private Logger mLogger = null;

}
