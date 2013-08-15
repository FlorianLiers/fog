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
package de.tuilmenau.ics.fog.transfer.forwardingNodes;


import net.rapi.Binding;
import net.rapi.Connection;
import net.rapi.Description;
import net.rapi.Identity;
import net.rapi.Name;
import net.rapi.impl.base.BaseBinding;
import net.rapi.properties.CommunicationTypeProperty;
import de.tuilmenau.ics.fog.FoGEntity;
import de.tuilmenau.ics.fog.packets.Packet;
import de.tuilmenau.ics.fog.packets.PleaseOpenConnection;
import de.tuilmenau.ics.fog.routing.Route;
import de.tuilmenau.ics.fog.transfer.TransferPlaneObserver.NamingLevel;


/**
 * Forwarding node representing a name registration from a higher layer.
 * The name was given by the higher layer. Incoming connection request
 * had to be accepted by the higher layer. The description lists the
 * requirements of the server for connections.
 */
public class ServerFN extends Multiplexer
{
	public ServerFN(FoGEntity entity, Name name, NamingLevel level, Description description, Identity identity)
	{
		super(entity, name, level, false, identity, entity.getController());
		
		this.description = description;
		
		binding = new BindingImpl();
	}
	
	/**
	 * Initializes forwarding node
	 */
	public void open()
	{
		mEntity.getTransferPlane().registerNode(this, mName, mLevel, description);
	}

	/**
	 * @return Description of the server requirements for all communications
	 */
	public Description getDescription()
	{
		return description;
	}
	
	@Override
	protected void handleDataPacket(Packet packet)
	{
		// are we allowed to open connections implicitly?
		CommunicationTypeProperty type = null;
		if(description != null) {
			type = (CommunicationTypeProperty) description.get(CommunicationTypeProperty.class);
		}
		if(type == null) {
			type = CommunicationTypeProperty.getDefault();
		}
		
		if(!type.requiresSignaling()) {
			PleaseOpenConnection artSigMsg = new PleaseOpenConnection(description);
			
			artSigMsg.setSendersRouteUpToHisClient(new Route());
			artSigMsg.execute(this, packet);
		} else {
			mLogger.err(this, "Binding is not allowed to open connection implicitly due to type " +type);
		}
	}
	
	public Binding getBinding()
	{
		return binding;
	}
	
	public void addNewConnection(Connection conn)
	{
		binding.addIncomingConnection(conn);
	}

	class BindingImpl extends BaseBinding
	{
		public BindingImpl()
		{
			super(ServerFN.this.getName(), description, null);
		}
		
		@Override
		public void close()
		{
			super.close();
			
			ServerFN.this.close();
		}
		
		@Override
		public boolean isActive()
		{
			return true;
		}

		@Override
		protected void notifyFailure(Throwable failure, EventListener listener)
		{
			getEntity().getLogger().warn(this, "Ignoring failure in event listener " +listener, failure);
		}
	}

	private Description description;
	private BindingImpl binding;
}
