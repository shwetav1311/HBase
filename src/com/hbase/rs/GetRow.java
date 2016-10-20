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
	
	
	
	ArrayList<ColumnFamily>  performGet(String rowkey,ArrayList<ColumnFamily> colFamily)
	{
		ArrayList<ColumnFamily> columnFamilies = new ArrayList<>();
		
		for(ColumnFamily cFamily : colFamily)
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
			}
			
			ColumnFamily.Builder ansCFamily = ColumnFamily.newBuilder();
			ansCFamily.setName(colFamilyName);

			Column.Builder ansColumn = Column.newBuilder(cFamily.getColumns(0));
			Cell.Builder cell = Cell.newBuilder();
			cell.setColValue(val);
			
			ansColumn.addCells(cell);
			
			columnFamilies.add(ansCFamily.build());
			
		}
		
		
		return columnFamilies;
	}
	
	
	String searchMemStore(String rowKey,String colFamily,String colName)  // when get method searches in memstore
	{
		
		try
		{
			return store.memStore.get(rowKey).get(colFamily).get(colName).get(0).getColValue();
		}catch(NullPointerException e)
		{
			return null;
		}
		
	}
	
	String searchInHFile(String rowKey,String colFamil,String colName)
	{
		
//		PutFile putFile = new PutFile(filename,filename)
		ListFile listFile = new ListFile(tableName+HBaseConstants.FILE_SEPARATOR+startKey);
//		ListFile listIndexFile = new ListFile("index_"+tableName+"_"+startKey);
		List<String> listNames = listFile.list();
		
		List<HFile> hList = new ArrayList<>();
		
		for(String fileName: listNames)
		{
			String[] token = fileName.split(HBaseConstants.FILE_SEPARATOR);
			HFile hFile = new HFile();
			hFile.timeStamp=token[0];
			hFile.fileName = fileName;
			hList.add(hFile);
		}
		
		Collections.sort(hList, new Comparator<HFile>() {

			@Override
			public int compare(HFile o1, HFile o2) {
				// TODO Auto-generated method stub
				return -(o1.timeStamp.compareTo(o2.timeStamp));  //descending order
			}
		});
		
		//search all hfiles in reverse order
		
		for(HFile list: hList)
		{
			GetFile getFile  = new GetFile(list.fileName, Constants.OUTPUT_FILE+list.fileName);
			
			Thread thread = new Thread(getFile);
			
			try {
				thread.join();  //waits for thread to get over
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			
			String indexFile = "index_"+list.fileName;
			
			GetFile getFile1 = new GetFile(indexFile, Constants.OUTPUT_FILE+indexFile);
			
			
			Thread thread1 = new Thread(getFile1);
			
			try {
				thread1.join();  //waits for thread to get over
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			
			Integer offset = returnOffsetHFile(Constants.OUTPUT_FILE+indexFile, rowKey);
			
			if(offset!=-1)
			{
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
				
			
			}
			
			
		}
		
		
		
		
		
		
		return null;
	}
	
	
	Integer returnOffsetHFile(String fileName,String rowKey)
	{
		File file = new File(fileName);
		FileInputStream fis;
		try {
			fis = new FileInputStream(file);
			byte[] data = new byte[(int) file.length()];
			fis.read(data);
			fis.close();
			
			IndexList indexList = IndexList.parseFrom(data);
			
			ArrayList<IndexEntry> indexArr = (ArrayList<IndexEntry>) indexList.getIndexList();
			
			Integer first = 0;
			Integer last = indexArr.size()-1;
			
			while(first<=last)
			{
				int mid = (first+last)/2;
				if(indexArr.get(mid).getRowID().equals(rowKey))
				{
					return indexArr.get(mid).getStartByte();
				}
				
				else if(indexArr.get(mid).getRowID().compareTo(rowKey)<0)
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
	
	
	List<ColumnFamily> getRowFromHFile(String fileName,Integer offset)
	{
		 byte[] bs = new byte[8];
		 FileInputStream fis = null;
		 
		 try {
			fis = new FileInputStream(fileName);
			
			fis.read(bs,offset,8);
			
			
			String str = new String(bs, StandardCharsets.UTF_8);
			System.out.println("The length we are looking for is "+str);
			int len = Converter.hexToDec(str);
			
			byte[] data = new byte[len];			
			
			fis.read(data,offset+8,len);
			
			fis.close();
			
			Row rows = Row.parseFrom(data);
			
			
			
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
