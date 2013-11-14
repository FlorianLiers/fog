package de.tuilmenau.ics.fog.transfer.manager;

import java.io.Serializable;

import net.rapi.Description;
import net.rapi.Identity;
import net.rapi.Name;
import net.rapi.NetworkException;
import net.rapi.Signature;
import de.tuilmenau.ics.fog.Config;
import de.tuilmenau.ics.fog.FoGEntity;
import de.tuilmenau.ics.fog.IContinuation;
import de.tuilmenau.ics.fog.IEvent;
import de.tuilmenau.ics.fog.application.util.Session;
import de.tuilmenau.ics.fog.packets.GateInformationMessage;
import de.tuilmenau.ics.fog.packets.Packet;
import de.tuilmenau.ics.fog.transfer.Gate;
import de.tuilmenau.ics.fog.transfer.forwardingNodes.GateContainer;
import de.tuilmenau.ics.fog.transfer.gates.DirectDownGate;
import de.tuilmenau.ics.fog.util.RequestResponseTimer;


/**
 * Bridge between connection through lower layer and FoG.
 * Forwards lower layer data to FoG and informs ProcessDownGate about events.
 * 
 * TODO events lost in case of multithreading?
 */
public class LowerLayerSession extends Session
{
	private static final int MAX_NUMBER_RETRIES_SIGNALING = 4;
	
	
	public LowerLayerSession(FoGEntity node, LowerLayerObserver lowerLayer, IContinuation<Gate> callback)
	{
		super(false, node.getLogger(), null);
		
		this.node = node;
		this.lowerLayer = lowerLayer;
		this.callback = callback;
	}
	
	@Override
	public void connected()
	{
		isAuthenticated = false;
		
		// get name if known
		peerBindingName = getConnection().getBindingName();
		
		// active or passive role?
		if(peerBindingName != null) {
			// 0. seems to be an active open
			
			// 1. create gate representing the connectivity to peer
			createGate();
			
			// 2. start signaling process and request reverse gate from peer
			GateInformationMessage requ = new GateInformationMessage(lowerLayer.getAttachmentName(), gate.getGateID(), lowerLayer.getMultiplexerGate());

			timer = new RequestResponseTimer(node.getTimeBase(), new Packet(requ), MAX_NUMBER_RETRIES_SIGNALING, Config.PROCESS_STD_TIMEOUT_SEC) {
				@Override
				protected boolean retryRequired()
				{
					return !isAuthenticated;
				}
				
				@Override
				protected void timeout()
				{
					logger.err(this, "No response from peer for " +getPacket() +". Closing.");
					stop();
				}
				
				@Override
				protected void sendRequest()
				{
					try {
						getConnection().write(getPacket());
					}
					catch(NetworkException exc) {
						logger.err(this, "Can not send packet " +getPacket(), exc);
					}
				}
			};
			
			// send request
			timer.start();
		} else {
			// seems to be passive open; wait for request from peer
			node.getTimeBase().scheduleIn(Config.PROCESS_STD_TIMEOUT_SEC, new IEvent() {
				@Override
				public void fire()
				{
					logger.err(this, "No request from peer. Closing.");
					stop();
				}
			});
		}
	}
	
