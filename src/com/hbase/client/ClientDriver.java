package com.hbase.client;

import java.io.IOException;
import com.hbase.zookeeper.*;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.StringTokenizer;
import java.util.Map.Entry;

import org.apache.zookeeper.KeeperException;
import org.apache.zookeeper.ZooKeeper;

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
import com.hbase.miscl.HBaseConstants;
import com.hbase.rs.IRegionServer;
import com.hdfs.miscl.HDFSConstants;
import com.hbase.zookeeper.*;

public class ClientDriver {

	
	/*
	
	hbase> create ’tablename’, 'cfname','cfname'   
	//Use ‘put’ to insert data into the table
	hbase> put ’tablename’, ’rowkey’, ’cfname:colname’, ‘value’
	
	 put 'Employee' '1' 'Address:City' 'Pune'
	 ./client.sh get 'Employee' '1' 'Address:City'
	//To get row data with rowkey	
	hbase> get ’tablename’, ’rowkey’ , 'cfname:colName'	...	
	
	
	
	
	*/

	public static IRegionServer rsStub;
	public static ZooKeeper zk = null;
	
	public static void main(String[] args) {
		// TODO Auto-generated method stub
		
		String path= com.hbase.zookeeper.ZookeeperConstants.HBASE_MASTER;
		
		ZooKeeperConnection conn=new ZooKeeperConnection();
		//ZooKeeper zk = null;
		try {
			zk = conn.connect(HBaseConstants.ZOOKEEPER_IP);
		} catch (IOException e2) {
			// TODO Auto-generated catch block
			e2.printStackTrace();
		} catch (InterruptedException e2) {
			// TODO Auto-generated catch block
			e2.printStackTrace();
		}
		
		byte[] bs = null;
		try {
			bs = zk.getData(path,false,null);
		} catch (KeeperException e2) {
			// TODO Auto-generated catch block
			e2.printStackTrace();
		} catch (InterruptedException e2) {
			// TODO Auto-generated catch block
			e2.printStackTrace();
		}
		String str = new String(bs);
		
		System.out.println("we got the ip and port of master:-"+str);
		
		
		
		StringTokenizer st = new StringTokenizer(str,":");
		
		String Master_ip=st.nextToken();
		String Master_port=st.nextToken();
		
		System.out.println("Master ip is "+Master_ip+" port is "+Master_port);
		
		
		Registry registry = null;		
		
		try {
			registry = LocateRegistry.getRegistry(Master_ip,Integer.parseInt(Master_port));
			//System.out.println("created");
		} catch (RemoteException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		
		}
		
		try 
		{
    		System.out.println(HBaseConstants.RS_DRIVER);
			
		    rsStub = (IRegionServer) registry.lookup(HBaseConstants.RS_DRIVER);
		  
		 // System.out.println("created2")
		}catch (NotBoundException | RemoteException e) {
			// TODO Auto-generated catch block
			System.out.println("Could not find Region Server");
			e.printStackTrace();
		} 	
		System.out.println("Table creation request started ..............");
		
		String action = args[0];
		String tableName = args[1];
		
		
		if(action.equalsIgnoreCase("create"))
		{
			createTable(tableName, args);
			System.out.println("Create Table call successfully called");
			System.exit(0);
			
		}else if(action.equalsIgnoreCase("put"))
		{
			putTable(tableName, args);
		}else if(action.equalsIgnoreCase("get"))
		{
			getTable(tableName, args);
		}
		
	}
	
	
	public static void createTable(String tableName,String[] args)
	{
		CreateTableRequest.Builder req = CreateTableRequest.newBuilder();
		req.setTableName(tableName);
		
		System.out.println("The args are "+args.length);
		
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
	
//	put ’tablename’, ’rowkey’, ’cfname:colname’, ‘value’
	
	public static void putTable(String tableName,String[] args)
	{
		int cnt=0;
		Integer rowkey=1;
//		while(true)
		{
			PutRequest.Builder putRequest = PutRequest.newBuilder();
			putRequest.setTableName(tableName);
//			putRequest.setRowkey(args[2]);
			putRequest.setRowkey(rowkey.toString());
			
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
			
			IRegionServer tblRegionStub = getRegionStub(tableName);
			
			byte[] res;
			try {
				res = tblRegionStub.put(putRequest.build().toByteArray());
				PutResponse putResponse = PutResponse.parseFrom(res);
				if (putResponse.getStatus() == HDFSConstants.STATUS_SUCCESS)
				{
					System.out.println("Success");
				}else
				{
					System.out.println("Failure");
				}
			
			} catch (RemoteException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (InvalidProtocolBufferException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			
			System.out.println(cnt);
			rowkey++;
			cnt++;

		}
		
		

		
		
	}
	
private static IRegionServer getRegionStub(String tableName) {
	byte[] bs = null;
	try {
	 bs=zk.getData(ZookeeperConstants.HBASE_META+"/"+tableName,false,null);
		
	} catch (KeeperException | InterruptedException e) {
		// TODO Auto-generated catch block
		e.printStackTrace();
	}
	String str = new String(bs);
	StringTokenizer st = new StringTokenizer(str,":");  
	String Assignedreg_ip=st.nextToken();
    String Assignedreg_port=st.nextToken();
    IRegionServer rsStub=null;
	Registry registry = null;
	try {
		registry = LocateRegistry.getRegistry(Assignedreg_ip,Integer.parseInt(Assignedreg_port));
	} catch (RemoteException e1) {
		// TODO Auto-generated catch block
		e1.printStackTrace();
	
	}
	try 
	{
	  rsStub = (IRegionServer) registry.lookup("RegionServer");
	}catch (NotBoundException | RemoteException e) {
		// TODO Auto-generated catch block
		System.out.println("Could not find Region Server");
		e.printStackTrace();
	} 	
      
	return rsStub;
	
		
		
	}


	//	get ’tablename’, ’rowkey’ , 'cfname:colName'	
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
			
			if(getResponse.getStatus()==HDFSConstants.STATUS_SUCCESS)
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

}
