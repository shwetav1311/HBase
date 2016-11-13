package com.hbase.rs;


import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Set;
import java.util.TreeMap;

import com.google.protobuf.InvalidProtocolBufferException;
import com.hbase.miscl.HBase.Cell;
import com.hbase.miscl.HBase.Column;
import com.hbase.miscl.HBase.ColumnFamily;
import com.hbase.miscl.HBase.IndexEntry;
import com.hbase.miscl.HBase.IndexList;
import com.hbase.miscl.HBase.Row;
import com.hbase.miscl.Converter;
import com.hbase.miscl.HBaseConstants;
import com.hdfs.miscl.PutFile;

public class WriteHFiles {
	
	public static TreeMap<String, TreeMap<String, TreeMap<String, List<Cell> > > > memStore;
	public long blockNumber; 
	
	public WriteHFiles(TreeMap<String, TreeMap<String, TreeMap<String, List<Cell> > > > tempStore,long blockNum) {
		// TODO Auto-generated constructor stub
		memStore = tempStore ;
		blockNumber = blockNum;
	}
	
	
	
	public void write(String tableName,String start, String end)
	{
		/** get all the values in sorted order **/
			
		Set<String> keys = memStore.keySet();
		
		/** Index files **/
		String hFileName = ""; 
		String indexHFile = "";
		FileOutputStream stream= null;
		FileOutputStream indexOut= null;
		/** index files and their out streams **/
		try {
			
			/** this has to be changed to the ts sent by the region server **/
			String timeStamp = blockNumber + "";
			String fileName = timeStamp + HBaseConstants.FILE_SEPARATOR + tableName + HBaseConstants.FILE_SEPARATOR + start;
			hFileName = fileName;
			stream = new FileOutputStream(fileName); //name of the file has to be decided
			
			String indexFile = HBaseConstants.INDEX_PREFIX + HBaseConstants.FILE_SEPARATOR  +timeStamp + HBaseConstants.FILE_SEPARATOR + tableName + HBaseConstants.FILE_SEPARATOR + start;
			indexHFile = indexFile;
			indexOut= new FileOutputStream(indexFile);
		} catch (FileNotFoundException e2) {
			// TODO Auto-generated catch block
			e2.printStackTrace();
		}
		
		
		IndexList.Builder indexListObj = IndexList.newBuilder();
		int startByteIndicator = 0;
		
        for(String key: keys){
        	
        	Row.Builder rowObject = Row.newBuilder();
        	IndexEntry.Builder indexEntryObj =IndexEntry.newBuilder(); 
        	
        	rowObject.setRowID(key);
        	TreeMap<String, TreeMap<String, List<Cell> > > tempColFamilies = memStore.get(key);
        	
        	//prepares Column Families
        	for(String colFamilyName: tempColFamilies.keySet())
        	{
        		ColumnFamily.Builder myColFam = ColumnFamily.newBuilder();
        		myColFam.setName(colFamilyName);
        		
//        		ArrayList<Column.Builder> myColumns = new ArrayList<>();
        		TreeMap<String, List<Cell>> myColumnMap = tempColFamilies.get(colFamilyName);
        		
        		//Prepares Columns
        		for(String colName : myColumnMap.keySet())
        		{
        			Column.Builder newCol = Column.newBuilder();
        			newCol.setColName(colName);
        			
        			for(Cell cell : myColumnMap.get(colName))
        			{
        				Cell.Builder newCell = Cell.newBuilder();
        				newCell.setColValue(cell.getColValue());
        				newCell.setTimestamp(cell.getTimestamp());
        				newCell.build();
        				newCol.addCells(newCell);
        			}
        			newCol.build();
        			
        			myColFam.addColumns(newCol);
        		}
        		myColFam.build();
        		rowObject.addColFamily(myColFam);
        	}
        	
        	
        	//Write the content to a file
        	byte[] bytes = rowObject.build().toByteArray(); // row object serialized 
        	
        	
			try {
				Row myRow = Row.parseFrom(bytes);
				
				System.out.println("After parsing"+myRow);
			} catch (InvalidProtocolBufferException e1) {
				// TODO Auto-generated catch block
				e1.printStackTrace();
			}
        	
        	
			int nBytes = bytes.length;
			
			System.out.println("The size of the file is "+nBytes);
			String hexNumber = Integer.toHexString(nBytes);
			
			//This is just to prefix something with 0s in case the length doesnt go upto 8 bytes
			/***************************************************************************/
			if(hexNumber.length()<8)
			{
				int prefix = 8 - hexNumber.length();
				String temp = "";
				while(prefix>0)
				{
					temp = temp + "0";
					prefix--;
				}			
				hexNumber = temp + hexNumber;
			}			
			/***************************************************************************/
			System.out.println("The hex number is "+hexNumber);
			
					
			//Writing HFile
			try {				
				stream.write(hexNumber.getBytes(Charset.forName("UTF-8")));
				stream.write(bytes);
				
			}
			catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			
			/** the hfile is written locally, write that to HDFS **/
			
			
        	
			indexEntryObj.setRowID(key);
			System.out.println("Start byte "+startByteIndicator);
			indexEntryObj.setStartByte(startByteIndicator);
			
			indexListObj.addIndex(indexEntryObj);
			
			startByteIndicator = startByteIndicator  +8 + nBytes; 
			
        }
        
        try {
			indexOut.write(indexListObj.build().toByteArray());
			indexOut.close();
		    stream.close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
       
        
        
        
        putIntoHDFS(hFileName);
        putIntoHDFS(indexHFile);
		
		
	}
	
	
	/** file(s) into HDFS **/ 
	private void putIntoHDFS(String hFileName) {
		// TODO Auto-generated method stub
		
		PutFile putFile = new PutFile(hFileName,hFileName);
		Thread thread1 = new Thread(putFile);
		thread1.start();
		try {
			thread1.join();
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		System.out.println("File inserted into HDFS");
	}
	
	



	/** code to convert hex to int **/ 
	public static int hexToDec(String str)
	{
		char[] array = str.toCharArray();
		double answer = 0;
		int k=0;		
		for(int i=7;i>=0;i--)
		{
			char ch = array[i];
			int digit = ch - '0';
			
			switch(ch)
			{
				case 'a': digit = 10;
							break;
				case 'b': digit = 11;
							break;
				case 'c': digit = 12;
							break;
				case 'd': digit = 13;
							break;
				case 'e': digit = 14;
							break;
				case 'f': digit = 15;
							break;
							
				default : break;
							
			}
			
			answer = answer + (digit* Math.pow(16, k));
			k++;
		}
		return (int)answer;
	}
	
	
}
