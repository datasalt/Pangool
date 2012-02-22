package com.datasalt.pangool.mapreduce;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.apache.hadoop.conf.Configuration;

import com.datasalt.pangool.cogroup.CoGrouperConfig;
import com.datasalt.pangool.cogroup.CoGrouperException;
import com.datasalt.pangool.cogroup.ConfigBuilder;
import com.datasalt.pangool.cogroup.sorting.Criteria;
import com.datasalt.pangool.cogroup.sorting.Criteria.SortElement;
import com.datasalt.pangool.io.tuple.ITuple;
import com.datasalt.pangool.io.tuple.Schema;

public class GroupComparator extends SortComparator {

	private Criteria groupSortBy;
	
	public GroupComparator(){}
	
	@Override
	public int compare(ITuple w1, ITuple w2) {
		int sourceId1 = grouperConf.getSourceIdByName(w1.getSchema().getName());
		int sourceId2 = grouperConf.getSourceIdByName(w2.getSchema().getName());
		int[] indexes1 = serInfo.getCommonSchemaIndexTranslation(sourceId1);
		int[] indexes2 = serInfo.getCommonSchemaIndexTranslation(sourceId2);
		return compare(w1.getSchema(), groupSortBy, w1, indexes1, w2, indexes2);
	}

	@Override
	public int compare(byte[] b1, int s1, int l1, byte[] b2, int s2, int l2) {
		try{
		Schema commonSchema = serInfo.getCommonSchema();
		return compare(b1,s1,b2,s2,commonSchema,groupSortBy,offsets);
		} catch(IOException e){
			throw new RuntimeException(e);
		}
	}
	
	@Override
	public void setConf(Configuration conf){
		super.setConf(conf);
		if (conf != null){
			List<SortElement> sortElements = grouperConf.getCommonCriteria().getElements();
			int numGroupByFields = grouperConf.getGroupByFields().size();
			List<SortElement> groupSortElements = new ArrayList<SortElement>();
			groupSortElements.addAll(sortElements);
			groupSortElements = groupSortElements.subList(0,numGroupByFields);
			groupSortBy = new Criteria(groupSortElements);					
			ConfigBuilder.initializeComparators(conf, grouperConf);
		}
	}	
}
