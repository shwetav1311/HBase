package com.hbase.rs;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.TreeMap;

import org.apache.zookeeper.proto.SetWatches;

import com.hbase.miscl.HBase.Cell;
import com.hbase.miscl.HBase.Column;
import com.hbase.miscl.HBase.ColumnFamily;
import com.hbase.miscl.HBase.PutRequest;
import com.hbase.miscl.HBaseConstants;
import com.hdfs.miscl.HDFSConstants;

public class MemStore {
	
	
	TreeMap<String, TreeMap<String, TreeMap<String, List<Cell> > > > memStore;
	private TreeMap<String, TreeMap<String, TreeMap<String, List<Cell> > > > tempStore; 
	private String tableName;
	private String startKey;
	private String endKey;
	private int memStoreSize = 0;
	private long numBlock=0;
	public int lastSeqId = 0; // last sequence ID in the memstore for a particular table 
	
	public MemStore(String tblName,String sKey,String eKey)
	{
		memStore = new TreeMap<String, TreeMap<String, TreeMap<String, List<Cell> > > >();		
		tableName  = tblName;
		startKey = sKey;
		endKey = eKey;
		
	}
	
	synchronized void  insertIntoMemStore(PutRequest dataIn,int seqID)
	{
		/** lastSeqID was introduced to write partially filled memStores, for unLoadTabe call from HMaster**/
		lastSeqId = seqID;
		
//		System.out.println("recaching insert memstore");
		System.out.println("Table name in row insert memstoe "+tableName);
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
//							System.out.println("");

					}
				}
								
			}
			
		}
		incrementCount(dataIn.toByteArray().length,seqID);
		

	}
	
	private synchronized void incrementCount(int size,int seqID)
	{
			memStoreSize= memStoreSize+size;
			if(isMemStoreFull(memStoreSize))
			{
				tempStore = new TreeMap<String, TreeMap<String, TreeMap<String, List<Cell> > > >(memStore);
				writeToHFile(seqID);
//				memStore = new TreeMap<String, TreeMap<String, TreeMap<String, List<Cell> > > >();
				memStore.clear();
//				writeToHFile();
				memStoreSize = 0;
//				memStore.clear();
			}
			
	}
	
	/** This is needed when, an unloadRegion call was made by HMAster to a particular region server
	 * the chosen table's memstore which may be partially filled, has to be written to HFile
	 * For this we needed the latest sequence ID which is common to all tables at the region server level
	 * we have passed it till the memstore level
	 */
	public synchronized void writePartialMemstoreToHFile()
	{
		writeToHFileRecovery(lastSeqId);
	}
	
	/** create a hfile when recovery is done **/
	public synchronized void writeToHFileRecovery(int seqID)
	{
		tempStore = new TreeMap<String, TreeMap<String, TreeMap<String, List<Cell> > > >(memStore);
		System.out.println("Tempstore size::"+tempStore.size());
		if(tempStore.size()!=0)
			writeToHFile(seqID);
		memStore.clear();
		memStoreSize = 0;
	}
	
	/** This write to HFile method is called when a normal sequence of put is called **/
	private synchronized void writeToHFile(int seqID) {
		// TODO Auto-generated method stub
			WriteHFiles myObj = new WriteHFiles(tempStore,getTimeStamp(),seqID);
			myObj.set(tableName,startKey,endKey);
//			myObj.r
			Thread t = new  Thread(myObj);
			t.start();
			
	}

	private  TreeMap<String, TreeMap<String, List<Cell>>> insertNewColumnFamily(ColumnFamily columnFamily,TreeMap<String, TreeMap<String, List<Cell> > > colFamilyMap ) {
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
	
	synchronized boolean  isMemStoreFull(int input_size)
	{
//		return input_size>HBaseConstants.MEMSTORE_CONTENTS;
		
		if(input_size > (HDFSConstants.BLOCK_SIZE*0.8))
		{
			return true;
		}else
		{
			return false;
		}
		
	}
	

	
	public  synchronized Long getTimeStamp()
	{
		
		
		try {
			
			BufferedReader buff = new BufferedReader(new FileReader(HBaseConstants.TIMESTAMP_GEN));
			String line=buff.readLine();
			buff.close();
			
			Long num = Long.parseLong(line);
			num++;
			PrintWriter pw;
			try {
				pw = new PrintWriter(new FileWriter(HBaseConstants.TIMESTAMP_GEN));
			    pw.write(num.toString());
		        pw.close();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			
			return num-1;
			
			
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		return ++numBlock;
	}

}
