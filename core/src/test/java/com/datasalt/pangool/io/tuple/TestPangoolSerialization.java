package com.datasalt.pangool.io.tuple;

import java.io.IOException;

import org.apache.hadoop.conf.Configuration;
import org.junit.Before;
import org.junit.Test;

import com.datasalt.pangolin.thrift.test.A;
import com.datasalt.pangool.BaseTest;
import com.datasalt.pangool.cogroup.CoGrouperConfig;
import com.datasalt.pangool.cogroup.CoGrouperException;
import com.datasalt.pangool.cogroup.ConfigBuilder;
import com.datasalt.pangool.cogroup.sorting.SortBy;
import com.datasalt.pangool.cogroup.sorting.Criteria.Order;
import com.datasalt.pangool.serialization.hadoop.HadoopSerialization;
import com.datasalt.pangool.serialization.thrift.ThriftSerialization;
import com.datasalt.pangool.serialization.tuples.PangoolDeserializer;
import com.datasalt.pangool.serialization.tuples.PangoolSerialization;
import com.datasalt.pangool.serialization.tuples.PangoolSerializer;


public class TestPangoolSerialization extends BaseTest{

	protected CoGrouperConfig pangoolConf;
	
	public static enum TestEnum {
		A,B,C
	};
	
	@Before
	public void prepare2() throws CoGrouperException{
		ConfigBuilder b = new ConfigBuilder();
		b.addSourceSchema(new Schema("schema1",Fields.parse("booleanField:boolean, intField:int, strField:string")));
		b.addSourceSchema(new Schema("schema2",Fields.parse("booleanField:boolean, intField:int, longField:long")));
		b.addSourceSchema(new Schema("schema3",Fields.parse("booleanField:boolean, intField:int, longField:long, vlongField:vlong,vintField:vint,strField:string")));
		b.addSourceSchema(new Schema("schema4",Fields.parse("booleanField:boolean, intField:int, longField:long, vlongField:vlong,vintField:vint,strField:string")));
		b.addSourceSchema(new Schema("schema5",Fields.parse("booleanField:boolean, intField:int, longField:long, vlongField:vlong,vintField:vint,strField:string, enumField:"+TestEnum.class.getName() + ",thriftField:" + A.class.getName())));
		
		b.setGroupByFields("booleanField","intField");
		b.setOrderBy(new SortBy().add("booleanField",Order.ASC).add("intField",Order.DESC).addSourceOrder(Order.DESC));
		b.setSecondaryOrderBy("schema1",new SortBy().add("strField",Order.DESC));
		b.setSecondaryOrderBy("schema2",new SortBy().add("longField",Order.ASC));
		pangoolConf = b.buildConf();
	}
	
	@Test
	public void testRandomTupleSerialization() throws IOException,  CoGrouperException {
		Configuration conf = new Configuration();
		ThriftSerialization.enableThriftSerialization(conf);
		
		HadoopSerialization hadoopSer = new HadoopSerialization(conf);
		Schema schema = pangoolConf.getSourceSchema("schema5"); //most complete
		PangoolSerialization serialization = new PangoolSerialization(hadoopSer,pangoolConf);
		PangoolSerializer serializer = (PangoolSerializer)serialization.getSerializer(null);
		PangoolDeserializer deser = (PangoolDeserializer)serialization.getDeserializer(null);
		Tuple tuple = new Tuple(schema);
		int NUM_ITERATIONS=100000;
		DatumWrapper<ITuple> wrapper = new DatumWrapper<ITuple>(tuple);
		for (int i=0 ; i < NUM_ITERATIONS; i++){
			fillTuple(true,wrapper.datum());
			assertSerializable(serializer, deser, wrapper, false);
		}
		
	}
	
}
