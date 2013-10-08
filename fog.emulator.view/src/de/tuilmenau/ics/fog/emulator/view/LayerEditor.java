package de.tuilmenau.ics.fog.emulator.view;

import java.util.LinkedList;

import net.rapi.Binding;
import net.rapi.Name;
import net.rapi.NeighborName;
import net.rapi.NetworkException;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.Platform;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorSite;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.part.EditorPart;

import de.tuilmenau.ics.fog.eclipse.ui.EditorRowComposite;
import de.tuilmenau.ics.fog.eclipse.ui.editors.EditorInput;
import de.tuilmenau.ics.fog.eclipse.ui.editors.SelectionProvider;
import de.tuilmenau.ics.fog.emulator.EmulatorLayer;
import de.tuilmenau.ics.fog.emulator.Port;


/**
 * Editor for showing and editing the internals of a bus entity.
 */
public class LayerEditor extends EditorPart
{
	public static final String ID = "de.tuilmenau.ics.frogger.bus.view.BusEntityEditor";
	

	public LayerEditor()
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
		// Neighbors
		//
		try {
			Iterable<NeighborName> neighbors = mBus.getNeighbors(null);
			if(neighbors != null) {
				int i = 0;
				for(Name neighb : neighbors) {
					i++;
					tGrp.createRow("Neighbor " +i, neighb.toString());
				}
				
				tGrp.createRow("Neighbors:", Integer.toString(i));
			}
		}
		catch(NetworkException exc) {
			tGrp.createRow("Error:", exc.getMessage());
		}
		
		//
		// Bindings and connections
		//
		Iterable<Port> ports = mBus.getPorts();
		
		if(ports != null) {
			int i = 0;
			for(Port port : ports) {
				i++;
				tGrp.createRow("Port " +port.getPortNumber(), port.toString());
			}
			
			tGrp.createRow("Ports:", Integer.toString(i));
		}
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
		
		if(inputObject != null) {
			// update title of editor
			setTitle(inputObject.toString());

			if(inputObject instanceof EmulatorLayer) {
				mBus = (EmulatorLayer) inputObject;
				
				// TODO impl
			}
			else {
				throw new PartInitException("Invalid input object " +inputObject +". BusEntity expected.");
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

		
	private EmulatorLayer mBus = null;
	private SelectionProvider mSelectionCache = null;
	private Display mDisplay = null;
}

