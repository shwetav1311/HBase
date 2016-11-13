package com.hbase.rs;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.TreeMap;

import com.hbase.miscl.HBase.Cell;
import com.hbase.miscl.HBase.Column;
import com.hbase.miscl.HBase.ColumnFamily;
import com.hbase.miscl.HBase.PutRequest;
import com.hdfs.miscl.Constants;

public class MemStore {
	
	
	public TreeMap<String, TreeMap<String, TreeMap<String, List<Cell> > > > memStore;
	public TreeMap<String, TreeMap<String, TreeMap<String, List<Cell> > > > tempStore; 
	public static String tableName;
	public String startKey;
	public String endKey;
	public int count = 0;
	
	public MemStore(String tblName,String sKey,String eKey)
	{
		memStore = new TreeMap<String, TreeMap<String, TreeMap<String, List<Cell> > > >();		
		tableName  = tblName;
		startKey = sKey;
		endKey = eKey;
	}
	
	void insertIntoMemStore(PutRequest dataIn)
	{
		
		System.out.println("recaching insert memstore");
		String rowKey = dataIn.getRowkey();
		if(memStore.containsKey(rowKey)==false)
		{
			/** Press ctrl-1 to get quick fix as options **/
			List<ColumnFamily> myColFamily = (List<ColumnFamily>) dataIn.getColFamilyList();
			
			/**Level 2 Map **/
			TreeMap<String, TreeMap<String, List<Cell> > > colFamilyMap = new TreeMap<String, TreeMap<String, List<Cell> > >();  
			for (Iterator<ColumnFamily> iterator = myColFamily.iterator(); iterator.hasNext();) {
				
				ColumnFamily columnFamily = (ColumnFamily) iterator.next();
				
				String colFamilyName = columnFamily.getName();
				List<Column> myColumns = (List<Column>) columnFamily.getColumnsList();
				
				/** Final level Map **/
				TreeMap<String, List<Cell> > cellListMap = new TreeMap<String, List<Cell> >(); 
				for (Iterator<Column> iterator2 = myColumns.iterator(); iterator2.hasNext();) {
					Column column = (Column) iterator2.next();
					
					String columnName = column.getColName();
					List<Cell> myCells =  new ArrayList<Cell>(); //) column.getCellsList();
					myCells.addAll(column.getCellsList()); //check this if there is any problem
					cellListMap.put(columnName, myCells); //Final level map added
					
				}
				
				colFamilyMap.put(colFamilyName, cellListMap);//level 2 map entry added
			}
			
			memStore.put(rowKey, colFamilyMap);//level 1 map entry added
		}
		else
		{
			List<ColumnFamily> myColFamily = (List<ColumnFamily>) dataIn.getColFamilyList();
			/**Level 2 Map **/
			TreeMap<String, TreeMap<String, List<Cell> > > colFamilyMap = memStore.get(rowKey);  
			for (Iterator<ColumnFamily> iterator = myColFamily.iterator(); iterator.hasNext();) {
				
				ColumnFamily columnFamily = (ColumnFamily) iterator.next();
				
				String colFamilyName = columnFamily.getName();
				List<Column> myColumns = (List<Column>) columnFamily.getColumnsList();
				
				if(colFamilyMap.containsKey(colFamilyName)==false)
				{
					colFamilyMap = insertNewColumnFamily(columnFamily, colFamilyMap);
				}
				else /** column family is present **/
				{
					/** Final level Map **/
					TreeMap<String, List<Cell> > cellListMap = colFamilyMap.get(colFamilyName); 
					
					for (Iterator<Column> iterator2 = myColumns.iterator(); iterator2.hasNext();) {
						Column column = (Column) iterator2.next();
						
						String columnName = column.getColName();
						List<Cell> myCell = (List<Cell>) column.getCellsList();
						
						if(cellListMap.containsKey(columnName)==false)/** column key is absent **/
						{
							List<Cell> myCells =  new ArrayList<Cell>(); //) column.getCellsList();
							myCells.addAll(column.getCellsList()); //check this if there is any problem
							cellListMap.put(columnName, myCells); //Final level map added
						}
						else
							cellListMap.get(columnName).addAll(myCell); // check here if something is wrong

					}
				}
								
			}
			
		}
		count++;
		if(isMemStoreFull(count))
		{
			tempStore = memStore;
			memStore = new TreeMap<String, TreeMap<String, TreeMap<String, List<Cell> > > >();
			memStore.clear();
			writeToHFile();
			count = 0;
		}

	}
	
	private  void writeToHFile() {
		// TODO Auto-generated method stub
			WriteHFiles myObj = new WriteHFiles(tempStore);
			
			myObj.write(tableName,startKey,endKey);
	}

	private static TreeMap<String, TreeMap<String, List<Cell>>> insertNewColumnFamily(ColumnFamily columnFamily,TreeMap<String, TreeMap<String, List<Cell> > > colFamilyMap ) {
		// TODO Auto-generated method stub
		
		
		String colFamilyName = columnFamily.getName();
		List<Column> myColumns = (List<Column>) columnFamily.getColumnsList();
		
		/** Final level Map **/
		TreeMap<String, List<Cell> > cellListMap = new TreeMap<String, List<Cell> >(); 
		for (Iterator<Column> iterator2 = myColumns.iterator(); iterator2.hasNext();) {
			Column column = (Column) iterator2.next();
			
			String columnName = column.getColName();
			List<Cell> myCells =  new ArrayList<Cell>(); //) column.getCellsList();
			
			myCells.addAll(column.getCellsList()); // get cells in not a list, just a single value
			cellListMap.put(columnName, myCells); //Final level map added
			
		}
		
		colFamilyMap.put(colFamilyName, cellListMap);//level 2 map entry added
		
		return colFamilyMap;
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
	
	boolean isMemStoreFull(int input_size)
	{
		return input_size>Constants.MEMSTORE_CONTENTS;
		
	}
	

	List<com.hbase.miscl.HBase.ColumnFamily> searchMemStore()  // when get method searches in memstore
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
