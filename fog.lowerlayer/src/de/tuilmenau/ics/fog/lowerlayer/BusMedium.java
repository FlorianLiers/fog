package de.tuilmenau.ics.fog.lowerlayer;

import java.io.IOException;
import java.io.Serializable;
import java.util.Arrays;
import java.util.Observable;
import java.util.Random;

import de.tuilmenau.ics.CommonSim.datastream.DatastreamManager;
import de.tuilmenau.ics.CommonSim.datastream.StreamTime;
import de.tuilmenau.ics.CommonSim.datastream.annotations.AutoWire;
import de.tuilmenau.ics.CommonSim.datastream.numeric.DoubleNode;
import de.tuilmenau.ics.CommonSim.datastream.numeric.IDoubleWriter;
import de.tuilmenau.ics.fog.Config;
import de.tuilmenau.ics.fog.Config.Simulator.SimulatorMode;
import de.tuilmenau.ics.fog.EventHandler;
import de.tuilmenau.ics.fog.IEvent;
import de.tuilmenau.ics.fog.IEventRef;
import net.rapi.Description;
import net.rapi.Layer;
import net.rapi.properties.DatarateProperty;
import net.rapi.properties.DelayProperty;
import net.rapi.properties.Property;
import net.rapi.properties.LossRateProperty;
import net.rapi.properties.PropertyException;
import net.rapi.properties.MinMaxProperty.Limit;
import de.tuilmenau.ics.fog.facade.DescriptionHelper;
import de.tuilmenau.ics.fog.packets.Packet;
import de.tuilmenau.ics.fog.topology.AutonomousSystem;
import de.tuilmenau.ics.fog.topology.Medium;
import de.tuilmenau.ics.fog.topology.Node;
import de.tuilmenau.ics.fog.topology.RemoteMedium;
import de.tuilmenau.ics.fog.topology.Simulation;
import de.tuilmenau.ics.fog.transfer.gates.headers.NumberingHeader;
import de.tuilmenau.ics.fog.ui.PacketLogger;
import de.tuilmenau.ics.fog.ui.Viewable;
import de.tuilmenau.ics.fog.util.Logger;
import de.tuilmenau.ics.fog.util.RateMeasurement;
import de.tuilmenau.ics.fog.util.Size;


public class BusMedium extends Observable implements Medium
{
	/**
	 * Dis-/Enables statistic information output. Just done in GUI mode,
	 * since such detailed informations are not needed in large batch mode simulations. 
	 */
	public static final boolean OUTPUT_STATISTICS_VIA_DATASTREAM = Config.Simulator.MODE != SimulatorMode.FAST_SIM;
	

	public BusMedium(AutonomousSystem pAS, String pName, Description pDescr)
	{
		mEventHandler = pAS.getTimeBase();
		mASName = pAS.getName();
		mLogger = new Logger(pAS.getLogger());
		mName = pName;
		mSim = pAS.getSimulation();
		mConfig = mSim.getConfig();
		
		packetLog = PacketLogger.createLogger(mEventHandler, this, null);

		mDescription = new Description();
		setDataRate(mConfig.Scenario.DEFAULT_DATA_RATE_KBIT, mConfig.Scenario.DEFAULT_DATA_RATE_VARIANCE);
		setDelayMSec(mConfig.Scenario.DEFAULT_DELAY_MSEC);
		setPacketLossProbability(mConfig.Scenario.DEFAULT_PACKET_LOSS_PROP);
		setBitErrorProbability(mConfig.Scenario.DEFAULT_BIT_ERROR_PROP);
		
		// if a description is given, override the default values
		if(pDescr != null) {
			Property prop = pDescr.get(DatarateProperty.class);
			if(prop != null) {
				setDataRate(((DatarateProperty) prop).getMax(), ((DatarateProperty) prop).getVariance());
			}
			
			prop = pDescr.get(DelayProperty.class);
			if(prop != null) {
				setDelayMSec(((DelayProperty) prop).getMin());
			}
			
			prop = pDescr.get(LossRateProperty.class);
			if(prop != null) {
				setPacketLossProbability(((LossRateProperty) prop).getLossRate());
			}
		}
		
		if(OUTPUT_STATISTICS_VIA_DATASTREAM) {
			DatastreamManager.autowire(this);
			
			mDatarateMeasurement = new RateMeasurement(mEventHandler, this +".rate");
		}
		
		central = new BusLayer(this);
	}
	
	@Override
	public Layer attach(Node node)
	{
		mLogger.trace(this, "Generate layer object for " +node);
		
		return central.attach(node);
	}

