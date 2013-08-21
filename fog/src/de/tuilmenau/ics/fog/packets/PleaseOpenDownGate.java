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
package de.tuilmenau.ics.fog.packets;

import net.rapi.Description;
import net.rapi.Identity;
import net.rapi.Name;
import net.rapi.NetworkException;
import de.tuilmenau.ics.fog.FoGEntity;
import de.tuilmenau.ics.fog.transfer.ForwardingNode;
import de.tuilmenau.ics.fog.transfer.gates.GateID;
import de.tuilmenau.ics.fog.transfer.gates.ReroutingGate;
import de.tuilmenau.ics.fog.transfer.manager.LowerLayerObserver;
import de.tuilmenau.ics.fog.transfer.manager.LowerLayerSession;
import de.tuilmenau.ics.fog.transfer.manager.Process;
import de.tuilmenau.ics.fog.transfer.manager.ProcessDownGate;
import de.tuilmenau.ics.fog.transfer.manager.ProcessGateConstruction;
import de.tuilmenau.ics.fog.ui.Viewable;


/**
 * Message asking a peer for opening a down gate to
 * somebody (in most cases the sender) else.
 */
public class PleaseOpenDownGate extends PleaseOpenGate
{
	private static final long serialVersionUID = -7309033185606147609L;

	/**
	 * Constructor for requesting a reverse gate though the same connection.
	 */
	public PleaseOpenDownGate(int pLocalProcessNumber, GateID pLocalOutgoingGateNumber, ForwardingNode pLocalDestination, Name pLocalAttachmentName)
	{
		super(pLocalProcessNumber, pLocalOutgoingGateNumber, null);
		
		mPeerNodeAttachmentName = pLocalAttachmentName;
		mPeerDestinationRoutingName = pLocalDestination.getEntity().getRoutingService().getNameFor(pLocalDestination);
	}

	/**
	 * Constructor for requesting a new connection through the same lower layer
	 * 
	 * @param pLocalProcessNumber Number of the process requesting the gate 
	 * @param pLocalOutgoingGateNumber Gate number of local gate acting as reverse gate for new gate
	 * @param pLocalDestination Forwarding node receiving the data passed through the new gate
	 * @param pLocalAttachmentName Name of the FoG peer requesting the gate
	 * @param pRequirements Requirements for new connection (null == no new connection; reuse existing one)
	 */
	public PleaseOpenDownGate(ProcessDownGate pProcess, ForwardingNode pLocalDestination, Name pLocalAttachmentName, Description pRequirements)
	{
		super(pProcess.getID(), pProcess.getGateNumber(), pRequirements);
		
		mPeerNodeAttachmentName = pLocalAttachmentName;
		mPeerDestinationRoutingName = pLocalDestination.getEntity().getRoutingService().getNameFor(pLocalDestination);
	}
	
	@Override
	public boolean execute(ForwardingNode pFN, Packet pPacket, Identity pRequester)
	{
		FoGEntity node = pFN.getEntity();
		
		node.getLogger().log(this, "execute open request for " +pFN + " from reverse FoG peer " +mPeerNodeAttachmentName +" with routing name " +mPeerDestinationRoutingName +" and requ=" +getDescription());
		
		// check if FN corresponds to a valid network interface
		LowerLayerObserver netInf = node.getController().getNetworkInterface(pFN);
		
		if(netInf != null) {
			// do we already have a process for this request?
			Process process = node.getController().getProcessFor(pFN, pRequester, getProcessNumber());
			
			// if there is none, create new one to handle request
			if(process == null) {
				ReroutingGate[] backup = new ReroutingGate[1];
				ProcessDownGate downGate = netInf.checkDownGateAvailable(mPeerNodeAttachmentName, backup);
				
				// check, if DownGate already available
				if(downGate == null) {
					// if there is a backup for the best-effort gate, there might be more gates,
					// which can be repaired?
/* TODO					if(backup[0] != null) {
						node.getLogger().trace(this, "BE DownGate to neighbor available as rerouting gate. Maybe other gates can be repaired, too? Schedule event.");
						
						node.getTimeBase().scheduleIn(1.0d, new RepairEvent(newNeighborName));
					}*/
				} else {
					node.getLogger().err(this, "No process but a gate available (gate=" +downGate +") for " +mPeerNodeAttachmentName);
				}
				
				try {
					// If requirements are given, the request opens a new connection.
					// If not, the gate is opened for the connection via the request was received.
					Description requ = getDescription();
					if(requ != null) {
						process = new ProcessDownGate(netInf, mPeerNodeAttachmentName, requ, pRequester, backup[0]);
					} else {
						if(mReceiveSession != null) {
							// Note: productive implementations would have to check,
							//       if receiving connection belongs really to the interface
							process = new ProcessDownGate(netInf, mReceiveSession, pRequester, backup[0]);
						} else {
							throw new NetworkException(this, "Can not setup DownGate since receiving session is not known.");
						}
					}
					
					// set information from peer
					process.setRequester(pRequester, getProcessNumber(), pPacket.getReturnRoute());
					process.start();
				}
				catch(NetworkException exc) {
					node.getLogger().err(this, "Failure during starting of " +process, exc);
				}
			}
			
			if(process instanceof ProcessGateConstruction) {
				try {
					((ProcessGateConstruction) process).update(getGateNumber(), mPeerDestinationRoutingName, pRequester);
				}
				catch(NetworkException exc) {
					node.getLogger().err(this, "Error during update of " +process, exc);
				}
			} else {
				// wrong process type
				node.getLogger().err(this, "Process " +process +" has wrong type for opening DownGate.");
			}
		} else {
			// FN without link to lower layer
			node.getLogger().err(this, "FN " +pFN +" does not belong to a network interface.");
		}
		
		return true;
	}
	
	/**
	 * Sets information locally required to setup gate.
	 */
	public void setReceiveSession(LowerLayerSession pSession)
	{
		mReceiveSession = pSession;
	}

	@Viewable("Peer node FoG attachment name")
	private Name mPeerNodeAttachmentName;
	
	@Viewable("Peer forwarding node routing name")
	private Name mPeerDestinationRoutingName;
	
	@Viewable("Session, which received message")
	private transient LowerLayerSession mReceiveSession;
}
