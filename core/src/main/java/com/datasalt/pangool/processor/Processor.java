package com.datasalt.pangool.processor;

import java.io.IOException;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.mapreduce.InputFormat;
import org.apache.hadoop.mapreduce.Job;
import org.apache.hadoop.mapreduce.OutputFormat;
import org.apache.hadoop.mapreduce.lib.output.FileOutputFormat;

import com.datasalt.pangool.cogroup.CoGrouperException;
import com.datasalt.pangool.mapreduce.lib.input.PangoolMultipleInputs;

/**
 * The Processor is a simple Pangool primitive that executes map-only Jobs. You can implement {@link ProcessorHandler} for using it.
 * See {@link Grep} for an example. You can instantiate your handler with Serializable state.
 * 
 */
public class Processor {

	private Configuration conf;

	private Class<?> jarByClass;
	private Class<?> outputKeyClass;
	private Class<?> outputValueClass;
	private Class<? extends OutputFormat> outputFormat;
	private ProcessorHandler processorHandler;
	
	private static final class Input {

		Path path;
		Class<? extends InputFormat> inputFormat;

		Input(Path path, Class<? extends InputFormat> inputFormat) {
			this.path = path;
			this.inputFormat = inputFormat;
		}
	}

	private Path outputPath;
	private List<Input> multiInputs = new ArrayList<Input>();

	public Processor setJarByClass(Class<?> jarByClass) {
		this.jarByClass = jarByClass;
		return this;
	}

	public Processor addInput(Path path, Class<? extends InputFormat> inputFormat) {
		this.multiInputs.add(new Input(path, inputFormat));
		return this;
	}

	public Processor setOutput(Path outputPath, Class<? extends OutputFormat> outputFormat, Class<?> outputKeyClass,
	    Class<?> outputValueClass) {
		this.outputFormat = outputFormat;
		this.outputKeyClass = outputKeyClass;
		this.outputValueClass = outputValueClass;
		this.outputPath = outputPath;
		return this;
	}
	
	public Processor setHandler(ProcessorHandler processorHandler) {
		this.processorHandler = processorHandler;
		return this;
	}

	public Processor(Configuration conf) {
		this.conf = conf;
	}

	public Job createJob() throws IOException, CoGrouperException, URISyntaxException {

		// TODO Checks
		
		Job job = new Job(conf);
		job.setNumReduceTasks(0);

		job.setJarByClass((jarByClass != null) ? jarByClass : processorHandler.getClass());
		job.setOutputFormatClass(outputFormat);
		job.setOutputKeyClass(outputKeyClass);
		job.setOutputValueClass(outputValueClass);
		FileOutputFormat.setOutputPath(job, outputPath);
		
		for(Input input : multiInputs) {
			PangoolMultipleInputs.addInputPath(job, input.path, input.inputFormat, processorHandler);
		}
		return job;
	}
}
