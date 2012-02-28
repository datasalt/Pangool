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
package com.datasalt.pangool.examples.secondarysort;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.apache.hadoop.fs.Path;
import org.apache.hadoop.io.DoubleWritable;
import org.apache.hadoop.io.LongWritable;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapreduce.lib.input.TextInputFormat;
import org.apache.hadoop.mapreduce.lib.output.TextOutputFormat;
import org.apache.hadoop.util.ToolRunner;

import com.datasalt.pangool.examples.BaseExampleJob;
import com.datasalt.pangool.io.ITuple;
import com.datasalt.pangool.io.Schema;
import com.datasalt.pangool.io.Schema.Field;
import com.datasalt.pangool.io.Schema.Field.Type;
import com.datasalt.pangool.io.Tuple;
import com.datasalt.pangool.tuplemr.Criteria.Order;
import com.datasalt.pangool.tuplemr.OrderBy;
import com.datasalt.pangool.tuplemr.TupleMRBuilder;
import com.datasalt.pangool.tuplemr.TupleMRException;
import com.datasalt.pangool.tuplemr.mapred.lib.input.HadoopInputFormat;
import com.datasalt.pangool.tuplemr.mapred.lib.output.HadoopOutputFormat;
import com.datasalt.pangool.tuplemr.mapred.tuplemr.TupleMapper;
import com.datasalt.pangool.tuplemr.mapred.tuplemr.TupleReducer;

/**
 * Code for solving a secondary sort problem with Pangool.
 * <p>
 * The secondary sort problem is: We have a file with sales registers: {departmentId nameId timestamp saleValue}. We
 * want to obtain meaningful statistics grouping by all people who perform sales (departmentId+nameId). We want to
 * obtain total sales value for certain periods of time, therefore we need to registers in each group to come sorted by
 * "timestamp".
 */
public class PangoolSecondarySort extends BaseExampleJob {

	@SuppressWarnings("serial")
	public static class IProcessor extends TupleMapper<LongWritable, Text> {
		private Tuple tuple;

		@Override
		public void map(LongWritable key, Text value, TupleMRContext context, Collector collector)
		    throws IOException, InterruptedException {
			if(tuple == null) {
				tuple = new Tuple(context.getTupleMRConfig().getIntermediateSchema(0));
			}

			String[] fields = value.toString().trim().split("\t");
			tuple.set("intField", Integer.parseInt(fields[0]));
			tuple.set("strField", fields[1]);
			tuple.set("longField", Long.parseLong(fields[2]));
			tuple.set("doubleField", Double.parseDouble(fields[3]));
			collector.write(tuple);
		}
	}

	@SuppressWarnings("serial")
	public static class Handler extends TupleReducer<Text, DoubleWritable> {

		private Text outputKey;
		private DoubleWritable outputValue;

		public void setup(TupleMRContext coGrouperContext, Collector collector)
    throws IOException, InterruptedException, TupleMRException {
			outputKey  = new Text();
			outputValue = new DoubleWritable();
		}
		
		@Override
		public void reduce(ITuple group, Iterable<ITuple> tuples, TupleMRContext context, Collector collector)
		    throws IOException, InterruptedException, TupleMRException {

			String groupStr = group.get("intField") + "\t" + group.get("strField");
			double accumPayments = 0;
			for(ITuple tuple : tuples) {
				accumPayments += (Double) tuple.get("doubleField");
			}
			outputValue.set(accumPayments);
			outputKey.set(groupStr);
			collector.write(outputKey, outputValue);
		}
	}

	public PangoolSecondarySort() {
		super("Needed arguments: [input] [output]");
	}
	
	@Override
	public int run(String[] args) throws Exception {
		if(args.length != 2) {
			failArguments("Invalid number of arguments");
			return -1;
		}
		String input = args[0];
		String output = args[1];

		deleteOutput(output);
		
		List<Field> fields = new ArrayList<Field>();
		fields.add(Field.create("intField",Type.INT));
		fields.add(Field.create("strField",Type.STRING));
		fields.add(Field.create("longField",Type.LONG));
		fields.add(Field.create("doubleField",Type.DOUBLE));
		Schema schema = new Schema("schema", fields);

		TupleMRBuilder grouper = new TupleMRBuilder(conf,"Pangool Secondary Sort");
		grouper.addIntermediateSchema(schema);
		grouper.setGroupByFields("intField", "strField");
		grouper.setOrderBy(new OrderBy().add("intField", Order.ASC).add("strField", Order.ASC)
		    .add("longField", Order.ASC));
		grouper.setTupleReducer(new Handler());
		grouper.addInput(new Path(input), new HadoopInputFormat(TextInputFormat.class), new IProcessor());
		grouper.setOutput(new Path(output), new HadoopOutputFormat(TextOutputFormat.class), Text.class, DoubleWritable.class);
		grouper.createJob().waitForCompletion(true);
	  return 1;
	}

	public static void main(String[] args) throws Exception {
		ToolRunner.run( new PangoolSecondarySort(), args);
	}
}
