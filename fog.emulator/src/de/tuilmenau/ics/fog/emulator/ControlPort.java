package de.tuilmenau.ics.fog.emulator;

import java.io.Serializable;

import net.rapi.Name;
import net.rapi.events.PeerInformationEvent;
import de.tuilmenau.ics.fog.emulator.packets.BindingReply;
import de.tuilmenau.ics.fog.emulator.packets.BindingRequest;
import de.tuilmenau.ics.fog.emulator.packets.Packet;
import de.tuilmenau.ics.fog.routing.naming.HierarchicalNameMappingService;
import de.tuilmenau.ics.fog.routing.naming.NameMappingEntry;
import de.tuilmenau.ics.fog.transfer.TransferPlaneObserver.NamingLevel;
import de.tuilmenau.ics.fog.ui.Viewable;
import de.tuilmenau.ics.fog.util.Logger;


public class ControlPort implements Port
{
	public ControlPort(EmulatorLayer layer)
	{
		this.logger = layer.getLogger();
		this.layer = layer;
		
		this.nameMapping = new HierarchicalNameMappingService<PortID>(null, this.logger);
	}
	
	@Override
	public int getPortNumber()
	{
		// default port for signaling messages
		return EmulatorLayer.CONTROL_PORT;
	}

	@Override
	public void handlePacket(Packet packet)
	{
		Serializable data = packet.getData();
		
		if(data instanceof BindingReply) {
			BindingReply msg = (BindingReply) data;
			PortID destination = packet.getSender();
			
			// already known mapping?
			if(!nameMapping.contains(msg.getBindingName(), destination)) {
				logger.log(this, "Received new mapping '" +msg.getBindingName() + "' to " +destination);
				
				nameMapping.registerName(msg.getBindingName(), destination, NamingLevel.NAMES);
				
				layer.notifyObservers(new PeerInformationEvent(layer, new EmulatorNeighborName(msg.getBindingName(), packet.getSender()), true));
			} else {
				// refresh entry
				logger.log(this, "Refreshing mapping '" +msg.getBindingName() + "' to " +destination);

				// TODO
			}
		}
		/*else if(data instanceof BindingRequest) {
			// TODO
		}*/
		else {
			logger.warn(this, "Received message '" +data + "' of unknown type.");
		}
	}
	
	public NameMappingEntry<PortID>[] getAddresses(Name name)
	{
		return nameMapping.getAddresses(name);
	}
	
	public Iterable<Name> getAllNames()
	{
		return nameMapping.getAllNames();
	}

	@Viewable("Name to address mapping for bindings")
	private HierarchicalNameMappingService<PortID> nameMapping;
	
	private Logger logger;
	private EmulatorLayer layer;
}
