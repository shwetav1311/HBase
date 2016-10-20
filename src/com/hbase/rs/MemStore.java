package com.hbase.rs;

import java.util.ArrayList;
import java.util.TreeMap;

import com.hbase.miscl.HBase.Cell;

public class MemStore {
	
	TreeMap<String, TreeMap<String, TreeMap<String, ArrayList<Cell> > > > memStore;
	TreeMap<String, TreeMap<String, TreeMap<String, ArrayList<Cell> > > > tempStore; 
	
	
	void insertIntoMemStore()
	{
		
	}
	
	
	void isMemStoreFull()
	{
		
	}
	
	ArrayList<com.hbase.miscl.HBase.ColumnFamily> searchMemStore()  // when get method searches in memstore
	{
		return null;
		
	}
	
	
	void writeHFile()
	{
		
	}
	
	
	void convertToList()
	{
		
	}

}
