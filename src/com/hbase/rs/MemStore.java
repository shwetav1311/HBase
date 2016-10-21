package com.hbase.rs;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.TreeMap;

import com.hbase.miscl.HBase.Cell;
import com.hbase.miscl.HBase.Column;
import com.hbase.miscl.HBase.ColumnFamily;
import com.hbase.miscl.HBase.PutRequest;

public class MemStore {
	
	
	static TreeMap<String, TreeMap<String, TreeMap<String, ArrayList<Cell> > > > memStore;
	static TreeMap<String, TreeMap<String, TreeMap<String, ArrayList<Cell> > > > tempStore; 
	
	
	void insertIntoMemStore(PutRequest dataIn)
	{
		String rowKey = dataIn.getRowkey();
		if(memStore.containsKey(rowKey)==false)
		{
			/** Press ctrl-1 to get quick fix as options **/
			ArrayList<ColumnFamily> myColFamily = (ArrayList<ColumnFamily>) dataIn.getColFamilyList();
			
			/**Level 2 Map **/
			TreeMap<String, TreeMap<String, ArrayList<Cell> > > colFamilyMap = new TreeMap<String, TreeMap<String, ArrayList<Cell> > >();  
			for (Iterator<ColumnFamily> iterator = myColFamily.iterator(); iterator.hasNext();) {
				
				ColumnFamily columnFamily = (ColumnFamily) iterator.next();
				
				String colFamilyName = columnFamily.getName();
				ArrayList<Column> myColumns = (ArrayList<Column>) columnFamily.getColumnsList();
				
				/** Final level Map **/
				TreeMap<String, ArrayList<Cell> > cellListMap = new TreeMap<String, ArrayList<Cell> >(); 
				for (Iterator<Column> iterator2 = myColumns.iterator(); iterator2.hasNext();) {
					Column column = (Column) iterator2.next();
					
					String columnName = column.getColName();
					ArrayList<Cell> myCells =  new ArrayList<Cell>(); //) column.getCellsList();
					myCells.addAll(column.getCellsList()); //check this if there is any problem
					cellListMap.put(columnName, myCells); //Final level map added
					
				}
				
				colFamilyMap.put(colFamilyName, cellListMap);//level 2 map entry added
			}
			
			memStore.put(rowKey, colFamilyMap);//level 1 map entry added
		}
		else
		{
			ArrayList<ColumnFamily> myColFamily = (ArrayList<ColumnFamily>) dataIn.getColFamilyList();
			/**Level 2 Map **/
			TreeMap<String, TreeMap<String, ArrayList<Cell> > > colFamilyMap = memStore.get(rowKey);  
			for (Iterator<ColumnFamily> iterator = myColFamily.iterator(); iterator.hasNext();) {
				
				ColumnFamily columnFamily = (ColumnFamily) iterator.next();
				
				String colFamilyName = columnFamily.getName();
				ArrayList<Column> myColumns = (ArrayList<Column>) columnFamily.getColumnsList();
				
				if(colFamilyMap.containsKey(colFamilyName)==false)
				{
					colFamilyMap = insertNewColumnFamily(columnFamily, colFamilyMap);
				}
				else /** column family is present **/
				{
					/** Final level Map **/
					TreeMap<String, ArrayList<Cell> > cellListMap = colFamilyMap.get(columnFamily); 
					
					for (Iterator<Column> iterator2 = myColumns.iterator(); iterator2.hasNext();) {
						Column column = (Column) iterator2.next();
						
						String columnName = column.getColName();
						ArrayList<Cell> myCell = (ArrayList<Cell>) column.getCellsList();
						
						if(cellListMap.containsKey(columnName)==false)/** column key is absent **/
						{
							ArrayList<Cell> myCells =  new ArrayList<Cell>(); //) column.getCellsList();
							myCells.addAll(column.getCellsList()); //check this if there is any problem
							cellListMap.put(columnName, myCells); //Final level map added
						}
						else
							cellListMap.get(columnName).addAll(myCell); // check here if something is wrong

					}
				}
								
			}
			
		}
		
		if(isMemStoreFull(4))
		{
			tempStore = memStore;
			memStore.clear();
			writeToHFile();
		}

	}
	
	private static void writeToHFile() {
		// TODO Auto-generated method stub
			WriteHFiles myObj = new WriteHFiles(tempStore);
	}

	private static TreeMap<String, TreeMap<String, ArrayList<Cell>>> insertNewColumnFamily(ColumnFamily columnFamily,TreeMap<String, TreeMap<String, ArrayList<Cell> > > colFamilyMap ) {
		// TODO Auto-generated method stub
		
		
		String colFamilyName = columnFamily.getName();
		ArrayList<Column> myColumns = (ArrayList<Column>) columnFamily.getColumnsList();
		
		/** Final level Map **/
		TreeMap<String, ArrayList<Cell> > cellListMap = new TreeMap<String, ArrayList<Cell> >(); 
		for (Iterator<Column> iterator2 = myColumns.iterator(); iterator2.hasNext();) {
			Column column = (Column) iterator2.next();
			
			String columnName = column.getColName();
			ArrayList<Cell> myCells =  new ArrayList<Cell>(); //) column.getCellsList();
			
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
		return input_size>4;
		
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
