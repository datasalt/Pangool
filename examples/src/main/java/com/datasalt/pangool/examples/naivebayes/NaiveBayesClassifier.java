package com.datasalt.pangool.examples.naivebayes;

import java.io.IOException;
import java.io.Serializable;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.StringTokenizer;

import org.apache.commons.collections.MapUtils;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.FileStatus;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.io.LongWritable;
import org.apache.hadoop.io.NullWritable;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapreduce.lib.input.TextInputFormat;
import org.apache.hadoop.mapreduce.lib.output.TextOutputFormat;
import org.apache.hadoop.util.ToolRunner;

import com.datasalt.pangool.examples.BaseExampleJob;
import com.datasalt.pangool.examples.naivebayes.NaiveBayesGenerate.Category;
import com.datasalt.pangool.io.ITuple;
import com.datasalt.pangool.tuplemr.MapOnlyJobBuilder;
import com.datasalt.pangool.tuplemr.mapred.MapOnlyMapper;
import com.datasalt.pangool.tuplemr.mapred.lib.input.HadoopInputFormat;
import com.datasalt.pangool.tuplemr.mapred.lib.input.TupleInputFormat.TupleInputReader;
import com.datasalt.pangool.tuplemr.mapred.lib.output.HadoopOutputFormat;

/**
 * This class is both a simple Naive Bayes Classifier with Add-1 Smoothing and a parallel map-only Hadoop Job
 * for performing parallel classification using this class. It uses the model generated by {@link NaiveBayesGenerate}.
 */
@SuppressWarnings("serial")
public class NaiveBayesClassifier extends BaseExampleJob implements Serializable {

	Map<Category, Integer> tokensPerCategory = new HashMap<Category, Integer>();
	Map<Category, Map<String, Integer>> wordCountPerCategory = new HashMap<Category, Map<String, Integer>>();
	int V;

	public NaiveBayesClassifier() {
		super("Arguments: [model-folder] [input-data] [output]");
	}

	// Read the Naive Bayes Model from HDFS
	public void init(Configuration conf, Path generatedModel) throws IOException, InterruptedException {
		FileSystem fileSystem = FileSystem.get(conf);
		for(Category category : Category.values()) {
			wordCountPerCategory.put(category, new HashMap<String, Integer>()); // init token count
		}
		// Use a HashSet to calculate the total vocabulary size
		Set<String> vocabulary = new HashSet<String>();
		// Read tuples from generate job
		for(FileStatus fileStatus : fileSystem.globStatus(generatedModel)) {
			TupleInputReader reader = new TupleInputReader(conf);
			reader.initialize(fileStatus.getPath(), conf);
			while(reader.nextKeyValueNoSync()) {
				// Read Tuple
				ITuple tuple = reader.getCurrentKey();
				Integer count = (Integer) tuple.get("count");
				Category category = (Category) tuple.get("category");
				String word = tuple.get("word").toString();
				vocabulary.add(word);
				tokensPerCategory.put(category, MapUtils.getInteger(tokensPerCategory, category, 0) + count);
				wordCountPerCategory.get(category).put(word, count);
			}
		}
		V = vocabulary.size();
	}

	/**
	 * Naive Bayes Text Classification with Add-1 Smoothing
	 * @param text Input text
	 * @return the best {@link Category}
	 */
	public Category classify(String text) {
		StringTokenizer itr = new StringTokenizer(text);
		Map<Category, Double> scorePerCategory = new HashMap<Category, Double>();
		double bestScore = Double.NEGATIVE_INFINITY;
		Category bestCategory = null;
		while(itr.hasMoreTokens()) {
			String token = NaiveBayesGenerate.normalizeWord(itr.nextToken());
			for(Category category : Category.values()) {
				int count = MapUtils.getInteger(wordCountPerCategory.get(category), token, 0) + 1;
				double wordScore  = Math.log(count / (double) (tokensPerCategory.get(category) + V));
				double totalScore = MapUtils.getDouble(scorePerCategory, category, 0.) + wordScore;
				if(totalScore > bestScore) {
					bestScore = totalScore;
					bestCategory = category;
				}
				scorePerCategory.put(category, totalScore);
			}
		}
		return bestCategory;
	}

  @Override
	public int run(String[] args) throws Exception {
  	if(args.length != 3) {
			failArguments("Wrong number of arguments");
			return -1;
		}
		String modelFolder = args[0];
		String input = args[1];
		String output = args[2];
		delete(output);
		
		init(conf, new Path(modelFolder));
		
		MapOnlyJobBuilder job = new MapOnlyJobBuilder(conf);
		job.setMapper(new MapOnlyMapper<LongWritable, Text, Text, NullWritable>() {
			protected void map(LongWritable key, Text value, Context context) throws IOException ,InterruptedException {
				value.set(value.toString() + "\t" + classify(value.toString()));
				context.write(value, NullWritable.get());
			}
		});
		job.setOutput(new Path(output), new HadoopOutputFormat(TextOutputFormat.class), Text.class, NullWritable.class);
		job.addInput(new Path(input), new HadoopInputFormat(TextInputFormat.class));
		job.createJob().waitForCompletion(true);
		
		return 1;
	}

	public static void main(String[] args) throws Exception {
		ToolRunner.run(new NaiveBayesClassifier(), args);
	}
}
