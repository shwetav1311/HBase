package com.hbase.client;

import java.io.IOException;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Map.Entry;
import java.util.StringTokenizer;

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
import com.hbase.zookeeper.ZookeeperConstants;
import com.hdfs.miscl.HDFSConstants;

public class ClientDriverTestGet extends Thread{

	
	/*
	
	hbase> create ’tablename’, 'cfname','cfname'   
	//Use ‘put’ to insert data into the table
	hbase> put ’tablename’, ’rowkey’, ’cfname:colname’, ‘value’
	
	 put 'Employee' '1' 'Address:City' 'Pune'
	 ./client.sh get 'Employee' '1' 'Address:City'
	//To get row data with rowkey	
	hbase> get ’tablename’, ’rowkey’ , 'cfname:colName'	...	
	
	
	
	
	*/
	
	static int MAX_COUNT=10;

	public  IRegionServer rsStub;
	public  ZooKeeper zk = null;
	
	public  ArrayList<String> tableNames;
	public  ArrayList<String> colFamily;
	public  ArrayList<String> colValues;
	
	public Integer count=0;
	
	private Integer id;
	
	public ClientDriverTestGet(Integer id)
	{
		this.id = id;
	}
	
	public static void main(String[] args) {
		// TODO Auto-generated method stub
				
	}
	
