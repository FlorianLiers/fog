package de.tuilmenau.ics.fog.lowerlayer;

import java.util.HashMap;
import java.util.LinkedList;

import net.rapi.Binding;
import net.rapi.Description;
import net.rapi.Identity;
import net.rapi.Layer.LayerStatus;
import net.rapi.Name;
import net.rapi.NeighborName;
import net.rapi.NetworkException;
import net.rapi.impl.base.BaseEventSource;
import net.rapi.impl.base.SimpleNeighborName;
import net.rapi.properties.PropertyException;
import de.tuilmenau.ics.fog.topology.Node;


public class BusLayer extends BaseEventSource
{
	public BusLayer(BusMedium medium)
	{
		this.medium = medium;
	}
	
	/**
	 * Returns an object, which represents the attachment of a node to a medium.
	 * 
	 * @return Point of attachment object
	 */
	public BusEntity attach(Node node)
	{
		if(medium != null) {
			BusEntity layer = layers.get(node);
			
			// no entry already available?
			if(layer == null) {
				layer = new BusEntity(this, node);
				
				layers.put(node, layer);
			}
			
			return layer;
		} else {
			throw new RuntimeException(this +" - Bus had been deleted from simulation.");
		}
	}
	
	public BusEntity detach(Node node)
	{
		BusEntity layer = layers.remove(node);
		
		// entry found?
		if(layer != null) {
			layer.deleted();
		}
			
		return layer;
	}
	
	/**
	 * Called if medium is deleted from simulation.
	 */
	public void deleted()
	{
		medium = null;
		
		// set all bindings to error state
		NetworkException exc = new NetworkException(this, "Layer had been deleted from simulation.");
		for(LinkedList<BindingImpl> bindingList : bindings.values()) {
			for(BindingImpl binding : bindingList) {
				binding.setError(exc);
			}
		}
		bindings.clear();
		
		layers.clear();
	}
	
	public LayerStatus getStatus()
	{
		if(medium == null) return LayerStatus.ERROR;
		else {
			switch(medium.isBroken()) {
			case OK: return LayerStatus.OPERATING;
			case BROKEN: return LayerStatus.DISCONNECTED;
			default:
				// error type not visible; do not hint at broken layer
				return LayerStatus.OPERATING;
			}
		}
	}
	
	public synchronized Binding bind(BusEntity layer, Name name, Description requ, Identity identity)
	{
		BindingImpl binding;

		if(medium != null) {
			if(name != null && !name.equals("")) {
				medium.getLogger().log(this, "Bind " +identity +" to name " +name +" (requ=" +requ +")");
				
				binding = new BindingImpl(medium.getLogger(), name, requ, identity);
				LinkedList<BindingImpl> bindingsLayer = bindings.get(layer);
				
				if(bindingsLayer == null) {
					bindingsLayer = new LinkedList<BindingImpl>();
					bindings.put(layer, bindingsLayer);
				}
		
				bindingsLayer.add(binding);
				
				// inform observers about potential new peer
				informNeighborObserver(binding, true);
			} else {
				binding = new BindingImpl(medium.getLogger(), name, new NetworkException(this, "Name not valid"));
			}
		} else {
			binding = new BindingImpl(medium.getLogger(), name, new NetworkException(this, "Medium had been deleted."));
		}
		
		return binding;
	}
	
	public synchronized boolean unbind(BusEntity layer, BindingImpl binding)
	{
		LinkedList<BindingImpl> bindingsLayer = bindings.get(layer);
		
		if(bindingsLayer != null) {
			if(bindingsLayer.remove(binding)) {
				informNeighborObserver(binding, false);
				
				return true;
			}
		}
		
		return false;
	}
	
	private void informNeighborObserver(BindingImpl binding, boolean appeared)
	{
		NeighborName peer = new SimpleNeighborName(binding.getName());
		
		for(BusEntity attachedNode : layers.values()) {
			attachedNode.informAboutNewNeighbor(peer, appeared);
		}
	}

