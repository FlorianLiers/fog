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

public abstract class NonFunctionalRequirementsProperty extends AbstractProperty
{
	/**
	 * This object represents the capabilities. The parameter property represents the requirement.
	 * The result is the minimal requirement for a connection through this link suitable by the capabilities and satisfying the requirements.
	 * 
	 * @throws PropertyException On error
	 */
	public abstract Property deriveRequirements(Property property) throws PropertyException;
	
	/**
	 * This objects represents the requirements. The parameter property represents the connection capabilities/requirement.
	 * The result indicates the remaining requirements for the rest of the connection.
	 * 
	 * @throws PropertyException On error
	 */
	public abstract Property removeCapabilities(Property property) throws PropertyException;
	
	public abstract boolean isBE();
}
