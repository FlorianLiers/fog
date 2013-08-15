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
package net.rapi;

import java.io.Serializable;


/**
 * General representation of a name.
 * Since the internals of a name are not 'visible' to the one using the name,
 * there are no methods for modifying it. Comparing and displaying MUST be done
 * with the methods provided by the Object class.
 */
public interface Name extends Serializable
{
	/**
	 * @return Namespace of the name (!= null)
	 */
	public Namespace getNamespace();
	
	/**
	 * @return Size of name in serialized version
	 */
	public int getSerialisedSize();
}
