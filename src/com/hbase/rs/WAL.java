package com.hbase.rs;

import java.io.File;
import java.io.IOException;

import com.google.protobuf.InvalidProtocolBufferException;
import com.hbase.miscl.HBase.PutRequest;
import com.hbase.miscl.HBase.WalEntry;
import com.hdfs.miscl.AppendFile;
import com.hdfs.miscl.PutFile;

public class WAL {

	public String walFname;
	private AppendFile appendObj;
	
	/**
	 * WAL constructor
	 * @param fName
	 */
	WAL(String fName)
	{
		walFname = fName;
		appendObj = new AppendFile();
	}
	
	/**
	 * WAL create File
	 * @param fName
	 */
	void createFile()
	{
		/**
		 * Code plug-in needed 
		 */
		File localWAL = new File(walFname);
		
		
		try 
		{
			if(localWAL.exists()==false)
				localWAL.createNewFile();
		} 
		catch (IOException e1) {
			
			e1.printStackTrace();
		}
		
		PutFile cWAL = new PutFile(walFname);
		Thread thread = new Thread(cWAL);
		thread.start();
		
		
		System.out.println("Empty file created");
		
	}
	
	/**
	 * append to HDFS
	 * @param entry
	 */
	public void appendToHDFS(String seqID, String RSID, byte[] dataIn)
	{
		WalEntry.Builder walEntry = WalEntry.newBuilder();
		walEntry.setRsID(seqID);
		walEntry.setRsID(RSID);
		PutRequest putObj = null;
		
		try {
			
			putObj = PutRequest.parseFrom(dataIn);			
			walEntry.setLogEntry(putObj);
			
			int size = walEntry.build().toByteArray().length;
			size = size + 8;
			
			byte[] dataOut = new byte[size];
			
			 
			appendObj.append(walFname, walEntry.build().toByteArray());
			
		} catch (InvalidProtocolBufferException e) {
			
			// TODO Auto-generated catch block
			e.printStackTrace();
		}		
	}
	
}
