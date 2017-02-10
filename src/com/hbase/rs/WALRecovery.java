package com.hbase.rs;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import com.hbase.miscl.Converter;
import com.hbase.miscl.HBaseConstants;
import com.hbase.miscl.HBase.WalEntry;
import com.hdfs.miscl.GetFile;
import com.hdfs.miscl.ListFile;



public class WALRecovery {

	/**
	 * @author master
	 */
	public class HFile
	{
		String timeStamp;
		String fileName; // index File name
	}
	/**
	 * create region
	 * find the WAL file name from the zoo-keeper
	 * once the WAL file is found, do a get of it
	 * find the latest index file and retrieve the sequence ID of it
	 * then do the recovery process, details yet to be sketched
	 **/
	private String tableName;
	private String walFname;
	private String localWal;
	private String localIFile;
	private Region region;
	
	WALRecovery(String tblName,Region rgn)
	{
		tableName = tblName;
		region = rgn;
	}

	/**
	 * find WAL file name
	 */
	void getWALName()
	{
		// this is where I perform get call to the zoo-keeper to get the WAL file Name
		walFname = "RS_0_WAL"; //hard coded for now
	}
	
	
	/**
	 * download WAL file
	 */
	void downloadAndRecoverWAL()
	{
		// create a directory structure
		createDirectory();
		//walFname,HBaseConstants.WAL_DIR+walFname);
		localWal = HBaseConstants.WAL_DIR+walFname;
		getFileFromHDFS(walFname,localWal);
		
		performRecovery();
	}

	/**
	 * set/update WAL name in zookeeper 
	 */
	void setWALName()
	{
		// make call to zookeeper and update wal for the table
	}
	
	
	/** find the last commited sequence number 
	 * read from the downloaded WAL file
	 */
	private void performRecovery()
	{
		int seqID = obtainSequenceNumber();
		readFromWal(seqID);
	}
	
	/**
	 * download the latest Index file and in that look for the latest sequence ID
	 * @return
	 */
	private int obtainSequenceNumber() {
		// TODO Auto-generated method stub
		String latestIFile = searchLatestIFile();
		localIFile = HBaseConstants.WAL_DIR+latestIFile;
		getFileFromHDFS(latestIFile,localIFile);
		
		
		byte[] buffer = new byte[8];
		InputStream is;
		//reads 8 bytes
		
		try {
			is = new FileInputStream(localIFile);
			if (is.read(buffer) != buffer.length) { 
			    System.out.println("Some problem the index file is not even 8 bytes long"); 
			}
			is.close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		String str = new String(buffer, StandardCharsets.UTF_8);
		
		return Converter.hexToDec(str); // this is the sequence ID
		
		
	}

	/**
	 * get the latest Index File
	 * possible errors : might not return the IFile, rabdi's code need to verify with her
	 * @return
	 */
	synchronized String searchLatestIFile()
	{
		
		ListFile listFile = new ListFile(tableName+HBaseConstants.FILE_SEPARATOR+"0");
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

		/** this is the custom sort for HFiles **/
		/*******************************************************/
		@Override
		public int compare(HFile o1, HFile o2) {
			// TODO Auto-generated method stub
			
		
			return  o2.timeStamp.compareTo(o1.timeStamp) ; //descending order
		} });
		/*******************************************************/
		
		String indexFileName = hList.get(0).fileName;
		System.out.println("The latest Index File is "+indexFileName);
		return indexFileName;
	}
	
	
	/**
	 * create a directory, inside this we can download wal files and IndexFiles
	 * possible errors: filename not found excpetion may shoot up if directory is not created
	 */
	private static void createDirectory() {
		// TODO Auto-generated method stub
		File theDir = new File(HBaseConstants.WAL_DIR);

		// if the directory does not exist, create it
		if (!theDir.exists()) {
		    System.out.println("creating directory: " + HBaseConstants.WAL_DIR);
		    boolean result = false;

		    try{
		        theDir.mkdir();
		        result = true;
		    } 
		    catch(SecurityException se){
		        //handle it
		    }        
		    if(result) {    
		        System.out.println("WAL recovery DIR created");  
		    }
		}
		
	}
	
	public static void main(String[] args)
	{
		createDirectory();
	}
	
	/**
	 * Get file from HDFS, inputFile( present in HDFS), outputFile( downloaded to local FIle system)
	 * possible errors: file get from HDFS fails!, this is a simple get
	 * @param inputFile
	 * @param outputFile
	 */
	private void getFileFromHDFS(String inputFile,String outputFile)
	{
		GetFile getFile1 = new GetFile(inputFile,outputFile);
		Thread thread1 = new Thread(getFile1);
		thread1.start();
		
		try {
			thread1.join();  //waits for thread to get over
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	
	/**
	 * Read WAL entries
	 * Possible errors: wrong parsing of bytes, or null data
	 * insert into memstore might fail
	 */
	private void readFromWal(int seqID) 
	{
		 byte[] bs = new byte[8];
		 FileInputStream fis = null;
		 
		 try {
			
			System.out.println("Reading "+localWal);
			
			fis = new FileInputStream(localWal);
			
			while(fis.read(bs,0,8)!=-1)
			{
				String str = new String(bs, StandardCharsets.UTF_8);
				System.out.println("The length we are looking for is "+str);
				int len = Converter.hexToDec(str);
				
				System.out.println("The length we are looking for in int is "+len);				
				byte[] data = new byte[len];
			
				int i = fis.read(data,0,len);
				System.out.println("Read bytes: "+i);
				
				/** wal entry is here.. WALEntry consists of: seqID, rsID, putrequest **/ 
				WalEntry trans = WalEntry.parseFrom(data);
				if(Integer.parseInt(trans.getSeqID()) > seqID)
				{
					//add entry to the memStore
					region.insertRow(trans.getLogEntry(),seqID);
				}
			}
			
			fis.close();			
			
//			System.out.println(rows);
			
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
         
	}
	
}
