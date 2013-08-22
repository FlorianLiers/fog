package de.tuilmenau.ics.fog.lowerlayer.view;

import org.eclipse.core.runtime.IAdapterFactory;
import org.eclipse.ui.views.properties.IPropertySource;

import de.tuilmenau.ics.fog.eclipse.properties.LayerPropertySource;
import de.tuilmenau.ics.fog.lowerlayer.BusEntity;
import de.tuilmenau.ics.fog.lowerlayer.BusMedium;


/**
 * Class is responsable for creating adapters for model elements.
 * 
 * Class is used only for convert operations registered via the extension
 * point "org.eclipse.core.runtime.adapters".
 */
public class PropertyAdapterFactory implements IAdapterFactory
{
	@SuppressWarnings("rawtypes")
	@Override
	public Object getAdapter(Object adaptableObject, Class adapterType)
	{
		if(adapterType == IPropertySource.class) {
			if(adaptableObject instanceof BusEntity) {
				return new LayerPropertySource((BusEntity) adaptableObject);
			}
			if(adaptableObject instanceof BusMedium) {
				return new BusMediumPropertySource((BusMedium) adaptableObject);
			}
		}
		
		return null;
	}

	@Override
	public Class<?>[] getAdapterList()
	{
		return new Class[] { IPropertySource.class };
	}
}

