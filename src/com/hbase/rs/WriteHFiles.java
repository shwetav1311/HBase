package com.hbase.rs;


import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.charset.Charset;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Date;
import java.util.Set;
import java.util.TreeMap;

import com.hbase.miscl.HBase.Cell;
import com.hbase.miscl.HBase.Column;
import com.hbase.miscl.HBase.ColumnFamily;
import com.hbase.miscl.HBase.IndexEntry;
import com.hbase.miscl.HBase.IndexList;
import com.hbase.miscl.HBase.Row;
import com.hbase.miscl.HBaseConstants;

public class WriteHFiles {
	
	public static TreeMap<String, TreeMap<String, TreeMap<String, ArrayList<Cell> > > > memStore;
	
	public WriteHFiles(TreeMap<String, TreeMap<String, TreeMap<String, ArrayList<Cell> > > > tempStore) {
		// TODO Auto-generated constructor stub
		memStore = tempStore ;
	}
	
	
	
	public void write(String tableName,String start, String end,String version)
	{
		/** get all the values in sorted order **/
			
		Set<String> keys = memStore.keySet();
		
		FileOutputStream stream= null;
		FileOutputStream indexOut= null;
		try {
			
			Date date= new Date();
			Timestamp ts = new Timestamp(date.getTime());
			String timeStamp = ts.toString();
			
			String fileName = timeStamp + HBaseConstants.FILE_SEPARATOR + tableName + HBaseConstants.FILE_SEPARATOR + start;
			
			stream = new FileOutputStream(fileName); //name of the file has to be decided
			
			String indexFile = "index"+ HBaseConstants.FILE_SEPARATOR  +timeStamp + HBaseConstants.FILE_SEPARATOR + tableName + HBaseConstants.FILE_SEPARATOR + start;
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
        	TreeMap<String, TreeMap<String, ArrayList<Cell> > > tempColFamilies = memStore.get(key);
        	
        	//prepares Column Families
        	for(String colFamilyName: tempColFamilies.keySet())
        	{
        		ColumnFamily.Builder myColFam = ColumnFamily.newBuilder();
        		myColFam.setName(colFamilyName);
        		
        		ArrayList<Column.Builder> myColumns = new ArrayList<>();
        		TreeMap<String, ArrayList<Cell> > myColumnMap = tempColFamilies.get(colFamilyName);
        		
        		//Prepares Columns
        		for(String colName : myColumnMap.keySet())
        		{
        			ArrayList<Cell> myCells = myColumnMap.get(colName);
        			
        			Column.Builder newCol = Column.newBuilder();
        			newCol.setColName(colName);
        			newCol.addAllCells(myCells);        			
        			
        			myColumns.add(newCol);
        		}
        		
        		rowObject.addColFamily(myColFam);
        	}
        	
        	
        	//Write the content to a file
        	byte[] bytes = rowObject.build().toByteArray(); // row object serialized 
			
			int nBytes = bytes.length;
			
			System.out.println("The size of the file is "+nBytes);
			String hexNumber = Integer.toHexString(nBytes);
			
			
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
			
			System.out.println("The hex number is "+hexNumber);
			
					
			
			try {				
				stream.write(hexNumber.getBytes(Charset.forName("UTF-8")));
				stream.write(bytes);
				
			}
			catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
        	
			indexEntryObj.setRowID(key);
			indexEntryObj.setStartByte(startByteIndicator);
			
			indexListObj.addIndex(indexEntryObj);
			startByteIndicator = startByteIndicator  + nBytes; 
			
        }
			
        try {				
			indexOut.write(indexListObj.build().toByteArray());
			indexOut.close();

		}
		catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
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