	@Override
	public boolean receiveData(Object obj)
	{
		if(obj instanceof Packet) {
			Packet packet = (Packet) obj;
			
			// clear overhead data
			packet.clearDownRoute();
			
			// do we know our peer?
			if(isAuthenticated) {
				lowerLayer.getMultiplexerGate().handlePacket(packet, null);
			} else {
				Serializable data = packet.getData();
				if(data instanceof GateInformationMessage) {
					GateInformationMessage msg = (GateInformationMessage) data;
					
					// does gate already exist?
					if(gate == null) {
						createGate();
					}
					
					try {
						getLogger().log(this, "Update gate " +gate +" with " +msg);
						
						// store the binding name of the peer
						Name msgPeerBindingName = msg.getPeerBindingName();
						if(msgPeerBindingName != null) {
							if(peerBindingName != null) {
								// check, if the name in the message is different than the name we used so far
								if(!peerBindingName.equals(msgPeerBindingName)) {
									getLogger().warn(this, "Binding name of answer (" +msgPeerBindingName +" differs from request ("+ peerBindingName +"). Using latter.");
									peerBindingName = msgPeerBindingName;
								}
							} else {
								peerBindingName = msgPeerBindingName;
							}
						}
						
						// try to extract the identity of the sender
						Identity peerIdentity = null;
						if(packet.getAuthentications() != null) {
							Signature sign = packet.getAuthentications().getFirst();
							if(sign != null) {
								peerIdentity = sign.getIdentity();
							}
						}
						
						// update gate with information of msg
						gate.update(msg.getPeerGateNumber(), msg.getPeerRoutingName(), peerIdentity);
						
						isAuthenticated = true;
						
						// are we waiting for this answer?
						if(timer != null) {
							// yes; cancel timer
							timer.cancel();
							timer = null;
						} else {
							// no; send answer
							getConnection().write(new Packet(new GateInformationMessage(lowerLayer.getAttachmentName(), gate.getGateID(), lowerLayer.getMultiplexerGate())));					
						}
						
						// do we have to inform someone about setup?
						if(callback != null) {
							callback.success(gate);
							callback = null;
						}
					}
					catch(NetworkException exc) {
						getLogger().err(this, "Error while updating gate " +gate, exc);
					}
					
				} else {
					getLogger().err(this, "Wrong data received. Packet " +packet +" dropped.");
				}
			}
		} else {
			getLogger().err(this, "Unknown data type of received content " +obj);
		}
		
		return true;
	}

	@Override
	public void error(Throwable errorDescription)
	{
		logger.err(this, "Stopping due to error from connection.", errorDescription);
		
		if(callback != null) {
			callback.failure(gate, errorDescription);
			callback = null;
		}
		
		stop();
	}
	
	@Override
	public void stop()
	{
		if(!isStopped()) {
			super.stop();
		}
		
		if(timer != null) {
			timer.cancel();
			timer = null;
		}
		
		// last resort since gate and error might be unknown
		if(callback != null) {
			callback.failure(gate, null);
			callback = null;
		}		
		
		deleteGate();
	}
	
	/**
	 * @return Peer FoG entity binding name; null, if not known 
	 */
	public Name getPeerBindingName()
	{
		if(peerBindingName != null) {
			return peerBindingName;
		} else {
			return getConnection().getBindingName();
		}
	}
	
	/**
	 * @return Observer for lower layer, to which the session belongs to
	 */
	public LowerLayerObserver getLowerLayer()
	{
		return lowerLayer;
	}
	
	private void createGate()
	{
		GateContainer fn = lowerLayer.getMultiplexerGate();
		gate = new DirectDownGate(node, this, null);
		
		fn.registerGate(gate);
		logger.log(this, "create gate " +gate +" at " +fn);
		
		// switch it to init state
		gate.initialise();
					
		if(Config.Connection.TERMINATE_WHEN_IDLE) {
			Description tRequ = getConnection().getRequirements();
			
			if(tRequ != null) {
				if(!tRequ.isBestEffort()) {
					gate.startCheckForIdle();
				}
			}
		}
	}
	
	private void deleteGate()
	{
		if(gate != null) {
			logger.log(this, "deleting gate " +gate +" at " +lowerLayer.getMultiplexerGate());
			
			// clear internal variable first, since the call of
			// shutdown might result in a recursive call to stop
			DirectDownGate tempGate = gate;
			gate = null;
			
			tempGate.shutdown();
			
			// register gate at FN responsible for lower layer
			lowerLayer.getMultiplexerGate().unregisterGate(tempGate);
		}
	}
	
	private FoGEntity node;
	private LowerLayerObserver lowerLayer;
	private IContinuation<Gate> callback;
	
	/**
	 * Indicates if the peers of the session exchanged authentication/information messages
	 */
	private boolean isAuthenticated = false;
	
	/**
	 * Name of the FoG binding of the remote peer
	 */
	private Name peerBindingName = null;
	
	/**
	 * Timer for authentication/information messages in authentication phase
	 */
	private RequestResponseTimer timer = null;
	
	/**
	 * Gate representing the session in a FoG network
	 */
	private DirectDownGate gate = null;
}