	@Override
	public void run() {
		// TODO Auto-generated method stub
		Integer start=1;
		Integer numTables=0;
		Integer totalEntries = 30;
		
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
		
		
		ArrayList<ArrayList<String>> multipleTables = prepareInput();
		numTables = 3;

		long startTime = System.currentTimeMillis();
		while (start <= totalEntries) {

			for (int i = 0; i < numTables; i++) {
				ArrayList<String> tableInput = multipleTables.get(i);
				getTable(start.toString(), tableInput.get(1), tableInput);
			}
			start++;

		}
		long endTime = System.currentTimeMillis();
//		System.out.println("Total Time taken : " + (endTime - startTime) / 1000);
//		System.out.println("ID: "+id);
//		System.out.println("Success "+count);
	}
	
	
	public  ArrayList<ArrayList<String>> prepareInput()
	{
//		id.
		
		tableNames = new ArrayList<>();
		tableNames.add("Sports"+id.toString());
		tableNames.add("Movies"+id.toString());
		tableNames.add("Politics"+id.toString());
		
		colFamily = new ArrayList<>();
		colFamily.add("Cricket:Player");
		colFamily.add("Drama:Actor");
		colFamily.add("National:Politician");
		
		colValues = new ArrayList<>();
		colValues.add("Sachin");
		colValues.add("Amitabh");
		colValues.add("NarendraModi");
		
		ArrayList<ArrayList<String>> multipleTables = new ArrayList<ArrayList<String>>();
		
		for(int i=0;i<3;i++)
		{
			ArrayList<String> oneTable = new ArrayList<String>();
	
			oneTable.add("get");
			oneTable.add(tableNames.get(i));
			oneTable.add("1"); //row key
			oneTable.add(colFamily.get(i));
			
			multipleTables.add(oneTable);
		}
		
//		System.out.println("Here :"+multipleTables.toString());
		
		return multipleTables;
	}
	
	
	public  void createTable(String tableName,String[] args)
	{
		CreateTableRequest.Builder req = CreateTableRequest.newBuilder();
		req.setTableName(tableName);
		
		System.out.println("The args are "+args.length);
		
		for(int i=2;i<args.length;i++)
		{
			req.addColFamilies(args[i]);
		}
		String path= com.hbase.zookeeper.ZookeeperConstants.HBASE_MASTER;
		byte[] bs = null;
		
		int cnt=0;
		
		while(cnt<=MAX_COUNT)
		{
			try {
				bs = zk.getData(path,false,null);
				String str = new String(bs);
				
				System.out.println("we got the ip and port of master:-"+str);	
				
				StringTokenizer st = new StringTokenizer(str,":");
				
				String Master_ip=st.nextToken();
				String Master_port=st.nextToken();
				
				System.out.println("Master ip is "+Master_ip+" port is "+Master_port);
				
				Registry registry = null;	
				registry = LocateRegistry.getRegistry(Master_ip,Integer.parseInt(Master_port));
				
				System.out.println(HBaseConstants.RS_DRIVER);
				
			    rsStub = (IRegionServer) registry.lookup(HBaseConstants.RS_DRIVER);
			    
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
				break;
				
			} catch (InvalidProtocolBufferException| KeeperException|InterruptedException|NumberFormatException|RemoteException | NotBoundException e2) {
				// TODO Auto-generated catch block
//				e2.printStackTrace();
				System.out.println("Exception: "+e2.getMessage());
				
			} 	
			
			try {
				new Thread().sleep(1000);
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			
			cnt++;
		
		}//end of while
		
	}
	
//	put ’tablename’, ’rowkey’, ’cfname:colname’, ‘value’
	
	public  void putTable(String rowkey, String tableName,ArrayList<String> args)
	{
		
		{
			PutRequest.Builder putRequest = PutRequest.newBuilder();
			putRequest.setTableName(tableName);
			putRequest.setRowkey(rowkey);
			
			
			
			/* club all column families together */
			HashMap<String, ArrayList<Column>> map = new HashMap<>();
			
			for(int i=3;i<args.size();)
			{
				String[] col = args.get(i).split(":");
				i++;
				String value = args.get(i);
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
			
			int cnt=0;
			while(cnt<=MAX_COUNT)
			{
				IRegionServer tblRegionStub;
				try {
					tblRegionStub = getRegionStub(tableName);
					
					byte[] res;
					res = tblRegionStub.put(putRequest.build().toByteArray());
					PutResponse putResponse = PutResponse.parseFrom(res);
					if (putResponse.getStatus() == HDFSConstants.STATUS_SUCCESS)
					{
						System.out.println("Success");
					}else
					{
						System.out.println("Failure");
					}
					break;
					
					
				} catch (NotBoundException |KeeperException | InterruptedException|RemoteException|InvalidProtocolBufferException e) {
					// TODO Auto-generated catch block
//					e.printStackTrace();
					System.out.println("Exception: "+e.getMessage());
				} 
				
				try {
					new Thread().sleep(1000);
				} catch (InterruptedException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				
				
				cnt++;
				
			}
			
			
			
			

		}
		
		

		
		
	}
	
private  IRegionServer getRegionStub(String tableName) throws KeeperException, InterruptedException, NumberFormatException, RemoteException, NotBoundException {
	byte[] bs = null;
	
	 bs=zk.getData(ZookeeperConstants.HBASE_META+"/"+tableName,false,null);
		
	
	String str = new String(bs);
//	System.out.println("ip "+str);
	StringTokenizer st = new StringTokenizer(str,":");  
	String Assignedreg_ip=st.nextToken();
    String Assignedreg_port=st.nextToken();
    IRegionServer rsStub=null;
	Registry registry = null;
	
	registry = LocateRegistry.getRegistry(Assignedreg_ip,Integer.parseInt(Assignedreg_port));
	
	rsStub = (IRegionServer) registry.lookup(HBaseConstants.RS_DRIVER);
	 	
      
	return rsStub;
	
		
		
	}


	//	get ’tablename’, ’rowkey’ , 'cfname:colName'	
	public  void getTable(String rowkey,String tableName,ArrayList<String> args)
	{
		GetRequest.Builder getRequest = GetRequest.newBuilder();
		getRequest.setTableName(tableName);
		getRequest.setRowkey(rowkey);
		
		/* club all column families together */
		HashMap<String, ArrayList<Column>> map = new HashMap<>();
		
		
		
		for(int i=3;i<args.size();i++)
		{
			String[] col = args.get(i).split(":");
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
		
		int cnt=0;
		while(cnt<=MAX_COUNT)
		{
			IRegionServer tblRegionStub;
			try {
				tblRegionStub = getRegionStub(tableName);
				byte[] res;
				res = tblRegionStub.get(getRequest.build().toByteArray());
				GetResponse getResponse = GetResponse.parseFrom(res);
				
				if(getResponse.getStatus()==HDFSConstants.STATUS_SUCCESS)
				{
//					System.out.print("success");
//					System.out.println(getResponse.getColFamilyCount());
					if(getResponse.getColFamilyCount()==0)
					{
						System.out.println("Not Found");
						System.out.println("ID is"+id);
						System.out.println("RowKey"+rowkey);
					}else
					{
						System.out.print(getResponse.getColFamily(0));
					}
					
					
					
					count++;
					break;
				}else
				{
					System.out.print("falild");
				}
				
				
				
			} catch (NotBoundException | KeeperException | InterruptedException|RemoteException| InvalidProtocolBufferException e) {
				// TODO Auto-generated catch block
//				e.printStackTrace();
				System.out.println("Exception: "+e.getMessage());
				
				
			} catch(Exception e)
			{
				e.printStackTrace();
			}
			try {
				new Thread().sleep(1000);
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			
			cnt++;
		}
		
		
		
		
		
	}

	

}
