package com.hbase.rs;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import com.hbase.miscl.Converter;
import com.hbase.miscl.HBase.Cell;
import com.hbase.miscl.HBase.Column;
import com.hbase.miscl.HBase.ColumnFamily;
import com.hbase.miscl.HBase.IndexEntry;
import com.hbase.miscl.HBase.IndexList;
import com.hbase.miscl.HBase.Row;
import com.hbase.miscl.HBaseConstants;
import com.hdfs.miscl.Constants;
import com.hdfs.miscl.GetFile;
import com.hdfs.miscl.ListFile;

public class GetRow {
	
	
	MemStore store;
	String startKey;
	String tableName;
	
	public class HFile
	{
		String timeStamp;
		String fileName;
	}

	
   public GetRow() {
	// TODO Auto-generated constructor stub
   }
   
   public GetRow(MemStore memStore,String start,String table)
   {
	   	this.store = memStore;
	   	this.startKey = start;
	   	this.tableName = table;
   }
	
	
	
	ArrayList<ColumnFamily>  performGet(String rowkey,List<ColumnFamily> list)
	{
		ArrayList<ColumnFamily> columnFamilies = new ArrayList<>();
		
		for(ColumnFamily cFamily : list)
		{
			String colFamilyName = cFamily.getName();
			String colName = cFamily.getColumns(0).getColName();
			
			String val = searchMemStore(rowkey, colFamilyName,colName); //assume only col name at a time
			if(val!=null)
			{
				//found in memstore
			}else
			{
				val = searchInHFile(rowkey, colFamilyName, colName);
				
				if(val==null)
				{
					System.out.println("Key Not Found in HFile");
					return new ArrayList<ColumnFamily>();
				}
			}
			
//			System.out.println("found "+val);
			ColumnFamily.Builder ansCFamily = ColumnFamily.newBuilder();
			ansCFamily.setName(colFamilyName);

			Column.Builder ansColumn = Column.newBuilder();
			ansColumn.setColName(colName);
			Cell.Builder cell = Cell.newBuilder();
			cell.setColValue(val);
			
			ansColumn.addCells(cell);
			ansCFamily.addColumns(ansColumn);
			columnFamilies.add(ansCFamily.build());
			
//			System.out.println(columnFamilies);
			
		}
		
		
		return columnFamilies;
	}
	
	
	String searchMemStore(String rowKey,String colFamily,String colName)  // when get method searches in memstore
	{
		
		try
		{
			System.out.print("Search in memStore "+store.memStore.get(rowKey).get(colFamily).get(colName).get(0).getColValue());
			return store.memStore.get(rowKey).get(colFamily).get(colName).get(0).getColValue();
		}catch(NullPointerException e)
		{
			return null;
		}
		
	}
	
