package com.hbase.rs;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import com.google.protobuf.InvalidProtocolBufferException;
import com.hbase.miscl.HBase.ColumnFamily;
import com.hbase.miscl.HBase.CreateTableRequest;
import com.hbase.miscl.HBase.CreateTableResponse;
import com.hbase.miscl.HBase.GetRequest;
import com.hbase.miscl.HBase.GetResponse;
import com.hbase.miscl.HBase.PutRequest;
import com.hbase.miscl.HBase.PutResponse;
import com.hbase.miscl.HBaseConstants;
import com.hdfs.miscl.Constants;
import com.hdfs.miscl.PutFile;

public class RSDriver implements IRegionServer {
	
	
	static int id;
	static int numBlock = 0;
	//map to store table to Region mapping
	
	static HashMap<String,ArrayList<Region>> regionMap;
	static WAL walObj;
	
	public static void main(String[] args)
	{
		
		/** WAL object, with WAL file Name **/
		walObj = new WAL(HBaseConstants.REGION_SERVER+id+HBaseConstants.WAL_SUFFIX);
		
		walObj.createFile();
		
		regionMap = new HashMap<>();
		
		id=Integer.parseInt(args[0]);
		
		System.out.println("Region server Binding to Registry...");
		
		File f = null;
	    boolean bool = false;
	    try{
	    	f = new File(HBaseConstants.TIMESTAMP_GEN);
	    	bool = f.exists();
	    	if(bool==false)
	    	{
	    		f.createNewFile();
	    		PrintWriter writer = new PrintWriter(HBaseConstants.TIMESTAMP_GEN, "UTF-8");
	    	    writer.println("0");	    	    
	    	    writer.close();
	    	}
	    }
	    catch(Exception e)
	    {
	    	System.out.println("File creation problem");
	    }
		
		bindToRegistry();
	}
	
	/** 
	 * export registry object
	 */
	public static void bindToRegistry()
	{
		System.setProperty("java.rmi.server.hostname",HBaseConstants.RS_DRIVER_IP);
		RSDriver obj = new RSDriver();
		try {
			
			Registry register=LocateRegistry.createRegistry(HBaseConstants.RS_PORT+id);
			IRegionServer stub = (IRegionServer) UnicastRemoteObject.exportObject(obj,HBaseConstants.RS_PORT+id);
			register.rebind(HBaseConstants.RS_DRIVER+id, stub);
			
			System.out.println("Region server started succesfully");
			
		} catch (RemoteException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}		
	}

	@Override
	public byte[] create(byte[] inp) throws RemoteException {
		// TODO Auto-generated method stub
		
		CreateTableResponse.Builder res = CreateTableResponse.newBuilder();
		res.setStatus(Constants.STATUS_FAILED);
		
		try {
			CreateTableRequest req  = CreateTableRequest.parseFrom(inp);
			
			String tableName = req.getTableName();
			
			/* write temporary file to be uploaded to hdfs */
			;
			String temp = "";
			
			for ( String name : req.getColFamiliesList())
			{
				temp=temp+name+"\n";
			}
			
			System.out.println(temp);
			
			byte data[] = temp.getBytes();
			
			FileOutputStream out = new FileOutputStream(tableName);
			out.write(data);
			out.close();
			
			int status = createTableHDFS(tableName);
			
			boolean success = (new File(tableName)).delete();
			
			if(regionMap.get(tableName) == null)
			{
				System.out.println("table not found");
				
				ArrayList<Region> arr = new ArrayList<>();
				Region region = new Region(tableName, "0");
				arr.add(region);
				regionMap.put(tableName,arr);
				
			}
		    
			
			res.setStatus(status);
			
			return res.build().toByteArray();
			
			
		} catch (InvalidProtocolBufferException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		
		return res.build().toByteArray();
	}

	@Override
	public byte[] put(byte[] inp) throws RemoteException {
		// TODO Auto-generated method stub
		/** Put request will be written here
		 * 	Some program will identify the region and give its reference
		 *  **/
		
		/** Generate next Sequence ID **/
		int seqID = getBlockNum();
		
		/**Write into WAL**/
		
		
		PutResponse.Builder res = PutResponse.newBuilder();
		res.setStatus(Constants.STATUS_SUCCESS);
		
		try {
			PutRequest req = PutRequest.parseFrom(inp);
			
			System.out.println("Request Received:  "+req.getRowkey() );
			
			String tableName = req.getTableName();
			
			if(regionMap.get(tableName) == null)
			{
				System.out.println("table not found");
				
				ArrayList<Region> arr = new ArrayList<>();
				Region region = new Region(tableName, "0");
				arr.add(region);
				regionMap.put(tableName,arr);
				
				regionMap.get(tableName).get(0).insertRow(req);
				
				
				
			}else
			{
				regionMap.get(tableName).get(0).insertRow(req);

			}

			appendIntoWAL(seqID+"", HBaseConstants.REGION_SERVER+id+"",inp);
			
		} catch (InvalidProtocolBufferException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}catch (Exception e)
		{
			e.printStackTrace();
			res.setStatus(Constants.STATUS_FAILED);
		}
		
		
		
		return res.build().toByteArray();
	}

	/**
	 * 
	 * @param seqID
	 * @param RSID
	 * @param dataIn
	 */
	private void appendIntoWAL(String seqID, String RSID, byte[] dataIn) {
		// TODO Auto-generated method stub
		walObj.appendToHDFS(seqID,RSID,dataIn);
	}

	@Override
	public byte[] get(byte[] inp) throws RemoteException {
		// TODO Auto-generated method stub
		
		try {
			GetRequest req = GetRequest.parseFrom(inp);
			
			String table = req.getTableName();
			
			if(regionMap.get(table) == null)
			{
				ArrayList<Region> arr = new ArrayList<>();
				Region region = new Region(table, "0");
				arr.add(region);
				regionMap.put(table,arr);
			}
			GetResponse.Builder res = GetResponse.newBuilder();
			res.setStatus(Constants.STATUS_NOT_FOUND);
			
			if(regionMap.get(table)!=null)
			{
				List<ColumnFamily> ans = regionMap.get(table).get(0).retreiveRow(req.getRowkey(),req.getColFamilyList());
				
				System.out.println("ans  size"+ ans.size());
				
				res.setStatus(Constants.STATUS_SUCCESS);
				res.addAllColFamily(ans);
			}	
				
				return res.build().toByteArray();	
			
			
			
		} catch (InvalidProtocolBufferException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		return null;
		
	}

	@Override
	public byte[] scan(byte[] inp) throws RemoteException {
		// TODO Auto-generated method stub
		return null;
	}
	
	
	public static int createTableHDFS(String filename)
	{
		PutFile putFile = new PutFile(filename,filename);
		Thread thread1 = new Thread(putFile);
		thread1.start();
		try {
			thread1.join();
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			
			return Constants.STATUS_FAILED;
		}
		
		
		
		return putFile.getPutStatus();
		
	}
	
	/**
	 * Generate Sequence ID
	 */
	public static synchronized int getBlockNum()
	{
		try {
			
			BufferedReader buff = new BufferedReader(new FileReader(Constants.BLOCK_NUM_FILE));
			String line=buff.readLine();
			buff.close();
			
			Integer num = Integer.parseInt(line);
			num++;
			PrintWriter pw;
			try {
				pw = new PrintWriter(new FileWriter(Constants.BLOCK_NUM_FILE));
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
	
	/**
	 * @author master
	 */
	public void createRegions()
	{
		
	}
	
}
