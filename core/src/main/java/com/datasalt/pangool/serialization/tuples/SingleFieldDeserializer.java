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
package com.datasalt.pangool.serialization.tuples;

import static org.apache.hadoop.io.WritableComparator.readDouble;
import static org.apache.hadoop.io.WritableComparator.readFloat;
import static org.apache.hadoop.io.WritableComparator.readInt;
import static org.apache.hadoop.io.WritableComparator.readLong;
import static org.apache.hadoop.io.WritableComparator.readVInt;
import static org.apache.hadoop.io.WritableComparator.readVLong;

import java.io.IOException;
import java.util.Map;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.io.VIntWritable;
import org.apache.hadoop.io.VLongWritable;
import org.apache.hadoop.io.WritableUtils;
import org.apache.hadoop.util.ReflectionUtils;

import com.datasalt.pangool.cogroup.TupleMRConfig;
import com.datasalt.pangool.io.Utf8;
import com.datasalt.pangool.io.tuple.Schema.Type;
import com.datasalt.pangool.serialization.hadoop.HadoopSerialization;

/**
 * A class for deserializing fields in Pangool format from a byte array.
 * Thead unsafe. It could cache the instance internally and reuse it in 
 * deserialize calls.
 */
public class SingleFieldDeserializer {

	private final Configuration conf;
	private final HadoopSerialization ser;
	private final Map<Class<?>, Enum<?>[]> cachedEnums;
	private final Class<?> type;
	private Object instance;
	
	public SingleFieldDeserializer(Configuration conf, TupleMRConfig grouperConfig, Class<?> type) throws IOException {
		this.conf = conf;
		this.ser = new HadoopSerialization(conf);
		this.cachedEnums = PangoolSerialization.getEnums(grouperConfig);
		this.type = type;
		Class<?> instanceType = instanceType(type);
		if (instanceType != null) {
			this.instance = ReflectionUtils.newInstance(instanceType, conf);
		}
	} 
	
	static Class<?> instanceType(Class<?> originType) {
		Type iType = Type.fromClass(originType);
		if (iType == Type.UTF8) {
			return Utf8.class;
		} else if (iType == Type.OBJECT) {
			return originType;
		} else {
			return null;
		}
	} 
	
	/**
	 * Deserialize an individual field from a byte array position that is encoded with the 
	 * {@link PangoolSerialization}.
	 * <br>
	 * @param bytes The byte array.
	 * @param offset The place to start reading.
	 */
	public Object deserialize(byte[] bytes, int offset) throws IOException {
		return deserialize(instance, bytes, offset, type, ser, conf, cachedEnums);
	}
	
	/**
	 * Deserialize an individual field from a byte array position that is encoded with the 
	 * {@link PangoolSerialization}.
	 * <br>
	 * Objects of {@link Type#OBJECT} can return null.
	 * 
	 * @param instance An instance to be reused in the case of objects following standard 
	 * 				Hadoop serialization or Utf8. If null, always return a new object. Usually
	 *        you should call this method first with a new instance, and then reuse this
	 *        instance in sucessive calls.  
	 * @param bytes The byte array.
	 * @param offset The place to start reading.
	 * @param type The type of the object to read.
	 * @param ser A Hadoop serialization service for the cases of objects serialized 
	 *  			with this format
	 * @param conf A Hadoop configuration
	 * @param cachedEnums A map with the cached enumerations, for fast lookup. 
	 * @return A deserialized instance. Null possible if the type is
	 *         {@link Type#OBJECT} 
	 * 				    
	 */
	public static Object deserialize(Object instance, byte[] bytes, int offset, Class<?> type, 
  		HadoopSerialization ser, Configuration conf, Map<Class<?>, Enum<?>[]> cachedEnums) throws IOException {
  	if(type == Integer.class) {
  		// Integer
  		return readVInt(bytes, offset);
  	} else if(type == Utf8.class) {  		
  		// String
  		int length = readVInt(bytes, offset);
  		offset += WritableUtils.decodeVIntSize(bytes[offset]);
  		((Utf8) instance).set(bytes, offset, length);
  		return instance;
  	} else if(type == Long.class) {
  		// Long
  		return readVLong(bytes, offset);			
  	} else if(type.isEnum()) {
  		// Integer
  		int value1 = readVInt(bytes, offset);
  		return cachedEnums.get(type)[value1];			
  	} else if(type == Float.class) {
  		// Float
  		return readFloat(bytes, offset);
  	} else if(type == Double.class) {
  		// Double
  		return readDouble(bytes, offset);
  	} else if(type == Boolean.class) {
  		// Boolean
  		return bytes[offset] > 0;
  	} else {
  		// Custom objects uses Hadoop Serialization, but has a header with the length.
  		int length = readVInt(bytes, offset);
  		if (length < 0) {
  			return null;
  		}
  		offset += WritableUtils.decodeVIntSize(bytes[offset]);
  		return ser.deser(instance, bytes, offset, length);
  	} 
  }	
}
