package com.hbase.rs;

import com.hbase.miscl.HBase.PutRequest;

public class PutRow {
	
	MemStore memStore;
	
	
	public PutRow(MemStore memStoreArg) {
		// TODO Auto-generated constructor stub
		memStore = memStoreArg;
	}
	
	void appendRow(PutRequest dataIn)
	{
		memStore.insertIntoMemStore(dataIn);
	}

}
