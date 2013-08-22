package de.tuilmenau.ics.fog.lowerlayer;

import net.rapi.Description;
import net.rapi.Identity;
import net.rapi.Name;
import net.rapi.impl.base.BaseBinding;
import de.tuilmenau.ics.fog.util.Logger;


/**
 * This class implements the Binding interface for Bus.
 */
public class BindingImpl extends BaseBinding
{

	public BindingImpl(Logger logger, Name name, Description requirements, Identity identity)
	{
		super(name, requirements, identity);
		
		this.logger = logger;
	}

	public BindingImpl(Logger logger, Name name, Exception error)
	{
		super(name, error);
		
		this.logger = logger;
	}

	@Override
	public synchronized void close()
	{
		super.close();
		
		active = false;
	}
	
	public boolean isClosed()
	{
		return !active;
	}
	
	@Override
	public boolean isActive()
	{
		return active;
	}

	@Override
	protected void notifyFailure(Throwable failure, EventListener listener)
	{
		logger.warn(this, "Ignoring event listerner failure from " +listener, failure);
	}
	
	private Logger logger;
	private boolean active = true; /* Used to tell if close() has been called */
}
