/*******************************************************************************
 * Forwarding on Gates Simulator/Emulator - Eclipse
 * Copyright (c) 2012, Integrated Communication Systems Group, TU Ilmenau.
 * 
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html.
 ******************************************************************************/
package de.tuilmenau.ics.fog.eclipse.ui.editors;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Frame;
import java.awt.Graphics;
import java.awt.Panel;

import javax.swing.JScrollPane;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.swt.SWT;
import org.eclipse.swt.awt.SWT_AWT;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.part.EditorPart;


public abstract class EditorAWT extends EditorPart
{
	public EditorAWT()
	{
		mTitle = null;
		mRootComp = null;
		mView = null;
	}
	
	@Override
	public String getTitle()
	{
		if(mTitle != null)
			return mTitle;
		else
			return super.getTitle();
	}

	@Override
	public void doSave(IProgressMonitor monitor)
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
	public void createPartControl(Composite parent)
	{
		mDisplay = Display.getCurrent();
		mRootComp = new Composite(parent, SWT.NO_BACKGROUND | SWT.EMBEDDED);
		
		try {
			System.setProperty("sun.awt.noerasebackground", "true");
		}
		catch(NoSuchMethodError tExc) {
			throw new RuntimeException("Can not init " +this, tExc);
		}
		
		Frame tFrame = SWT_AWT.new_Frame(mRootComp);
		
		try {
			Panel tRootPanel = new Panel(new BorderLayout()) {
				public void update(Graphics g) {
					// do not erase background
					paint(g);
				}
			};
			
			JScrollPane tScroll = new JScrollPane(mView);
			tRootPanel.add(tScroll);
//			tRootPanel.add(mGraphView);
			
			tFrame.add(tRootPanel);
		
/*			JRootPane tRoot = new JRootPane();
			tRootPanel.add(tRoot, BorderLayout.CENTER);

			tRoot.add(tViewer.getComponent());*/
			
			//tFrame.add(tViewer.getComponent());
		}
		catch(Exception tExc) {
			tExc.printStackTrace();
		}
	}

	@Override
	public void setFocus()
	{
		if(mRootComp != null) {
			mRootComp.setFocus();
		}
	}

	/**
	 * Updates title of editor
	 */
	protected void setTitle(String newTitle)
	{
		mTitle = newTitle;
		firePropertyChange(PROP_TITLE);
	}
	
	/**
	 * Had to be called during init
	 */
	protected void setView(Component newView)
	{
		mView = newView;
	}
	
	protected Display getDisplay()
	{
		return mDisplay;
	}

	private String    mTitle;
	private Composite mRootComp;
	private Component mView;
	private Display   mDisplay;
}

