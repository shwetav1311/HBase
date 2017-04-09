package com.hbase.miscl;

import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.util.Random;

import com.hbase.miscl.TestPutResponse;
import com.google.protobuf.InvalidProtocolBufferException;
import com.hbase.miscl.HBase.CreateTableRequest;
import com.hbase.miscl.HBase.CreateTableResponse;
import com.hbase.rs.IRegionServer;
import com.hdfs.miscl.HDFSConstants;
import com.hbase.miscl.TestHBasePut;


public class TestPutGet implements Runnable {

	public static IRegionServer rsStub;
	public static String[] myCities;
	public static String[] myStates;
	public static String[] myDesignation;
	private Integer clientID; // this is used to generate key numbers
	private Integer noOfOps; // this is used to decide on no of put or get
	private Integer choice; // this is used to tell whether you are looking at put or get
	private static boolean flag = false;
	
	TestPutGet(int clientID,int noOfOps,int choice)
	{
		this.clientID = clientID;
		this.noOfOps = noOfOps;
		this.choice = choice;
	}
	
	@Override
	public void run() {
		// TODO Auto-generated method stub
		callClient(clientID,noOfOps,choice);
	}

	
	public void callClient(int clientID,int noOfOps,int choice) {
		// TODO Auto-generated method stub
		
		// 1. Bind with registry
		// 2. the id 1 is for region server ID
		String id="1";
		Registry registry = null;
		try {
			registry = LocateRegistry.getRegistry(HBaseConstants.RS_DRIVER_IP,HBaseConstants.RS_PORT+Integer.parseInt(id));
		} catch (RemoteException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		
		}
		
			try 
			{
				rsStub = (IRegionServer) registry.lookup(HBaseConstants.RS_DRIVER+id);
			}catch (NotBoundException | RemoteException e) {
				// TODO Auto-generated catch block
				System.out.println("Could not find Region Server");
				e.printStackTrace();
			} 	
		
		String tableName = "MyCountry";
		
		try {
			Thread.sleep(1000);
		} catch (InterruptedException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
		
		initializeData();

		if(choice==0)
			doPut(tableName,noOfOps);	
		else	
			doGet(tableName,noOfOps);
		
			
	}
	
	public  void doPut(String tableName,int noOfOps)
	{
		
		
		
		
		int end = (clientID*noOfOps) + noOfOps;
		int cnt=clientID*noOfOps;
		
		int i =0;
		while(cnt<end)
		{
			StringBuilder myStringBuilder = new StringBuilder();
			myStringBuilder = myStringBuilder.append("put "+tableName+" ");
			myStringBuilder.append(String.valueOf(cnt++)+" "); // row key
			myStringBuilder.append("Address:State "+myStates[i]+" ");
			myStringBuilder.append("Address:City "+myCities[i]+" ");
			myStringBuilder.append("Work:Designation "+myDesignation[i]+" ");
			
			String str[] = myStringBuilder.toString().split(" ");
			
			TestHBasePut obj = new TestHBasePut(rsStub, tableName, str);
			obj.callPutMethod();
			
			i = (i+1)%myCities.length;
			
		}
		
		
				
		

	}
	
	//get tablename rowkey colFamily:colName
	public  void doGet(String tableName,int noOfOps)
	{
		
		int i = 0;
		
		int end = (clientID*noOfOps) + noOfOps;
		int count=clientID*noOfOps;
		
		while(count<end)
		{		
			StringBuilder myStringBuilder = new StringBuilder();
			myStringBuilder = myStringBuilder.append("get "+tableName+" ");
			myStringBuilder.append(String.valueOf(count)+" "); // row key
			myStringBuilder.append("Address:State ");
			
			count++; // retrieve the next ROW ID by key
			String str[] = myStringBuilder.toString().split(" ");
			
			
			TestHBaseGet obj = new TestHBaseGet(rsStub, tableName, str);
			obj.callGetMethod();
			i = (i + 1)%noOfOps;
		}
		
	}
	
	/**Initializes Data for simulating Put **/
	public static void initializeData()
	{
		
		myDesignation = new String[15];
		
		Integer count = 1;
		
		
		
		
		myCities = new String[] {"Bangalore","Pune","Delhi","Madras","Ahmedabad","Jaipur","Bikaner","Mumbai","Saurashtra","Chandigarh","Pondicherry","Lucknow","Panjim","Kolkata","Indore"};
		myStates = new String[] {"Karnataka","Maharashtra","Delhi","TamilNadu","Gujarat","Gujarat","Rajastan","Maharashtra","Gujarat","Chandigarh","Pondicherry","UttarPradesh","Goa","WestBengal","MadhyaPradesh"};
		for(int i=0;i<15;i++)
		{
			myCities[i] = TestMaster.myString;
			myStates[i] = TestMaster.myString;
			
			myDesignation[i] = "SC"+count.toString();
			count++;
		}
	}
	
	/** Put response Counter, increments on every put and resets by a thread **/
		
		
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
			
			if (crTableResponse.getStatus()==HDFSConstants.STATUS_SUCCESS)
			{
				System.out.println("Table created successfully");
				
			}else if ((crTableResponse.getStatus()==HDFSConstants.STATUS_NOT_FOUND))
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

	//put ’tablename’, ’rowkey’, ’cfname:colname’, ‘value’

	
}