	synchronized String searchInHFile(String rowKey,String colFamil,String colName)
	{
		
		ListFile listFile = new ListFile(tableName+HBaseConstants.FILE_SEPARATOR+startKey);
		List<String> listNames = listFile.list();
		
		System.out.println("List files"+listNames);
		
		List<HFile> hList = new ArrayList<>();
		
		for(String fileName: listNames)
		{
			if(fileName.startsWith(HBaseConstants.INDEX_PREFIX))
				continue;
			String[] token = fileName.split(HBaseConstants.FILE_SEPARATOR);
			HFile hFile = new HFile();
			hFile.timeStamp=token[0];
			hFile.fileName = fileName;
			hList.add(hFile);
		}
		
		hList.sort(new Comparator<HFile>(){

			@Override
			public int compare(HFile o1, HFile o2) {
				// TODO Auto-generated method stub
				
				
				return  o2.timeStamp.compareTo(o1.timeStamp) ; //descending order
			} });
		
		
		//search all hfiles in reverse order
		
//		System.out.print("After sorting"+hList);
//		
//		for(HFile list: hList)
//		{
//			System.out.println(list.fileName+" "+list.timeStamp);
////			System.out.println(list.timeStamp);
//			
//		}
		
		
		for(HFile list: hList)
		{
			
			String indexFile = "index_"+list.fileName;
			
			
			File f = new File(Constants.OUTPUT_FILE+indexFile);
			if(f.exists()) { 
			    // do something
				
//				System.out.println("Index file Already There "+indexFile);
			}else
			{
				GetFile getFile1 = new GetFile(indexFile, Constants.OUTPUT_FILE+indexFile);
				Thread thread1 = new Thread(getFile1);
				thread1.start();
				
				try {
					thread1.join();  //waits for thread to get over
				} catch (InterruptedException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
			
			
			
			
		
			
			Integer offset = returnOffsetHFile(Constants.OUTPUT_FILE+indexFile, rowKey);
			
//			System.out.println("Offset  to be read is"+offset);
			
			if(offset!=-1)
			{
				
				File f1 = new File(Constants.OUTPUT_FILE+list.fileName);
				if(f1.exists()) { 
				    // do something
//					System.out.println("Already There");
					
				}else
				{
					GetFile getFile  = new GetFile(list.fileName, Constants.OUTPUT_FILE+list.fileName);
					
					Thread thread = new Thread(getFile);
					thread.start();
					
					try {
						thread.join();  //waits for thread to get over
					} catch (InterruptedException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
				}
				
				
				
				
				List<ColumnFamily> cFamily = getRowFromHFile(Constants.OUTPUT_FILE+list.fileName, offset);
				
				for(ColumnFamily c : cFamily)
				{
					if(c.getName().equals(colFamil))
					{
						for(Column col: c.getColumnsList())
						{
							if(col.getColName().equals(colName))
							{
								return col.getCells(0).getColValue();
							}
						}
					}
				}
				
			
			}else
			{
				System.out.println("Index not found in "+Constants.OUTPUT_FILE+indexFile);
			}
			
			
		}
		
		
		
		
		
		
		return null;
	}
	
	
	Integer returnOffsetHFile(String fileName,String rowKey)
	{
//		System.out.println("Index file name is"+fileName);
//		System.out.println("rokey"+rowKey);
		
		File file = new File(fileName);
		FileInputStream fis;
		try {
			fis = new FileInputStream(file);
			
			/*Todo
			 *  read first 8 bytes to get the sequence id
			 */
			
			long totalLen = file.length();
			
			/* skip the sequence ID */
			byte[] bs = new byte[8];
			
			fis.read(bs,0,8);
			
			fis.close();
			
			System.out.println("SequenceID : " + Converter.hexToDec(new String(bs, StandardCharsets.UTF_8)));
			
			fis = new FileInputStream(file);
			fis.skip(8);
			
			byte[] data = new byte[(int) (totalLen - 8)];
			fis.read(data);
			fis.close();
			
			IndexList indexList = IndexList.parseFrom(data);
			
			List<IndexEntry> indexArr = indexList.getIndexList();
			
//			System.out.println("Index list"+indexList);
			
			Integer first = 0;
			Integer last = indexArr.size()-1;
			
			while(first<=last)
			{
				int mid = (first+last)/2;
				if(indexArr.get(mid).getRowID().equals(rowKey))
				{
					return indexArr.get(mid).getStartByte();
				}
				
				else if(indexArr.get(mid).getRowID().compareTo(rowKey)>0)
				{
					
					last=mid-1;
				}else 
				{
					first = mid+1;
				}
				
			}
			
			
			
			
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return -1;
		
		
	}
	
	/**
	 * Remove synchronization and check
	 * @param fileName
	 * @param offset
	 * @return
	 */
	List<ColumnFamily> getRowFromHFile(String fileName,Integer offset) 
	{
		 byte[] bs = new byte[8];
		 FileInputStream fis = null;
		 
		 try {
			
			 System.out.println("Reading "+fileName);
			 System.out.println("Offset "+offset);
			 fis = new FileInputStream(fileName);
			 
			 fis.skip(offset);
			 fis.read(bs,0,8);
			
			
			String str = new String(bs, StandardCharsets.UTF_8);
//			System.out.println("The length we are looking for is "+str);
			int len = Converter.hexToDec(str);
//			System.out.println("The length we are looking for in int is "+len);
			
			byte[] data = new byte[len];			
			
			int i = fis.read(data,0,len);
//			System.out.println("Read "+i);
			
			fis.close();
			
//			System.out.println("Reading lenth"+data.length);
			
			Row rows = Row.parseFrom(data);
			
//			System.out.println(rows);
			
			
			return rows.getColFamilyList();
			
		
			
			
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
         
         // read bytes to the buffer
		 
		 return null;
	}
	
	
	
	
}
