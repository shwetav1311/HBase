package com.hbase.miscl;

import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;

import java.util.Map.Entry;
import com.hbase.miscl.TestCheckPutResponse;
import com.google.protobuf.InvalidProtocolBufferException;
import com.hbase.miscl.HBase.Cell;
import com.hbase.miscl.HBase.Column;
import com.hbase.miscl.HBase.ColumnFamily;
import com.hbase.miscl.HBase.CreateTableRequest;
import com.hbase.miscl.HBase.CreateTableResponse;
import com.hbase.miscl.HBase.GetRequest;
import com.hbase.miscl.HBase.GetResponse;
import com.hbase.miscl.HBase.PutRequest;
import com.hbase.miscl.HBase.PutResponse;
import com.hbase.rs.IRegionServer;
import com.hdfs.miscl.Constants;
import com.hbase.miscl.TestHBasePutThread;


public class TestPutAndGet implements Runnable {

	public static IRegionServer rsStub;
	public static String[] myCities;
	public static String[] myStates;
	public static String[] myDesignation;
	public static Integer putResponseCounter = 0; 
	
	public static void main(String[] args) {
		// TODO Auto-generated method stub
		
		// 1. Bind with registry
		// 2. Put
//		String id="1";
//		Registry registry = null;
//		try {
//			registry = LocateRegistry.getRegistry(HBaseConstants.RS_DRIVER_IP,HBaseConstants.RS_PORT+Integer.parseInt(id));
//		} catch (RemoteException e1) {
//			// TODO Auto-generated catch block
//			e1.printStackTrace();
//		
//		}
//		
//			try 
//			{
//				rsStub = (IRegionServer) registry.lookup(HBaseConstants.RS_DRIVER+id);
//			}catch (NotBoundException | RemoteException e) {
//				// TODO Auto-generated catch block
//				System.out.println("Could not find Region Server");
//				e.printStackTrace();
//			} 	
		
//		String action = args[0];
		String tableName = "MyCountry";
		System.out.println("HBase System Test: put ");
		
		System.out.println("Calling create table ");
		String[] createTable ={"create",tableName,"Address"};
		System.out.println(createTable[0]+" "+createTable[1]+" "+createTable[2]);
		System.out.println(" ----------------------------------------------------------------- ");
		System.out.println();
//		createTable(tableName, createTable);
		
			
		initializeData();
		TestCheckPutResponse threadPutRes = new TestCheckPutResponse();
		threadPutRes.run();
		
		
		for(int i =0;i<myCities.length;i++)
		{
			StringBuilder myStringBuilder = new StringBuilder();
			myStringBuilder = myStringBuilder.append("put "+tableName+" ");
			myStringBuilder.append(String.valueOf(i)+" "); // row key
			myStringBuilder.append("Address:State "+myStates[i]+" ");
			myStringBuilder.append("Address:City "+myCities[i]+" ");
			myStringBuilder.append("Work:Designation "+myDesignation[i]+" ");
			System.out.println("calling put on ");
			System.out.println(myStringBuilder.toString());
			
			TestHBasePutThread myThread = new TestHBasePutThread(rsStub, tableName, args);
			myThread.run();
			System.out.println(" ----------------------------------------------------------------- ");
			System.out.println();
		}
		
			
	}
	
	public static void initializeData()
	{
		
		myDesignation = new String[15];
		
		Integer count = 1;
		
		myCities = new String[] {"Bangalore","Pune","Delhi","Madras","Ahmedabad","Jaipur","Bikaner","Mumbai","Saurashtra","Chandigarh","Pondicherry","Lucknow","Panjim","Kolkata","Indore"};
		myStates = new String[] {"Karnataka","Maharashtra","Delhi","TamilNadu","Gujarat","Gujarat","Rajastan","Maharashtra","Gujarat","Chandigarh","Pondicherry","Uttar Pradesh","Goa","West Bengal","Madhya Pradesh"};
		for(int i=0;i<15;i++)
		{
			myDesignation[i] = "SC"+count.toString();
			count++;
		}
	}
	
