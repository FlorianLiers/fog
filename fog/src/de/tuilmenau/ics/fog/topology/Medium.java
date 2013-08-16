package de.tuilmenau.ics.fog.topology;

import net.rapi.Layer;


public interface Medium extends Breakable
{
	public String getName();
	
	public Layer attach(Node pNode);
	public Layer detach(Node pNode);
	
	public void deleted();
	
	/**
	 * Returns a proxy for the medium, if it is the original object. The
	 * proxy can be used to make the object available on other VMs. 
	 * 
	 * @return Proxy or null, if object is already a proxy
	 */
	public RemoteMedium getProxy();
}