	@Override
	public Layer detach(Node node)
	{
		mLogger.trace(this, "Remove layer object for " +node);
		
		return central.detach(node);
	}

	@Override
	public void deleted()
	{
		central.deleted();
		central = null;
		
		setBroken(true, Config.Routing.ERROR_TYPE_VISIBLE);
		
		packetLog.close();
		packetLog = null;
	}
	
	@Override
	public RemoteMedium getProxy()
	{
		// TODO Auto-generated method stub
		return null;
	}
	
	private boolean isBusFree()
	{
		// end of last transmission reached?
		return (currentPacket == null);
	}
	
	public boolean packetAvailable(Connection conn, ConnectionEndPoint source)
	{
		if(isBusFree()) {
			Serializable nextPacket = source.getOutboundPacket();
			if(nextPacket != null) {
				sendPacket(nextPacket, conn.getPeerFor(source));
				return true;
			}
		}
		// else: we wait for the end event of the packet currently in transit
		
		return false;
	}
	
	private void sendPacket(Serializable packet, ConnectionEndPoint destination)
	{
		double tNow = getTimeBase().now();
		StreamTime tNowStream = null;
		if(OUTPUT_STATISTICS_VIA_DATASTREAM) {
			tNowStream = new StreamTime(tNow);
		}

		//
		// Error handling
		//
		boolean packetLost = isPacketLost(packet);
		
		if(packetLost) {
			generateByteErrors(packet);
		}

		if(OUTPUT_STATISTICS_VIA_DATASTREAM) {
			if(packetLost) {
				mDroppedPackets.write(1.0d, tNowStream);
			} else {
				mDroppedPackets.write(0.0d, tNowStream);
			}
		}
		
		//
		// Calculate timing issues
		//
		double tDelayForPacket = 0;
		if(mConfig.Scenario.DEFAULT_DELAY_CONSTANT) {
			tDelayForPacket += mDelaySec;
		} else {
			if(mBandwidth.floatValue() >= 0) {
				// 1000 * kbit/s = bit/s
				// bit/s / 8 = byte/s
				double tBytesPerSecond = 1000 * mBandwidth.floatValue() / 8;
				
				double size = 0;
				if(packet instanceof Packet) {
					size = ((Packet) packet).getSerialisedSize();
				} else {
					size = Size.sizeOf(packet);
				}
				
				tDelayForPacket += size / tBytesPerSecond;
			}
			// else: data rate is infinity (no delay)
		}
		
		if(Config.Transfer.DEBUG_PACKETS) {
			if(mConfig.Scenario.DEFAULT_DELAY_CONSTANT) {
				mLogger.debug(this, "Bus delay is " + tDelayForPacket + "s");
			} else {
				mLogger.debug(this, "Bus data rate is " + mBandwidth + "kbit/s and packet takes " +tDelayForPacket +"s delay");
			}
		}

		// Statistic
		if(packet instanceof Packet) {
			((Packet)packet).addBus(mName);
			
			packetLog.add((Packet) packet);
		}
		
		//
		// Create delivery event
		//
		PacketDeliveryEvent delivery;
		if(packetLost) {
			delivery = new PacketDeliveryEvent(packet, null);
		} else {
			delivery = new PacketDeliveryEvent(packet, destination);
		}
		
		currentPacket = getTimeBase().scheduleIn(tDelayForPacket, delivery);		
	}
	
	public String getName()
	{
		return mName;
	}
	
	public EventHandler getTimeBase()
	{
		return mEventHandler;
	}
	
	public Logger getLogger()
	{
		return mLogger;
	}
	
	public synchronized Description getDescription()
	{
		return mDescription;
	}
	
	public int getBitErrorProbability()
	{
		return (int) (mBitErrorRate *100.0f);
	}
	
	public void setBitErrorProbability(int newError)
	{
		if(newError < 0) newError = 0;
		if(newError > 100) newError = 100;
		
		mBitErrorRate = (float) newError / 100.0f;
	}

	public int getPacketLossProbability()
	{
		return (int) (mPacketLossRate *100.0f);
	}
	
	public void setPacketLossProbability(int newLoss)
	{
		if(newLoss < 0) newLoss = 0;
		if(newLoss > 100) newLoss = 100;
		
		mPacketLossRate = (float) newLoss / 100.0f;
		
		// update description
		mDescription.set(new LossRateProperty(getPacketLossProbability(), Limit.MIN));
	}
	
