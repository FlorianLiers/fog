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

/**
 * This class provides a facade for the network stack(s) on a node.
 * Its main purpose is to provide access to the available layers of
 * a node.
 */
public interface LayerContainer
{
	/**
	 * Returns a layer entity residing in this container.
	 * If multiple layers with the same class are available,
	 * the method returns one of them.
	 * 
	 * @param layerClass Filter; {@code null} for default layer
	 * @return Reference to layer or {@code null} is no layer for the filter exists
	 */
	public Layer getLayer(Class<?> layerClass);
	
	/**
	 * @param layerClass Filter; {@code null} for all layer entities
	 * @return List of layers ({@code != null})
	 */
	public Layer[] getLayers(Class<?> layerClass);	
}
