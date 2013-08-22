package de.tuilmenau.ics.fog.lowerlayer.view;

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

import de.tuilmenau.ics.fog.lowerlayer.BusEntity;
import de.tuilmenau.ics.fog.ui.Logging;
import de.tuilmenau.ics.fog.eclipse.ui.EditorRowComposite;
import de.tuilmenau.ics.fog.eclipse.ui.editors.EditorInput;
import de.tuilmenau.ics.fog.eclipse.ui.editors.SelectionProvider;


/**
 * Editor for showing and editing the internals of a bus entity.
 */
public class BusEntityEditor extends EditorPart
{
	public static final String ID = "de.tuilmenau.ics.fog.bus.view.BusEntityEditor";
	

	public BusEntityEditor()
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
				tGrp.createRow("Neighbors:", "n.a.");
	
				int i = 0;
				for(Name neighb : neighbors) {
					i++;
					tGrp.createRow("Neighbor " +i, neighb.toString());
				}
			}
		}
		catch(NetworkException exc) {
			tGrp.createRow("Error:", exc.getMessage());
		}
		
		//
		// Bindings and connections
		//
		LinkedList<Binding> bindings = mBus.getBindings();
		
		if(bindings != null) {
			tGrp.createRow("Bindings:", Integer.toString(bindings.size()));
			
			for(Binding binding : bindings) {
				tGrp.createRow(binding.toString(), binding.getName().toString());
			}
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
		Logging.log(this, "init editor for " +inputObject + " (class=" +inputObject.getClass() +")");
		
		if(inputObject != null) {
			// update title of editor
			setTitle(inputObject.toString());

			if(inputObject instanceof BusEntity) {
				mBus = (BusEntity) inputObject;
				
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

		
	private BusEntity mBus = null;
	private SelectionProvider mSelectionCache = null;
	private Display mDisplay = null;
}

