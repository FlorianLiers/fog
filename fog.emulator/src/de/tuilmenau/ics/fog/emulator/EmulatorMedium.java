/*******************************************************************************
 * Forwarding on Gates Simulator/Emulator - emulator interface
 * Copyright (c) 2012, Integrated Communication Systems Group, TU Ilmenau.
 * 
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html.
 ******************************************************************************/
package de.tuilmenau.ics.fog.emulator;

import java.io.IOException;

import net.rapi.Description;
import net.rapi.Layer;
import net.rapi.NetworkException;
import de.tuilmenau.ics.fog.emulator.Interface.ReceiveResult;
import de.tuilmenau.ics.fog.emulator.packets.Packet;
import de.tuilmenau.ics.fog.topology.AutonomousSystem;
import de.tuilmenau.ics.fog.topology.Medium;
import de.tuilmenau.ics.fog.topology.Node;
import de.tuilmenau.ics.fog.topology.RemoteMedium;
import de.tuilmenau.ics.fog.topology.Simulation;
import de.tuilmenau.ics.fog.ui.Viewable;
import de.tuilmenau.ics.fog.util.Logger;


/**
 * Emulates a broadcast medium.
 * Class represents the medium itself. The attachment points of nodes are
 * represented by {@link EmulatorLayer} objects.
 */
public class EmulatorMedium implements Medium, Runnable
{
	
	public EmulatorMedium(AutonomousSystem pAS, String pName, String pInterfaceNameIn, String pInterfaceNameOut) throws NetworkException
	{	
		Logger tLogger = pAS.getLogger();
		
		mAS = pAS;
		mName = pName;
		mLogger = new Logger(tLogger);
		
		mInterface = Interface.get(pInterfaceNameIn, pInterfaceNameOut, tLogger);
		
		new Thread(this).start();
	}

	@Override
	public Status isBroken()
	{
		if(mBroken) {
			return Status.BROKEN;
		} else {
			return Status.OK;
		}
	}
	
	@Override
	public void setBroken(boolean pBroken, boolean pErrorTypeVisible)
	{
		mBroken = pBroken;
	}
	
	@Override
	public String getName() 
	{
		return mName;
	}

	@Override
	public synchronized Layer attach(Node node)
	{
		if(layer != null) {
			throw new RuntimeException(this +" - Can not attach " +node +" because there is already " +layer +" registered.");
		} else {
			layer = new EmulatorLayer(mInterface.getAddress(), this);
		}

		return layer;
	}
	
	@Override
	public synchronized Layer detach(Node node)
	{
		Layer res = layer;
		
		if(layer != null) {
			layer.detached();
			layer = null;
		}
		
		return res;
	}

	@Override
	public void deleted()
	{
		detach(null);
		
		if(mInterface != null) {
			mInterface.close();
			mInterface = null;
		}
	}
	
	@Override
	public RemoteMedium getProxy()
	{
		// Ethernet object can not be shared with remote computers
		return null;
	}

	public synchronized void sendPacket(Packet packet) throws NetworkException
	{
		if(!mBroken) {
			if(layer != null) {
				mLogger.trace(this, "Sending packet " +packet +" to " +packet.getDestination());
				
				try {
					mInterface.send(packet.getDestination().getAddress(), packet);
				}
				catch(IOException exc) {
					throw new NetworkException(this, "Can not send packet " +packet, exc);
				}
			} else {
				mLogger.warn(this, "Someone tries to send packet " +packet +", but nobody is attached.");
			}
		} else {
			// we are broken; simulate error 
			throw new NetworkException(this, "Can not send packet since medium is broken.");
		}
	}
	
	public Logger getLogger()
	{
		return mLogger;
	}
	
	public Description getDescription()
	{
		return null; // TODO is lossy; what about bandwidth and delay?
	}
	
	@Override
	public void run()
	{
		while(mInterface != null) {
			try {
				ReceiveResult res = mInterface.receive();
				if(res != null) {
					if(res.data instanceof Packet) {
						packetReceived((Packet) res.data);
					}
				}
			}
			catch(Exception exc) {
				mLogger.err(this, "Can not receive data. Ignoring one packet.", exc);
			}
		}
	}
	
	private void packetReceived(Packet packet) throws Exception
	{
		if(layer != null) {
			layer.handleReceivedPacket(packet);
		} else {
			mLogger.log(this, "Ignoring packet '" +packet +"' since medium is not connected to a node.");
		}
	}
	
	public Simulation getSim()
	{
		return mAS.getSimulation();
	}
	
	public String toString()
	{
		return mName +"(" +mBroken +")";
	}
	
	public int getPacketLossProbability()
	{
		return lossProbability;
	}

	public void setPacketLossProbability(int lossProbInPercent)
	{
		lossProbability = lossProbInPercent;
	}
	
	private AutonomousSystem mAS;
	private Logger mLogger;
	
	private String mName;
	
	@Viewable("Broken")
	private boolean mBroken = false;
	@Viewable("Layer")
	private EmulatorLayer layer = null;
	@Viewable("Interface")
	private Interface mInterface;
	@Viewable("Loss probability [%]")
	private int lossProbability = 0;
}