	private void setDataRate(int newBandwidth, double newBandwidthVariance)
	{
		mBandwidth = newBandwidth;
		
		if(mBandwidth.floatValue() > 0) {
			// update description
			mDescription.set(new DatarateProperty(mBandwidth.intValue(), newBandwidthVariance, Limit.MAX));
		} else {
			// Infinite data rate:
			// remove previous limits from list
			mDescription.remove(mDescription.get(DatarateProperty.class));
		}
	}
	
	public synchronized void modifyBandwidth(int bandwidthModification)
	{
		DatarateProperty dr = (DatarateProperty) mDescription.get(DatarateProperty.class);
		
		if(dr != null) {
			if(dr.getMax() > 0) {
				dr = new DatarateProperty(dr.getMax() +bandwidthModification, Limit.MAX);
				
				mDescription.set(dr);
			}
		}
	}
	
	public long getDelayMSec()
	{
		return Math.round(mDelaySec * 1000.0d);
	}
	
	public void setDelayMSec(long newDelayMSec)
	{
		mDelaySec = Math.max(0, (double) newDelayMSec / 1000.0d);
		
		// update description
		mDescription.set(new DelayProperty((int)getDelayMSec(), Limit.MIN));
	}
	
	public synchronized void reserveResources(Description requirements) throws PropertyException
	{
		// any limits given?
		if(requirements != null) {
			// Check, if the layer supports the requirements.
			// The following method will throw an exception if not.
			// Do not use the return value; we will reserve exactly the requested resources.
			DescriptionHelper.deriveRequirements(mDescription, requirements);
			
			// reserve resources
			DatarateProperty datarateUsage = (DatarateProperty) requirements.get(DatarateProperty.class);
			if(datarateUsage != null) {
				// is it really a requirement?
				if(!datarateUsage.isBE()) {
					// reserve bandwidth
					modifyBandwidth(-datarateUsage.getMax());
				}
			}
		}
	}
	
	public synchronized void freeResources(Description requirements)
	{
		// any reservations given?
		if(requirements != null) {
			// free resources
			DatarateProperty datarateUsage = (DatarateProperty) requirements.get(DatarateProperty.class);
			if(datarateUsage != null) {
				// is it really a requirement?
				if(!datarateUsage.isBE()) {
					// free bandwidth
					modifyBandwidth(+datarateUsage.getMax());
				}
			}
		}
	}
	
	/**
	 * @return Number of all packets processed by the bus.
	 */
	public int getNumberPackets()
	{
		if(packetLog != null) {
			return packetLog.getPacketCounter();
		} else {
			return 0;
		}
	}
	
	/**
	 * Just for GUI purposes.
	 */
	public BusLayer getLayer()
	{
		return central;
	}
	
	public Status isBroken()
	{
		if(broken) {
			if(mErrorTypeVisible) {
				return Status.BROKEN;
			} else {
				return Status.UNKNOWN_ERROR;
			}
		}
		
		return Status.OK;
	}
		
	public void setBroken(boolean pBroken, boolean pErrorTypeVisible)
	{
		// does the state change?
		if(broken != pBroken) {
			// is it a repair operation?
			boolean repaired = broken && !pBroken; 
			
			broken = pBroken;
			if(broken) {
				mErrorTypeVisible = pErrorTypeVisible;
				
				central.disconnected();
			} else {
				// reset it to default
				mErrorTypeVisible = Config.Routing.ERROR_TYPE_VISIBLE;
			}
			
			notifyObservers(pBroken);
			
			// initiate the repair operation
			if(repaired) {
				mEventHandler.scheduleIn(0, new IEvent() {
					@Override
					public void fire()
					{
						central.connected();
					}
				});
			}
		}
	}
	
	/**
	 * @return Random decision if a packet gets lost in the lower layer. 
	 */
	private boolean isPacketLost(Serializable packet)
	{
		if((mPacketLossRate > 0) && !isSpecialPacket(packet)) {
			return randomGenerator.nextFloat() <= mPacketLossRate;
		} else {
			return false;
		}
	}

