package com.hbase.rs;

import java.util.TreeMap;

import com.hbase.miscl.HBase.Cell;
import com.hbase.miscl.HBase.Row;

public class Region {

	String startKey;
	String endKey;
	TreeMap<String, Row> memStore;
	TreeMap<String, Row> tempStore;   // when memstore is full
	int size;
	
	
	/** Handled by Shesh
	 * Major working points in insert
	 * 1. File Format : Table1.st_key.end_key.TS 
	 * 2. Index File: Table1.st_key.end_key.TS.index
	 * 3. Insert into MemStore ( cross Threshold - write in background)
	 * 4. Writing into HFile 
	 * @param rowId
	 * @param cell
	 */
	void insert(String rowId,Cell cell)
	{
		
	}
	
	/** Handled by Rabdirita
	 * 1. Search in MemStore
	 * 2. Index File search (Binary Search)	 * 
	 * @param rowId
	 * @param cell
	 */
	
	void get(String rowId,Cell cell)
	{
		
	}
	
	void isMemStoreFull(int input_size)
	{
		
	}
}
