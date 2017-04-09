package com.hbase.rs;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Random;
import java.util.StringTokenizer;

import org.apache.zookeeper.CreateMode;
import org.apache.zookeeper.KeeperException;
import org.apache.zookeeper.ZooDefs;
import org.apache.zookeeper.server.auth.IPAuthenticationProvider;

import com.google.protobuf.InvalidProtocolBufferException;
import com.hbase.miscl.HBase.ColumnFamily;
import com.hbase.miscl.HBase.CreateTableRequest;
import com.hbase.miscl.HBase.CreateTableResponse;
import com.hbase.miscl.HBase.GetRequest;
import com.hbase.miscl.HBase.GetResponse;
import com.hbase.miscl.HBase.LoadRegionRequest;
import com.hbase.miscl.HBase.LoadRegionResponse;
import com.hbase.miscl.HBase.PutRequest;
import com.hbase.miscl.HBase.PutResponse;
import com.hbase.miscl.HBaseConstants;
import com.hbase.zookeeper.Node;
import com.hbase.zookeeper.Runner;
import com.hbase.zookeeper.ZookeeperConstants;
import com.hdfs.miscl.HDFSConstants;
import com.hdfs.miscl.PutFile;

public class RSDriver implements IRegionServer {
	
	
	static Integer id;
	static int seqID = 0; 
	static int recFlag = 0; // 1 means recovery

	//map to store table to Region mapping	
	static HashMap<String,ArrayList<Region>> regionMap;
	static WAL walObj;
	
	
	public static void main(String[] args)
	{
		
		/** WAL object, with WAL file Name **/
		
		
		id=Integer.parseInt(args[0]);
		
		walObj = new WAL(HBaseConstants.REGION_SERVER+id.toString()+HBaseConstants.WAL_SUFFIX);
		
		walObj.createFile();
		
		regionMap = new HashMap<>();
		
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
		
		try {
			new Thread().sleep(3000);
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
			
		
		
	}
	
//	public static void testRecovery(String tableName)
//	{
//		String id="1";  // Region Server ID
//		Registry registry = null;
//		try {
//			registry = LocateRegistry.getRegistry(HBaseConstants.RS_DRIVER_IP,HBaseConstants.RS_PORT+Integer.parseInt(id));
//		} catch (RemoteException e1) {
//			// TODO Auto-generated catch block
//			e1.printStackTrace();
//		
//		}
//		
//		
//		IRegionServer rsStub = null;
//			try 
//			{
//				rsStub = (IRegionServer) registry.lookup(HBaseConstants.RS_DRIVER+id);
//			}catch (NotBoundException | RemoteException e) {
//				// TODO Auto-generated catch block
//				System.out.println("Could not find Region Server");
//				e.printStackTrace();
//			} 	
//		
//			 try {
////				rsStub.loadRegion(tableName, false);
//			} catch (RemoteException e) {
//				// TODO Auto-generated catch block
//				e.printStackTrace();
//			}
//	}
	
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
			register.rebind(HBaseConstants.RS_DRIVER, stub);
			
			System.out.println("Region server started succesfully");
			
		} catch (RemoteException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}	
		