	/**
	 * @return Randomize bytes in the payload (only type byte[]!) of a packet. 
	 * The loss rate defines the amount of packets, related to 100%, which should have errors. 
	 * The amount of errors per packet also depends on the defined error rate.
	 * If packet loss is zero no bits will be modified!
	 */
	private boolean generateByteErrors(Serializable data)
	{
		if((mPacketLossRate > 0) && (mBitErrorRate > 0) && !isSpecialPacket(data) && (randomGenerator.nextFloat() <= mBitErrorRate)) {
			if(data instanceof Packet) {
				Packet packet = (Packet) data;
				float tAdaptedLoss = mBitErrorRate * 10;
				Object tPacketData = packet.getData();
				Object tUserData = null;
				
				if(tPacketData instanceof NumberingHeader) {
					NumberingHeader tNumberHeader = (NumberingHeader)packet.getData();
					packet.setData(tNumberHeader.clone());
					tUserData = tNumberHeader.getData();
				} else {
					tUserData = packet.getData();
				}
				
				if(tUserData instanceof byte[])	{
					byte[] tPacketPayloadArray = (byte[])tUserData;
					byte[] tPacketPayloadArrayCopy = Arrays.copyOf(tPacketPayloadArray, tPacketPayloadArray.length);
					
					for(int i = 64 /* protect headers */; i < tPacketPayloadArrayCopy.length; i++)	{
						if (randomGenerator.nextFloat() <= tAdaptedLoss)
							tPacketPayloadArrayCopy[i] = (byte) (127 * (-randomGenerator.nextFloat())); 
					}
					if(tPacketData instanceof NumberingHeader) {
						NumberingHeader tNumberHeader = (NumberingHeader)packet.getData();
						tNumberHeader.setData(tPacketPayloadArrayCopy);
						tNumberHeader.setIsCorrupted();
					} else {
						packet.setData(tPacketPayloadArrayCopy);
					}
					return true;
				}
			}
			// else: TODO
		}
		return false;
	}
	
	private boolean isSpecialPacket(Serializable packet)
	{
		if(packet instanceof Packet) {
			return ((Packet) packet).isInvisible() || ((Packet) packet).isSignalling();
		}
		
		return false;
	}
	
	public String toString()
	{
		return mName;
	}
	
	private Simulation mSim;
	private EventHandler mEventHandler;
	private Logger mLogger;
	private Config mConfig;
	
	private PacketLogger packetLog;
	private static Random randomGenerator = new Random();
	
	private BusLayer central;
	
	private IEventRef currentPacket = null;
	
	@Viewable("Broken")
	private Boolean broken = false;
	private boolean mErrorTypeVisible = Config.Routing.ERROR_TYPE_VISIBLE;
	
	@Viewable("AS name")
	private String mASName = null;
	@Viewable("Name")
	private String mName = null;

	@AutoWire(name="DeliveredPackets", type=DoubleNode.class, unique=true, prefix=true)
	private IDoubleWriter mDeliveredPackets;
	
	@AutoWire(name="AheadOfTime", type=DoubleNode.class, unique=true, prefix=true)
	private IDoubleWriter mAheadOfTime;
	
	@AutoWire(name="DroppedPackets", type=DoubleNode.class, unique=true, prefix=true)
	private IDoubleWriter mDroppedPackets;
	
	private RateMeasurement mDatarateMeasurement = null;
	
	//
	// QoS parameters for lower layer
	//
	@Viewable("Description")
	private Description mDescription;
	@Viewable("Bandwidth")
	private Number mBandwidth;
	@Viewable("Delay (sec)")
	private double mDelaySec;
	@Viewable("Loss probability")
	private float mPacketLossRate;
	@Viewable("Bit error probability")
	private float mBitErrorRate;

	/**
	 * Internal event for the end of a packet transmission.
	 */
	private class PacketDeliveryEvent implements IEvent
	{
		public PacketDeliveryEvent(Serializable packet, ConnectionEndPoint destination)
		{
			this.packet = packet;
			this.dest = destination;
		}

		@Override
		public void fire()
		{
			currentPacket = null;
			
			// is packet dropped?
			if(dest != null) {
				// deliver packet
				try {
					dest.storeDataForApp(packet);
				}
				catch(IOException exc) {
					mLogger.err(this, "Can not deliver packet " +packet, exc);
				}
			} else {
				if (Config.Transfer.DEBUG_PACKETS) mLogger.log(this, "Lost packet " +packet);
								
				// log end result of packet
				if(OUTPUT_STATISTICS_VIA_DATASTREAM) {
					mDroppedPackets.write(1.0d, new StreamTime(getTimeBase().now()));
				}
				
				// special treatment for packets from FoG
				if(packet instanceof Packet) {
					packetLog.add((Packet)packet);
					
					// inform it about end
					((Packet) packet).logStats(mSim, this);
				}
			}
			
			// search next packet for delivery
			central.selectNextPacketForTransmission();
		}
		
		private Serializable packet;
		private ConnectionEndPoint dest;
	}

}
