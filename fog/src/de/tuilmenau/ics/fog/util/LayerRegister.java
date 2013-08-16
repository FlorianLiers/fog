package de.tuilmenau.ics.fog.util;

import java.util.Iterator;
import java.util.LinkedList;

import net.rapi.Layer;
import net.rapi.LayerContainer;
import net.rapi.events.LayerSetEvent;
import net.rapi.impl.base.BaseEventSource;



public class LayerRegister extends BaseEventSource implements LayerContainer
{
	public LayerRegister()
	{
	}
	
	public void register(Layer layer)
	{
		if(layer != null) {
			if(layerList == null) layerList = new LinkedList<Layer>();
			else {
				// avoid duplicated entries
				if(contains(layer)) {
					return;
				}
			}
			
			layerList.addLast(layer);
			
			notifyObservers(new LayerSetEvent(this, layer, true));
		}
	}
	
	@Override
	public Layer getLayer(Class<?> layerClass)
	{
		if(layerList != null) {
			if(layerClass != null) {
				for(Layer layer : layerList) {
					if(layer.getClass().equals(layerClass)) {
						return layer;
					}
				}
			} else {
				// return default layer
				return layerList.getFirst();
			}
		}

		return null;
	}
	
	@Override
	public Layer[] getLayers(Class<?> layerClass)
	{
		if(layerList != null) {
			LinkedList<Layer> found = null;
			
			// find suitable layers
			for(Layer layer : layerList) {
				boolean match = true;
				if(layerClass != null) {
					match = layer.getClass().isAssignableFrom(layerClass); 
				}
				
				if(match) {
					if(found == null) found = new LinkedList<Layer>();
					
					found.add(layer);
				}
			}
			
			return convertList(found);
		}
		
		return emptyList;
	}
	
	/**
	 * Copy list to array 
	 * 
	 * @return Array (!= null)
	 */
	private Layer[] convertList(LinkedList<Layer> list)
	{
		Layer[] res = emptyList;
		
		if(list != null) {
			if(list.size() > 0) {
				res = new Layer[list.size()];
				for(int i = 0; i < list.size(); i++) {
					res[i] = list.get(i);
				}
			}
		}
		
		return res;
	}
	
	public boolean unregister(Layer layer)
	{
		if(layerList != null) {
			Iterator<Layer> iter = layerList.iterator();
		
			while(iter.hasNext()) {
				Layer layerIn = iter.next();
				
				if(layerIn == layer) {
					iter.remove();
					
					notifyObservers(new LayerSetEvent(this, layerIn, false));
					return true;
				}
			}
		}
		
		return false;
	}
	
	@Override
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
			for(Layer layerIn : layerList) {
				if(layerIn == layer) {
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
	
	private static final Layer[] emptyList = new Layer[0]; // lazy creation
	private LinkedList<Layer> layerList = null; // lazy creation
}