	public synchronized ConnectionEndPoint connect(BusEntity entity, Name name, Description requirements, Identity identity)
	{	
		if(medium != null) {
			medium.getLogger().log(this, "Connect " +identity +" to " +name +" with requirements " +requirements);
			
			// determine binding to connect to
			BindingImpl binding = getBinding(name);
			if(binding != null) {
				// modify available resources for medium
				try {
					medium.reserveResources(requirements);
				}
				catch(PropertyException exc) {
					return new ConnectionEndPoint(medium.getLogger(), exc);
				}
				
				// create connection
				Connection conn = new Connection(entity, binding, medium, identity, name, requirements);
				connections.add(conn);
				
				// accept connection for requester
				conn.getPeer1().connect();

				// inform peer about connection
				binding.addIncomingConnection(conn.getPeer2());
				
				return conn.getPeer1();
			} else {
				// binding not known; return dummy
				return new ConnectionEndPoint(medium.getLogger(), new NetworkException(this, "Name '" +name +"' not known."));
			}
		} else {
			// medium deleted from simulation
			return new ConnectionEndPoint(medium.getLogger(), new NetworkException(this, "Medium had been deleted."));
		}
	}
	
	public void connectionClosed(Connection connection)
	{
		// remove connection from list
		if(connections.remove(connection)) {
			// release own resources reserved for connection
			medium.freeResources(connection.getRequirements());
		} else {
			medium.getLogger().err(this, "Connection " +connection +" does not belong to bus.");
		}
	}

	/**
	 * Returns one entry per neighbor matching search criteria.
	 * 
	 * @param namePrefix Optional; neighbor is included if it equals this parameter OR if parameter is null
	 * @return List of neighbors (!= null)
	 */
	public Iterable<NeighborName> getNeighbors(Name namePrefix)
	{
		LinkedList<NeighborName> neighborlist = new LinkedList<NeighborName>();
		
		for(LinkedList<BindingImpl> bindingList : bindings.values()) {
			for(BindingImpl binding : bindingList) {
				// check, if binding name equals given name
				if(namePrefix != null) {
					if(namePrefix.equals(binding.getName())) {
						neighborlist.addLast(new SimpleNeighborName(binding.getName()));
					}
				} else {
					// report all neighbors
					neighborlist.addLast(new SimpleNeighborName(binding.getName()));
				}
			}
		}
		
		return neighborlist;
	}

	
	/**
	 * @param name Name of binding
	 * @return Binding for name or null if no binding with this name is registered
	 */
	public BindingImpl getBinding(Name name)
	{
		for(LinkedList<BindingImpl> bindingList : bindings.values()) {
			for(BindingImpl binding : bindingList) {
				// check, if binding name equals given name
				if(binding.getName().equals(name)) {
					return binding;
				}
			}
		}
		
		return null;
	}
	
	public LinkedList<Binding> getBindings()
	{
		LinkedList<Binding> list = new LinkedList<Binding>();
		
		for(LinkedList<BindingImpl> bindingList : bindings.values()) {
			for(BindingImpl binding : bindingList) {
				list.add(binding);
			}
		}
		
		return list;
	}
	
	public LinkedList<Binding> getBindings(BusEntity busEntity)
	{
		LinkedList<BindingImpl> bindingsLayer = bindings.get(busEntity);
		LinkedList<Binding> res = new LinkedList<Binding>();
		
		if(bindingsLayer != null) {
			for(BindingImpl bind : bindingsLayer) {
				res.addLast(bind);
			}
		}
		
		return res;
	}
	
	public LinkedList<Connection> getConnections()
	{
		return connections;
	}
	
	/**
	 * Scheduler; decides, which packet is send next
	 */
	public void selectNextPacketForTransmission()
	{
		for(Connection connection : connections) {
			if(connection.isEstablished()) {
				if(connection.hasPacket()) {
					try {
						if(connection.packetAvailable(null)) {
							return;
						}
					}
					catch(NetworkException exc) {
						medium.getLogger().err(this, "Ignoring exception while sending data for connection " +connection, exc);
					}
				}
			}
		}
	}
	
	public void connected()
	{
		for(BusEntity attachedNode : layers.values()) {
			attachedNode.connected();
		}
	}

	public void disconnected()
	{
		for(BusEntity attachedNode : layers.values()) {
			attachedNode.disconnected();
		}
	}
	
	public String toString()
	{
		return "Layer@" +medium;
	}
	
	@Override
	protected void notifyFailure(Throwable failure, EventListener listener)
	{
		// TODO Auto-generated method stub
		
	}
	
	private BusMedium medium;
	private HashMap<BusEntity, LinkedList<BindingImpl>> bindings = new HashMap<BusEntity, LinkedList<BindingImpl>>();
	private LinkedList<Connection> connections = new LinkedList<Connection>();
	private HashMap<Node, BusEntity> layers = new HashMap<Node, BusEntity>();
}
