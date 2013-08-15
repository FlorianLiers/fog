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

public class PriorityProperty extends NonFunctionalRequirementsProperty
{
	public PriorityProperty(int priority)
	{
		this.priority = priority;
	}
	
	public int getPriority()
	{
		return priority;
	}
	
	public Property deriveRequirements(Property property) throws PropertyException
	{
		return this;
	}
	
	public Property removeCapabilities(Property property) throws PropertyException
	{
		return this;
	}
	
	public boolean isBE()
	{
		return priority == 0;
	}
	
	@Override
	public String getPropertyValues()
	{
		return Integer.toString(priority);
	}
	
	private int priority;
}
