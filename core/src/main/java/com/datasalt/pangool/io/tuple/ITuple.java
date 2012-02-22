/**
 * Copyright [2012] [Datasalt Systems S.L.]
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.datasalt.pangool.io.tuple;


/**
 * This is the common interface implemented by tuples.
 * A Tuple is basically an ordered list of objects whose types are defined by a {@link Schema}.
 * @see http://en.wikipedia.org/wiki/Tuple 
 */
public interface ITuple{

	public Schema getSchema();
	
	public void clear();
	
	public Object get(int pos);
	public void set(int pos, Object object);
	
	public void set(String field, Object object);
	public Object get(String field);
	
}
