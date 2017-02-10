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
		walEntry.setSeqID(seqID);
		walEntry.setRsID(RSID);
		PutRequest putObj = null;
		
		try {
			
			putObj = PutRequest.parseFrom(dataIn);			
			walEntry.setLogEntry(putObj);
			
			int size = walEntry.build().toByteArray().length;
	
			
			
			Integer length = walEntry.build().toByteArray().length;			
			String hexNumber = Integer.toHexString(length);
			
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
			
			byte[] first8Bytes = new byte[8];
			byte[] data = new byte[size];
			
			System.arraycopy(hexNumber.getBytes(), 0, first8Bytes, 0, 8);
			System.arraycopy(walEntry.build().toByteArray(), 0, data, 0, length);
			
			//http://stackoverflow.com/questions/5513152/easy-way-to-concatenate-two-byte-arrays
			byte[] binDataOut = new byte[first8Bytes.length + data.length];
			
			System.arraycopy(first8Bytes, 0, binDataOut, 0, first8Bytes.length);
			System.arraycopy(data, 0, binDataOut, first8Bytes.length, data.length);
			
			
			appendObj.append(walFname, binDataOut);
			
		} catch (InvalidProtocolBufferException e) {
			
			// TODO Auto-generated catch block
			e.printStackTrace();
		}		
	}
	
}
