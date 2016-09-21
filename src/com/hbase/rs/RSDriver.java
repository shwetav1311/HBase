package com.hbase.rs;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;

import com.google.protobuf.InvalidProtocolBufferException;
import com.hbase.miscl.HBase.CreateTableRequest;
import com.hbase.miscl.HBase.CreateTableResponse;
import com.hbase.miscl.HBaseConstants;
import com.hdfs.miscl.Constants;
import com.hdfs.miscl.PutFile;

public class RSDriver implements IRegionServer {
	
	
	static int id;
	
	
	public static void main(String[] args)
	{
		id=Integer.parseInt(args[0]);
		
		System.out.println("Region server Binding to Registry...");
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
		 * Some program will identify the region and give its reference
		 *  **/
		return null;
	}

	@Override
	public byte[] get(byte[] inp) throws RemoteException {
		// TODO Auto-generated method stub
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
	
}