		connectToZoo(HBaseConstants.RS_DRIVER_IP,HBaseConstants.RS_PORT+id+"",id);
	}

	/**
	 * Need to call load region from create, for a randomly chosen region server
	 */
	@Override
	public byte[] create(byte[] inp) throws RemoteException {
		// TODO Auto-generated method stub
		
		CreateTableResponse.Builder res = CreateTableResponse.newBuilder();
		res.setStatus(HDFSConstants.STATUS_FAILED);
		
		try {
			CreateTableRequest req  = CreateTableRequest.parseFrom(inp);
			
			String tableName = req.getTableName();
			
			/* write temporary file to be uploaded to hdfs */			
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
			
			if(status==HDFSConstants.STATUS_SUCCESS)
			{
				try {
					Node.addTable(tableName); // add table to zookeeper
					chooseRSandLoad(tableName);
					
					
				} catch (KeeperException e) {
					// TODO Auto-generated catch block
					
					e.printStackTrace();
					status = HDFSConstants.STATUS_FAILED;
					
				} catch (InterruptedException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
					status = HDFSConstants.STATUS_FAILED;
				}
				
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
		int seqID = getSeqID();
		
		/**Write into WAL**/
		int stat = appendIntoWAL(seqID+"", HBaseConstants.REGION_SERVER+id+"",inp);
		
		System.out.println("---------------Back to put---------------");
		
		PutResponse.Builder res = PutResponse.newBuilder();
		res.setStatus(HDFSConstants.STATUS_SUCCESS);
		
		
		if(stat==HBaseConstants.APPEND_STATUS_FAILURE)
		{
			res.setStatus(HDFSConstants.STATUS_FAILED);
			return res.build().toByteArray();
		}
		
		
		
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
				
				regionMap.get(tableName).get(0).insertRow(req,seqID);
				
			}else
			{
				regionMap.get(tableName).get(0).insertRow(req,seqID);
			}

			
			
		} catch (InvalidProtocolBufferException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}catch (Exception e)
		{
			e.printStackTrace();
			res.setStatus(HDFSConstants.STATUS_FAILED);
		}
		
		
		
		return res.build().toByteArray();
	}

	/**
	 * 
	 * @param seqID
	 * @param RSID
	 * @param dataIn
	 * @return 
	 */
	private int appendIntoWAL(String seqID, String RSID, byte[] dataIn) {
		// TODO Auto-generated method stub
		return walObj.appendToHDFS(seqID,RSID,dataIn);
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
			res.setStatus(HDFSConstants.STATUS_NOT_FOUND);
			
			if(regionMap.get(table)!=null)
			{
				List<ColumnFamily> ans = regionMap.get(table).get(0).retreiveRow(req.getRowkey(),req.getColFamilyList());
				
				System.out.println("ans  size"+ ans.size());
				
				res.setStatus(HDFSConstants.STATUS_SUCCESS);
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
		PutFile putFile = new PutFile(filename);
		Thread thread1 = new Thread(putFile);
		thread1.start();
		try {
			thread1.join();
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			
			return HDFSConstants.STATUS_FAILED;
		}
		
		
		
		return putFile.getPutStatus();
		
	}
	
	/**
	 * Generate Sequence ID
	 */
	public static synchronized int getSeqID()
	{
		Integer num=0;
		try {
						
			BufferedReader buff = new BufferedReader(new FileReader(HBaseConstants.SEQ_ID_FILE));
			String line=buff.readLine();
			buff.close();
			
			num = Integer.parseInt(line);
			num++;
			PrintWriter pw;
			try {
				pw = new PrintWriter(new FileWriter(HBaseConstants.SEQ_ID_FILE));
			    pw.write(num.toString());
		        pw.close();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			
			
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		return num-1;
	}
	
	/**
	 * @author master
	 */
	

	@Override
	public byte[] loadRegion(byte[] inp) throws RemoteException {
		// TODO Auto-generated method stub
		
		LoadRegionRequest loadReq = null;
		try {
			loadReq = LoadRegionRequest.parseFrom(inp);
		} catch (InvalidProtocolBufferException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		String tableName = loadReq.getTableName();
		boolean isCreate = loadReq.getIsCreate();
		
		LoadRegionResponse.Builder respObj = LoadRegionResponse.newBuilder();
		respObj.setStatus(HDFSConstants.STATUS_FAILED);
				
//		String tableName,boolean callerMethod
		
		
		
		if(isCreate == true)
		{
			System.out.println("Create table has called by HMaster ");
			
			/** create a Region  here and add it to the entry, table to region map **/	
			
			String walName = HBaseConstants.REGION_SERVER+id+HBaseConstants.WAL_SUFFIX;
			System.out.println("Wal file name "+walName);
			createRegion(tableName);
			createTableInZookeeper(tableName, walName);
		}
		else
		{
			System.out.println("WAL recovery call");
			/** Things stop here until recovery here **/
			createRegion(tableName);
			
			WALRecovery walObj = new WALRecovery(tableName,regionMap.get(tableName).get(0));
			walObj.getWALName();
			walObj.downloadAndRecoverWAL();
			walObj.setWALName(id.toString());
			
		}
		
		respObj.setStatus(HDFSConstants.STATUS_SUCCESS);
				
		return respObj.build().toByteArray();
		
	}
	
	public void createTableInZookeeper(String tableName,String walName)
	{
		String data = HBaseConstants.RS_DRIVER_IP+":"+(HBaseConstants.RS_PORT+id);
		
		Node.createNode(ZookeeperConstants.HBASE_META, tableName, data,0); // 0 : ephemeral
	
		Node.createNode(ZookeeperConstants.HBASE_WAL, tableName, walName, 1); // 1: persistent
	}
	
	public boolean chooseRSandLoad(String tableName) throws KeeperException, InterruptedException 
	{
		System.out.println("chooseRSand Load...............");
		
		boolean result = false;
		 
		List<String> childNodePaths = null;
	
		childNodePaths = Node.zoo.getChildren(ZookeeperConstants.LEADER_ELECTION_ROOT_NODE, false);
		
		
		int size=childNodePaths.size()-1;
		Collections.sort(childNodePaths);
		
		Random rand = new Random();

		int  n = rand.nextInt(size) +1;
		
		String Assigned_reg=childNodePaths.get(n); // To randomly select a child(new RS) from the list
		
		Assigned_reg=com.hbase.zookeeper.ZookeeperConstants.LEADER_ELECTION_ROOT_NODE+"/"+Assigned_reg;
		
		byte[] bs = null;
		
		bs = Node.zoo.getData(Assigned_reg,false,null);// the data is IP:port, false because any update wont generate an event

		
		String str = new String(bs);
		System.out.println("The random ip and port oion server:-  "+str);
		
		StringTokenizer st = new StringTokenizer(str,":");  
		String Assignedreg_ip=st.nextToken();						
		String Assignedreg_port=st.nextToken();
		
		IRegionServer rsStub=null;
		Registry registry = null;
		
		try {
			registry = LocateRegistry.getRegistry(Assignedreg_ip,Integer.parseInt(Assignedreg_port));
			rsStub = (IRegionServer) registry.lookup(HBaseConstants.RS_DRIVER);
			
			LoadRegionRequest.Builder reqObj = LoadRegionRequest.newBuilder();
			reqObj.setTableName(tableName);
			reqObj.setIsCreate(true);
			
			byte[] responseArray = rsStub.loadRegion(reqObj.build().toByteArray());
			
			LoadRegionResponse respObj = null;
			
			try {
				respObj = LoadRegionResponse.parseFrom(responseArray);
			} catch (InvalidProtocolBufferException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			
			if(respObj.getStatus()==HDFSConstants.STATUS_SUCCESS)
				result = true;
						
			System.out.println("Result for loadRegion "+result);
		}
		catch (RemoteException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
			return false;
			
		} catch (NotBoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			return false;
		}
		
		return result;
		
	}

	
	/**
	 * Creates a mem-store internally, (in memory structure)
	 * @param tableName
	 */
	public void createRegion(String tableName)
	{
		Region newRegion = new Region(tableName, "0"); // table name and start key
		ArrayList<Region> arr = new ArrayList<>();
		arr.add(newRegion);
		regionMap.put(tableName,arr);
	}
	
	
	public static void connectToZoo(String ip,String port,int id)
	{
		try {
			Runner.start(ip,port,id);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (KeeperException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
}
