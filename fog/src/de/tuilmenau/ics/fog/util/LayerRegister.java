package de.tuilmenau.ics.fog.util;

import java.util.Iterator;
import java.util.LinkedList;

import de.tuilmenau.ics.fog.topology.Medium;

import net.rapi.Layer;
import net.rapi.LayerContainer;
import net.rapi.events.PeerInformationEvent;
import net.rapi.impl.base.BaseEventSource;



public class LayerRegister extends BaseEventSource implements LayerContainer
{
	public LayerRegister()
	{
	}
	
	public void register(Medium medium, Layer layer)
	{
		if(layer != null) {
			if(layerList == null) layerList = new LinkedList<MediumLayerRelation>();
			else {
				// avoid duplicated entries
				if(contains(layer)) {
					return;
				}
			}
			
			layerList.addLast(new MediumLayerRelation(medium, layer));
			
			notifyObservers(new PeerInformationEvent(this, null, true));
		}
	}
	
	@Override
	public Layer getLayer(Class<?> layerClass)
	{
		if(layerList != null) {
			if(layerClass != null) {
				for(MediumLayerRelation mlr : layerList) {
					if(mlr.layer.getClass().equals(layerClass)) {
						return mlr.layer;
					}
				}
			} else {
				// return default layer
				return layerList.getFirst().layer;
			}
		}

		return null;
	}
	
	@Override
	public Layer[] getLayers(Class<?> layerClass)
	{
		Layer[] res = emptyList;
		
		if(layerList != null) {
			LinkedList<Layer> found = null;
			
			// find suitable layers
			for(MediumLayerRelation mlr : layerList) {
				boolean match = true;
				if(layerClass != null) {
					match = mlr.layer.getClass().isAssignableFrom(layerClass); 
				}
				
				if(match) {
					if(found == null) found = new LinkedList<Layer>();
					
					found.add(mlr.layer);
				}
			}
			
			// copy list to array
			if(found != null) {
				if(found.size() > 0) {
					res = new Layer[found.size()];
					for(int i = 0; i < found.size(); i++) {
						res[i] = found.get(i);
					}
				}
			}
		}
		
		return res;
	}
	
	public boolean unregister(Layer layer)
	{
		if(layerList != null) {
			Iterator<MediumLayerRelation> iter = layerList.iterator();
		
			while(iter.hasNext()) {
				MediumLayerRelation mlr = iter.next();
				
				if(mlr.layer == layer) {
					iter.remove();
					
					notifyObservers(new PeerInformationEvent(this, null, false));
					
					return true;
				}
			}
		}
		
		return false;
	}
	
	public boolean unregister(Medium medium)
	{
		int deletedCounter = 0;
		
		if(layerList != null) {
			LinkedList<MediumLayerRelation> found = null;

			// 1. find all candidates for the deletion
			for(MediumLayerRelation mlr : layerList) {
				if(mlr.medium == medium) {
					// lacy creation
					if(found == null) found = new LinkedList<MediumLayerRelation>();
					
					found.add(mlr);
				}
			}
			
			// 2. delete these candidates
			if(found != null) {
				for(MediumLayerRelation mlr : found) {
					if(layerList.remove(mlr)) {
						deletedCounter++;
					}
				}
			}
		}
		
		return deletedCounter > 0;
	}
	
	public int size()
	{
		if(layerList != null) {
			return layerList.size();
		} else {
			return 0;
		}
	}
	
	/**
	 * Checks whether a specific layer is included in the register
	 */
	public boolean contains(Layer layer)
	{
		if(layerList != null) {
			for(MediumLayerRelation mlr : layerList) {
				if(mlr.layer == layer) {
					return true;
				}
			}
		}
		
		return false;
	}
	
	/**
	 * Checks whether a specific medium is included in the register
	 */
	public boolean contains(Medium medium)
	{
		if(layerList != null) {
			for(MediumLayerRelation mlr : layerList) {
				if(mlr.medium == medium) {
					return true;
				}
			}
		}
		
		return false;
	}
	
	@Override
	protected void notifyFailure(Throwable failure, EventListener listener)
	{
		// ignore
	}
	
	private class MediumLayerRelation
	{
		public MediumLayerRelation(Medium medium, Layer layer)
		{
			this.medium = medium;
			this.layer = layer;
		}
		
		public Medium medium;
		public Layer layer;
	}
	
	private static final Layer[] emptyList = new Layer[0]; // lazy creation
	private LinkedList<MediumLayerRelation> layerList = null; // lazy creation
}
