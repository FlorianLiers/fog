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
package de.tuilmenau.ics.fog.facade;

import net.rapi.Description;
import net.rapi.properties.DatarateProperty;
import net.rapi.properties.DelayProperty;
import net.rapi.properties.LossRateProperty;
import net.rapi.properties.NonFunctionalRequirementsProperty;
import net.rapi.properties.Property;
import net.rapi.properties.PropertyException;
import net.rapi.properties.MinMaxProperty.Limit;
import net.rapi.properties.OrderedProperty;
import de.tuilmenau.ics.fog.application.InterOpIP;
import de.tuilmenau.ics.fog.facade.properties.FunctionalRequirementProperty;
import de.tuilmenau.ics.fog.facade.properties.IpDestinationProperty;
import de.tuilmenau.ics.fog.facade.properties.TransportProperty;


/**
 * Helper methods for creating descriptions.
 */
public class DescriptionHelper
{
	/**
	 * Calculate description for request to remote system based on the
	 * description of the way from the remote system to the local one.
	 * 
	 * @param pDescr Description from original way to the local system
	 * @return Description for way back (!= null)
	 */
	public static Description calculateDescrForRemoteSystem(Description descr) throws PropertyException
	{
		Description tReturnDescription = new Description();
		
		for(Property tProp : descr) {
			if(tProp instanceof FunctionalRequirementProperty) {
				FunctionalRequirementProperty tRemoteProp = ((FunctionalRequirementProperty) tProp).getRemoteProperty();
				if(tRemoteProp != null) {
					tReturnDescription.add(tRemoteProp);
				} /*else {
					// Property for remote system is unknown. 
				}*/
			} else {
				tReturnDescription.add(tProp);
			}
		}
		
		return tReturnDescription;
	}

	/**
	 * {@link NonFunctionalRequirementsProperty} method deriveRequirements
	 */
	public static Description deriveRequirements(Description pCapabilities, Description pDescr) throws PropertyException
	{
		Description tRes = new Description();
		
		if(pDescr != null) {
			// iterate all elements in capabilities
			for(Property tProp : pCapabilities) {
				if(tProp instanceof NonFunctionalRequirementsProperty) {
					Property tMinusProp = pDescr.get(tProp.getClass());
					
					if(tMinusProp != null) {
						tRes.add(((NonFunctionalRequirementsProperty)tProp).deriveRequirements(tMinusProp));
					} else {
						// Requ is not listed in other list. Take over old one without changes.
						tRes.add(tProp);
					}
				} else {
					throw new PropertyException(pCapabilities, "Can not handle non functional " +tProp);
				}
			}
			
			// check remaining elements in requirements
			for(Property tProp : pDescr) {
				// already handled in first loop?
				Property alreadyCovered = tRes.get(tProp.getClass());
				if(alreadyCovered == null) {
					tRes.add(tProp);
				}
			}
		} else {
			// Other list is not defined. Take over old list without changes.
			return pCapabilities;
		}
		
		return tRes;
	}

	/**
	 * {@link NonFunctionalRequirementsProperty} method removeCapabilities
	 */
	public static Description removeCapabilities(Description pRequ, Description pDescr) throws PropertyException
	{
		Description tRes = new Description();
		
		if(pDescr != null) {
			for(Property tProp : pRequ) {
				if(tProp instanceof NonFunctionalRequirementsProperty) {
					Property tPlusProp = pDescr.get(tProp.getClass());
					
					if(tPlusProp != null) {
						tRes.add(((NonFunctionalRequirementsProperty)tProp).removeCapabilities(tPlusProp));
					} else {
						tRes.add(tProp);
					}
				} else {
					if(tProp instanceof FunctionalRequirementProperty) {
						throw new PropertyException(pRequ, "Can not handle non functional " +tProp);
					} else {
						// it is a property, which does not belong in any category
						tRes.add(tProp);
					}
				}
			}
		}
		
		
		return tRes;
	}

	/**
	 * Creates stream description without any QoS requirements (best effort) 
	 * 
	 * @param ordered If the stream data should be ordered or not
	 * @return Description object
	 */
	public static Description createBE(boolean ordered)
	{
		Description descr = new Description();

		if(ordered)
			descr.set(new OrderedProperty(ordered));
		
		return descr;
	}

	/**
	 * Factory method for QoS descriptions.
	 * 
	 * @param ordered If the stream data should be ordered or not
	 * @param delayMilliSec Maximum delay in milliseconds
	 * @param bandwidthKBitSec Minimum bandwidth in kilobits per seconds
	 * @return Description object
	 */
	public static Description createQoS(boolean ordered, int delayMilliSec, int bandwidthKBitSec)
	{
		Description descr = new Description();

		descr.set(new OrderedProperty(ordered));
		descr.set(new DatarateProperty(bandwidthKBitSec, Limit.MIN));
		descr.set(new DelayProperty(delayMilliSec, Limit.MAX));
		
		return descr;
	}

	/**
	 * Factory method for IP destination descriptions.
	 * 
	 * @param pDestIp Destination IP address
	 * @param pDestPort Destination port number
	 * @param pDestTransport Destination IP based transport (TCP, UDP,..)
	 * @return Description object
	 */
	public static Description createIpDestination(byte[] pDestIp, int pDestPort, InterOpIP.Transport pDestTransport)
	{
		Description descr = new Description();
//		System.out.println("Creating new IP destination description)");
//		for (int i = 0; i < pDestIp.length; i++)
//			System.out.println("Data " + i + " = " + pDestIp[i]);
		descr.set(new IpDestinationProperty(pDestIp, pDestPort, pDestTransport));
		
		return descr;
	}
	
	/**
	 * @return Requirements modeling the assumptions about TCP
	 */
	public static Description createTCPlike()
	{
		Description requ = new Description();
		
		requ.set(new TransportProperty(true, false));
		
		return requ;
	}

	/**
	 * Factory method for getting an empty description for avoiding
	 * null pointer with description parameters.
	 */
	public static Description createEmpty()
	{
		return new Description();
	}

	//TODO: cleanup with createHostExtended
	/**
	 * Factory method for getting a description with all possible properties.
	 */
	public static Description createAll()
	{
		Description tDesc = createQoS(true, 200, 64);
		byte[] tTargetIp = new byte[4];
		tTargetIp[0] = 127;
		tTargetIp[1] = 0;
		tTargetIp[2] = 0;
		tTargetIp[3] = 1;		

		tDesc.set(new IpDestinationProperty(tTargetIp, 5000, InterOpIP.Transport.UDP));
		tDesc.set(new TransportProperty(true, false));
		tDesc.set(new LossRateProperty());
		
		return tDesc;
	}
	
	/**
	 * Factory method for getting a description with extended host properties.
	 */
	public static Description createHostExtended()
	{
		Description tDesc = createQoS(true, 200, 64);
		byte[] tTargetIp = new byte[4];
		tTargetIp[0] = 127;
		tTargetIp[1] = 0;
		tTargetIp[2] = 0;
		tTargetIp[3] = 1;		

		tDesc.set(new IpDestinationProperty(tTargetIp, 5000, InterOpIP.Transport.UDP));
		tDesc.set(new TransportProperty(true, false));
		tDesc.set(new LossRateProperty());
		
		return tDesc;
	}
	
	/**
	 * Factory method for getting a description with basic properties.
	 * Basically, it represents end host functions.
	 */
	public static Description createHostBasic()
	{
		Description tDesc = new Description();

		tDesc.set(new TransportProperty(true, false));
		
		return tDesc;
	}

}
