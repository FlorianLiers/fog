/*******************************************************************************
 * Forwarding on Gates Simulator/Emulator - Lower Layer View
 * Copyright (c) 2013, Integrated Communication Systems Group, TU Ilmenau.
 * 
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html.
 ******************************************************************************/
package de.tuilmenau.ics.fog.lowerlayer.view;

import java.util.LinkedList;

import net.rapi.Binding;
import net.rapi.NetworkException;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.Platform;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorSite;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.part.EditorPart;

import de.tuilmenau.ics.fog.Config;
import de.tuilmenau.ics.fog.lowerlayer.BusMedium;
import de.tuilmenau.ics.fog.lowerlayer.Connection;
import de.tuilmenau.ics.fog.ui.Logging;
import de.tuilmenau.ics.fog.eclipse.ui.EditorRowComposite;
import de.tuilmenau.ics.fog.eclipse.ui.editors.EditorInput;
import de.tuilmenau.ics.fog.eclipse.ui.editors.SelectionProvider;


/**
 * Editor for showing and editing the internals of a bus.
 */
public class BusMediumEditor extends EditorPart
{
	public static final String ID = "de.tuilmenau.ics.fog.bus.view.BusMediumEditor";
	

	public BusMediumEditor()
	{
	}
	
	@Override
	public void createPartControl(Composite parent)
	{
		mDisplay = Display.getCurrent();
		mSelectionCache = new SelectionProvider(mDisplay);
		getSite().setSelectionProvider(mSelectionCache);

		EditorRowComposite tGrp = new EditorRowComposite(parent, SWT.SHADOW_NONE);
		
		//
		// Packet loss probability
		//
		int from = 1;
		int to = 40;
		boolean enabled = true;
		
		if(Config.DEVELOPER_VERSION) {
			from = 0;
			to = 100;
		}
		if(!Config.DEVELOPER_VERSION) {
			if (mBus.getPacketLossProbability() == 0) {
				enabled = false;
			}
		}
		
		tGrp.createRow("Loss probability:", Integer.toString(mBus.getPacketLossProbability()), "%", from, to, mBus.getPacketLossProbability(), enabled, tGrp.new SliderChangeListener() {
			@Override
			public void handleEvent(Event event)
			{
				super.handleEvent(event);
				
				mBus.setPacketLossProbability(mSlider.getSelection());
			}
		});

		//
		// Bit error probability
		//
		tGrp.createRow("Bit error probability:", Integer.toString(mBus.getBitErrorProbability()), "%", from, to, mBus.getBitErrorProbability(), enabled, tGrp.new SliderChangeListener() {
			@Override
			public void handleEvent(Event event)
			{
				super.handleEvent(event);

				mBus.setBitErrorProbability(mSlider.getSelection());
			}
		});

		//
		// Delay
		//
		int currentDelay = (int) mBus.getDelayMSec();
		
		tGrp.createRow("Link delay:", Integer.toString(currentDelay), "msec", 0, Math.max(1000, currentDelay), currentDelay, true, tGrp.new SliderChangeListener() {
			@Override
			public void handleEvent(Event event)
			{
				super.handleEvent(event);

				mBus.setDelayMSec(mSlider.getSelection());
			}
		});
		
		
		//
		// Bindings and connections
		//
		LinkedList<Binding> bindings = mBus.getLayer().getBindings();
		LinkedList<Connection> connections = mBus.getLayer().getConnections();
		
		tGrp.createRow("Bindings / Connections", bindings.size() +" / " +connections.size());
		
		for(Binding binding : bindings) {
			tGrp.createRow(binding.toString(), binding.getName().toString());
			tGrp.createRow("", "requ=" +binding.getRequirements());
		}
		
		for(Connection conn : connections) {
			tGrp.createRow(conn.toString(), "establ=" +conn.isEstablished() +", data=" +conn.hasPacket(), "Terminate", new TerminateListener(conn));
			tGrp.createRow("", "requ=" +conn.getRequirements());
		}
		
	}
	
	private class TerminateListener implements Listener
	{
		public TerminateListener(Connection conn)
		{
			this.conn = conn;
		}
		
		@Override
		public void handleEvent(Event event)
		{
			NetworkException exc = new NetworkException(this, "Terminated by user.");
			conn.getPeer1().setError(exc);
			conn.getPeer2().setError(exc);
		}
		
		private Connection conn;
	}

	@Override
	public void init(IEditorSite site, IEditorInput input) throws PartInitException
	{
		setSite(site);
		setInput(input);
		
		// get selected object to show in editor
		Object inputObject;
		if(input instanceof EditorInput) {
			inputObject = ((EditorInput) input).getObj();
		} else {
			inputObject = null;
		}
		Logging.log(this, "init editor for " +inputObject + " (class=" +inputObject.getClass() +")");
		
		if(inputObject != null) {
			// update title of editor
			setTitle(inputObject.toString());

			if(inputObject instanceof BusMedium) {
				mBus = (BusMedium) inputObject;
			}
			else {
				throw new PartInitException("Invalid input object " +inputObject +". Bus expected.");
			}
		} else {
			throw new PartInitException("No input for editor.");
		}
	}
	
	@Override
	public void doSave(IProgressMonitor arg0)
	{
	}

	@Override
	public void doSaveAs()
	{
	}

	@Override
	public boolean isDirty()
	{
		return false;
	}

	@Override
	public boolean isSaveAsAllowed()
	{
		return false;
	}

	@Override
	public void setFocus()
	{
	}


	@Override
	public Object getAdapter(Class required)
	{
		if(this.getClass().equals(required)) return this;
		
		Object res = super.getAdapter(required);
		
		if(res == null) {
			res = Platform.getAdapterManager().getAdapter(this, required);
			
			if(res == null)	res = Platform.getAdapterManager().getAdapter(mBus, required);
		}
		
		return res;
	}

		
	private BusMedium mBus = null;
	private SelectionProvider mSelectionCache = null;
	private Display mDisplay = null;
}

