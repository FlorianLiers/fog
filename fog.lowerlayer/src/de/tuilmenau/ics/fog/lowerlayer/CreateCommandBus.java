package de.tuilmenau.ics.fog.lowerlayer;

import net.rapi.Description;
import net.rapi.properties.DatarateProperty;
import net.rapi.properties.DelayProperty;
import net.rapi.properties.LossRateProperty;
import net.rapi.properties.MinMaxProperty.Limit;
import de.tuilmenau.ics.fog.commands.CreateCommand;
import de.tuilmenau.ics.fog.topology.AutonomousSystem;



/**
 * Command extension for the "create" command of Frogger.
 * Enables the creation of Bus via command:
 * "create bus <name of bus>" 
 */
public class CreateCommandBus implements CreateCommand
{
	@Override
	public boolean create(AutonomousSystem pAS, String[] tParts)
	{
		boolean tRes = false;
		
		if(tParts[1].equals("bus")) {
			Description tDescr = null;
			
			// at least one QoS parameter present?
			if(tParts.length > 3) {
				//
				// <max bandwidth> <min delay> <loss probability>
				//
				tDescr = new Description();
				tDescr.set(new DatarateProperty(Integer.parseInt(tParts[3]), Limit.MAX));
				
				if(tParts.length > 4) {
					tDescr.set(new DelayProperty(Integer.parseInt(tParts[4]), Limit.MIN));
					
					if(tParts.length > 5) {
						tDescr.set(new LossRateProperty(Integer.parseInt(tParts[5]), Limit.MAX));
					}
				}
			}
			
			tRes = pAS.addBus(new BusMedium(pAS, tParts[2], tDescr));
		}
		
		return tRes;
	}

}
