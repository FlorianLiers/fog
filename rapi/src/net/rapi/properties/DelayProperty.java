/******************************************************************************
 * Recursive API
 * Copyright 2013 Integrated Communication Systems Group, TU Ilmenau.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *****************************************************************************/
package net.rapi.properties;


public class DelayProperty extends MinMaxProperty
{
	public static int DefaultMaxValueMSec = 100;
	
	
	public DelayProperty(int minDelayMSec, int maxDelayMSec, double variance)
	{
		super(minDelayMSec, maxDelayMSec, variance);
	}
	
	public DelayProperty(int delayMilliSec, Limit minValue)
	{
		super(delayMilliSec, minValue);
	}
	
	public DelayProperty()
	{
		super(DefaultMaxValueMSec, Limit.MAX);
	}

	@Override
	public Property create(int min, int max, double variance)
	{
		return new DelayProperty(min, max, variance);
	}
	
	@Override
	public Property deriveRequirements(Property property) throws PropertyException
	{	
		if(property instanceof DelayProperty) {
			// do NoS introduces delay?
			if(getMin() != UNDEFINED) {
				int maxDelay = ((DelayProperty) property).getMax();
				
				if(maxDelay != UNDEFINED) {
					// min delay introduced and max delay required
					if(getMin() <= maxDelay) {
						return create(getMin(), getMin(), getVariance());
					} else {
						throw new PropertyException(this, "Min of " +this +" exceeds max value " +property);
					}
				} else {
					// delay introduced and not limited by requirements
					return create(getMin(), UNDEFINED, 0);
				}
			} else {
				// no delay introduced by NoS
				return create(0, 0, 0);
			}
		} else {
			throw new PropertyException(this, "Parameter " +property +" is not a " +DelayProperty.class + " object.");
		}
	}
	
	@Override
	public Property removeCapabilities(Property property) throws PropertyException
	{	
		if(property instanceof DelayProperty) {
			// delay restricted by requirements?
			if(getMax() != UNDEFINED) {
				int maxDelay = ((DelayProperty) property).getMax();
				
				if(maxDelay != UNDEFINED) {
					if(getMax() >= maxDelay) {
						return create(UNDEFINED, getMax() -maxDelay, getVariance() +((DelayProperty) property).getVariance());
					} else {
						throw new PropertyException(this, "Max of " +property +" exceeds max value " +this);
					}
				} else {
					// is there a minimum value?
					if(((DelayProperty) property).getMin() != UNDEFINED) {
						throw new PropertyException(this, "Max restricted to " +this +" but " +property +" does not restrict it.");
					} else {
						// delay restricted and not introduced -> no changes
						return this;
					}
				}
			} else {
				// no delay limit -> no changes
				return this;
			}
		} else {
			throw new PropertyException(this, "Parameter " +property +" is not a " +DelayProperty.class + " object.");
		}
	}
	
	@Override
	public boolean isBE()
	{
		return getMax() == UNDEFINED;
	}
	
	@Override
	public String getUnit()
	{
		return "msec";
	}
	
	@Override
	public Property clone()
	{
		return new DelayProperty(getMin(), getMax(), getVariance());
	}
}
