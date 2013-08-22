package de.tuilmenau.ics.fog.lowerlayer.view;

import java.util.LinkedList;

import org.eclipse.ui.views.properties.IPropertyDescriptor;
import org.eclipse.ui.views.properties.TextPropertyDescriptor;

import de.tuilmenau.ics.fog.lowerlayer.BusMedium;
import de.tuilmenau.ics.fog.eclipse.properties.AnnotationPropertySource;


public class BusMediumPropertySource extends AnnotationPropertySource
{
	public BusMediumPropertySource(BusMedium bus)
	{
		this.bus = bus;
	}

	@Override
	protected void extendPropertyList(LinkedList<IPropertyDescriptor> list)
	{
		list.addLast(new TextPropertyDescriptor(PROPERTY_PACKETS_NUMBER, "Number Packets"));
		
		extendPropertyListBasedOnAnnotations(list, bus);
	}
	
	@Override
	public Object getPropertyValue(Object name)
	{
		if(PROPERTY_PACKETS_NUMBER.equals(name)) {
			return bus.getNumberPackets();
		} else {
			return getPropertyValueBasedOnAnnotation(name, bus);
		}
	}

	private BusMedium bus;
	
	private static final String PROPERTY_PACKETS_NUMBER = "Bus.Packets.Number";
}

