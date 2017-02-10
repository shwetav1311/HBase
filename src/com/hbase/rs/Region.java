package com.hbase.rs;

import java.util.ArrayList;
import java.util.List;

import com.hbase.miscl.HBase.ColumnFamily;
import com.hbase.miscl.HBase.PutRequest;

public class Region {
	
	String startKey;
	String endKey;
	MemStore memStore;
	GetRow getRow;
	PutRow putRow;
	String tableName;
	

	
	public Region(String tableName,String startKey) {
		super();
		this.startKey = startKey;
		this.endKey = "test";
		this.memStore = new MemStore(tableName, startKey, endKey);
		this.getRow = new GetRow(memStore, startKey, tableName);
		this.putRow = new PutRow(memStore);
	}

	void insertRow(PutRequest req,int seqID)
	{
		
		putRow.appendRow(req,seqID);
	}
	
	
	List<ColumnFamily> retreiveRow(String rowkey, List<ColumnFamily> list)
	{
		
		return getRow.performGet(rowkey, (List<ColumnFamily>) list);
	}
	
	
	

}