	public static synchronized Integer updateCounter(Integer incr)
	{
		if(incr!=0)			
			putResponseCounter = putResponseCounter + 1;
		else
			putResponseCounter = 0;
		
		return putResponseCounter;
	}
	
		
	public static void createTable(String tableName,String[] args)
	{
		CreateTableRequest.Builder req = CreateTableRequest.newBuilder();
		req.setTableName(tableName);
		
		for(int i=2;i<args.length;i++)
		{
			req.addColFamilies(args[i]);
		}
		
		try {
			byte[] res = rsStub.create(req.build().toByteArray());
			CreateTableResponse crTableResponse = CreateTableResponse.parseFrom(res);
			
			if (crTableResponse.getStatus()==Constants.STATUS_SUCCESS)
			{
				System.out.println("Table created successfully");
				
			}else if ((crTableResponse.getStatus()==Constants.STATUS_NOT_FOUND))
			{
				System.out.println("Table already exists");
			}else
			{
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

	public static void putTable(String tableName,String[] args)
	{
		PutRequest.Builder putRequest = PutRequest.newBuilder();
		putRequest.setTableName(tableName);
		putRequest.setRowkey(args[2]);
		
		/* club all column families together */
		HashMap<String, ArrayList<Column>> map = new HashMap<>();
		
		for(int i=3;i<args.length;)
		{
			String[] col = args[i].split(":");
			i++;
			String value = args[i];
			i++;
			
			Column.Builder column = Column.newBuilder();
			column.setColName(col[1]);
			
			Cell.Builder cell = Cell.newBuilder();
			cell.setColValue(value);
			cell.setTimestamp(new Date().getTime());
			
			column.addCells(cell.build());
			
			if (map.get(col[0]) != null)
			{
				map.get(col[0]).add(column.build());
			}else
			{
				ArrayList<Column> cols = new ArrayList<>();
				cols.add(column.build());
				map.put(col[0],cols);
			}
			
		}
		
		
		
		
		for (Entry<String, ArrayList<Column>> entry : map.entrySet())
		{
			
			ColumnFamily.Builder cFamily = ColumnFamily.newBuilder();
			cFamily.setName(entry.getKey());
			cFamily.addAllColumns(entry.getValue());
			
			putRequest.addColFamily(cFamily);
			
		}

	
		
		
		//put ’tablename’, ’rowkey’, ’cfname:colname’, ‘value’ 
		
		byte[] res;
		try {
			res = rsStub.put(putRequest.build().toByteArray());
			PutResponse putResponse = PutResponse.parseFrom(res);
			if (putResponse.getStatus() == Constants.STATUS_SUCCESS)
			{
				System.out.println("Success");
				//increment the put response count to meausre the throughput
				putResponseCounter = putResponseCounter + 1;
			}else
			{
				System.out.println("failed");
			}
		
		} catch (RemoteException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (InvalidProtocolBufferException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		
	}
// put ’tablename’, ’rowkey’, ’cfname:colname’, ‘value’
	public static void getTable(String tableName,String[] args)
	{
		GetRequest.Builder getRequest = GetRequest.newBuilder();
		getRequest.setTableName(tableName);
		getRequest.setRowkey(args[2]);
		
		/* club all column families together */
		HashMap<String, ArrayList<Column>> map = new HashMap<>();
		
		
		
		for(int i=3;i<args.length;i++)
		{
			String[] col = args[i].split(":");
			Column.Builder column = Column.newBuilder();
			column.setColName(col[1]);
			
			if (map.get(col[0]) != null)
			{
				map.get(col[0]).add(column.build());
			}else
			{
				ArrayList<Column> cols = new ArrayList<>();
				cols.add(column.build());
				map.put(col[0],cols);
			}
			
		}
		
		for (Entry<String, ArrayList<Column>> entry : map.entrySet())
		{
			Cell.Builder cell = Cell.newBuilder();
			cell.setTimestamp(new Date().getTime());
			ColumnFamily.Builder cFamily = ColumnFamily.newBuilder();
			cFamily.setName(entry.getKey());
			cFamily.addAllColumns(entry.getValue());

			getRequest.addColFamily(cFamily.build());

		}
		
		
		byte[] res;
		try {
			res = rsStub.get(getRequest.build().toByteArray());
			GetResponse getResponse = GetResponse.parseFrom(res);
			
			if(getResponse.getStatus()==Constants.STATUS_SUCCESS)
			{
				System.out.print("success");
				System.out.println(getResponse.getColFamilyCount());
				if(getResponse.getColFamilyCount()==0)
				{
					System.out.println("Not Found");
					System.exit(0);
				}
				
				System.out.print(getResponse.getColFamily(0));
			}else
			{
				System.out.print("falild");
			}
		
		} catch (RemoteException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (InvalidProtocolBufferException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		
	}

	@Override
	public void run() {
		// TODO Auto-generated method stub
		
	}

}
