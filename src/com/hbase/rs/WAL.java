package com.hbase.rs;

import com.google.protobuf.InvalidProtocolBufferException;
import com.hbase.miscl.HBase.PutRequest;
import com.hbase.miscl.HBase.WalEntry;
import com.hdfs.miscl.AppendFile;

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
			
			appendObj.append(walFname, walEntry.build().toByteArray());
			
		} catch (InvalidProtocolBufferException e) {
			
			// TODO Auto-generated catch block
			e.printStackTrace();
		}		
	}
	
}
