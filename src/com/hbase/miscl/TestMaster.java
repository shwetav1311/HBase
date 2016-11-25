package com.hbase.miscl;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.util.Vector;

import com.google.protobuf.InvalidProtocolBufferException;
import com.hbase.miscl.HBase.CreateTableRequest;
import com.hbase.miscl.HBase.CreateTableResponse;
import com.hbase.rs.IRegionServer;
import com.hdfs.miscl.Constants;

public class TestMaster {

	public static Integer putResponseCounter = 0;
	public static Integer putBytes = 0;
	public static Integer getResponseCounter = 0;
	public static Integer getBytes = 0;
	public static String myString;
	
	public static void main(String[] args)
	{
		int noOfClients = 10;
		int choice = 1; //put or get
		int noOfOps = 25000;
		
		
		
		createTableNew();
		
		prepareString();
		
		
		for(int i=0;i<noOfClients;i++)
		{
			Thread myThread = new Thread( new TestPutGet(i, noOfOps, choice));
			myThread.start();
		}
	}
	
	private static void prepareString()
	{
		try {
			myString = new String(Files.readAllBytes(Paths.get("test_input")));
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	
	}
	
	private static void createTableNew()
	{
		
		
		
		String tableName = "MyCountry";
		System.out.println("HBase System Test: put ");
		
		System.out.println("Calling create table ");
		String[] createTable ={"create",tableName,"Address"};
		System.out.println(createTable[0]+" "+createTable[1]+" "+createTable[2]);
		System.out.println(" ----------------------------------------------------------------- ");
		System.out.println();
		
		
			createTable(tableName, createTable);
		
	}
	
	public static synchronized Vector<Integer> updatePutCounter(Integer incr,Integer bytesWritten)
	{
		int val=putResponseCounter;
		int size = putBytes;
		Vector<Integer> myVector = new Vector<>();
		
		if(incr!=0)
		{
			putResponseCounter = putResponseCounter + incr;		
			putBytes = putBytes + bytesWritten;
			val=putResponseCounter;
			size = putBytes;			
		}
		else
		{
			val=putResponseCounter;
			size = putBytes;
			putResponseCounter = 0;
			putBytes = 0;
		}
		
		
		myVector.addElement(val);
		myVector.addElement(size);
		
		return myVector;
	}
	
	/** Get response Counter, increments on every get and resets by a thread **/
	public static synchronized Vector<Integer> updateGetCounter(Integer incr,Integer bytesWritten)
	{
		int val=getResponseCounter;
		int size = getBytes;
		Vector<Integer> myVector = new Vector<>();
		
		if(incr!=0)
		{
			getResponseCounter = getResponseCounter + incr;		
			getBytes = getBytes + bytesWritten;
			val=getResponseCounter;
			size = getBytes;			
		}
		else
		{
			val=getResponseCounter;
			size = getBytes;
			getResponseCounter = 0;
			getBytes = 0;
		}
		
		
		myVector.addElement(val);
		myVector.addElement(size);
		
		return myVector;
	}

	public static void createTable(String tableName,String[] args)
	{
		
		
		IRegionServer rsStub = null;

		String id = "1";
		Registry registry = null;
		try {
			registry = LocateRegistry.getRegistry(HBaseConstants.RS_DRIVER_IP,
					HBaseConstants.RS_PORT + Integer.parseInt(id));
		} catch (RemoteException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();

		}

		try {
			rsStub = (IRegionServer) registry.lookup(HBaseConstants.RS_DRIVER + id);
		} catch (NotBoundException | RemoteException e) {
			// TODO Auto-generated catch block
			System.out.println("Could not find Region Server");
			e.printStackTrace();
		}

		CreateTableRequest.Builder req = CreateTableRequest.newBuilder();
		req.setTableName(tableName);

		for (int i = 2; i < args.length; i++) {
			req.addColFamilies(args[i]);
		}

		try {
			byte[] res = rsStub.create(req.build().toByteArray());
			CreateTableResponse crTableResponse = CreateTableResponse.parseFrom(res);

			if (crTableResponse.getStatus() == Constants.STATUS_SUCCESS) {
				System.out.println("Table created successfully");

			} else if ((crTableResponse.getStatus() == Constants.STATUS_NOT_FOUND)) {
				System.out.println("Table already exists");
			} else {
				System.out.println("Table creation failed");
			}

		} catch (RemoteException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (InvalidProtocolBufferException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

	}

	//put ’tablename’, ’rowkey’, ’cfname:colname’, ‘value’

	
}

