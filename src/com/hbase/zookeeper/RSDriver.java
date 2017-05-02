package com.hbase.zookeeper;

import java.io.IOException;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;
import java.util.StringTokenizer;

import org.apache.zookeeper.CreateMode;
import org.apache.zookeeper.KeeperException;
import org.apache.zookeeper.ZooDefs;

import com.hbase.rs.IRegionServer;
import com.hbase.zookeeper.*;


public class RSDriver implements IRegionServer{
	public static String ip="127.0.0.1";
	public static String port="10001";
	public static int id;
//	static public List<String> tables = new ArrayList<>();
	public static void main(String[] args) throws IOException, InterruptedException, KeeperException {
		
		
//		args[0]="1";
		
		id = Integer.parseInt("1");
		Runner.start(ip,port,id);
		bindToRegistry();
		
	}
	
	public static void bindToRegistry()
	{
		System.setProperty("java.rmi.server.hostname","127.0.0.1");
		RSDriver obj = new RSDriver();
		try {
			
			Registry register=LocateRegistry.createRegistry(10000+id);
			IRegionServer stub = (IRegionServer) UnicastRemoteObject.exportObject(obj,10000+id);
			register.rebind("RegionServer", stub);
			
			System.out.println("Region server started succesfully");
			
		} catch (RemoteException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}		
	}
//	
//	void initialiseZookeeper(String id)
//	{
//		/* create ephemeral znode hbase/regions/<ip:port>
//		 * 
//		 */
//		
//		
//		
//		
//	}
	static void get_leader(String tablename)
	{
		
	}
	void performLeaderElection()
	{
		/* create/update znode /hbase/master/<ip:port> //master
		 * 
		 */
	}
	
	
	/*Region->Region Server mapping: These z-nodes are present in 
	
	*/
	
	void mapTableToRegion(String tableName,String ip_port)
	{
		/*/hbase/meta/<tableName> content : ip_port of region_server */
		
	}
	
	/*Tablet->WAL file mapping.
	 *  These are permanent z-nodes. 
	 *  These are present in /hbase/wal/<tableName>. 
	 *  The content of the file includes the name of the WAL file used for the table. 
	 *  This z-node is created by the region server that is responsible for the region after it completes log recovery, but before it updates the meta z-node.

*/
	
	void mapTableToWAL(String tableName,String walName)
	{
		
	}


	public static String get_leader_path() {
		return "/hbase/master";
		
	}


	public
	void create(String tableName) throws IOException, InterruptedException, KeeperException {
		System.out.println("Create Started...............");
		tableName="/"+tableName;
		// TODO Auto-generated method stub
		//ZooKeeperConnection conn=new ZooKeeperConnection();
		//Node.zoo=conn.connect("localhost");
		List<String> childNodePaths =Node.zoo.getChildren("/hbase/region", false);
		int size=childNodePaths.size()-1;
		Collections.sort(childNodePaths);
		
		Random rand = new Random();

		int  n = rand.nextInt(size) +1;
		
		String Assigned_reg=childNodePaths.get(n);
		//System.out.println("hey");
		//System.out.println(Assigned_reg);
		Assigned_reg="/hbase/region/"+Assigned_reg;
		byte[] bs=Node.zoo.getData(Assigned_reg,false,null);
		String str = new String(bs);
		System.out.println("The random ip and port of Region server:-      "+str);
		StringTokenizer st = new StringTokenizer(str,":");  
		String Assignedreg_ip=st.nextToken();
				//"127.0.0.1";
		//System.out.println(Assignedreg_ip);
		
		String Assignedreg_port=st.nextToken();
		//System.out.println(Assignedreg_port);
				//"10001";
		String id="1";  // Region Server ID
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
		
//			boolean res = rsStub.loadRegion(tableName,true);
//			Node.zoo.getChildren("/hbase/Meta",true);
		
		//master will allocate table to region server 
		// fetch ip port of that region server.
		//make a remote call loadRegion to that;
		
		//call loadRegion rmi;
		
		// flag=0 create , flag=1 perform recovery
		
		
//		if (!res)
//			System.out.println("Wal and table in zookeeper not successful");
//		else
//			System.out.println("wal and table in zookeeper is successful");
	
	}


	
	public boolean loadRegion(String tableName, boolean flag) throws IOException, InterruptedException {
		// TODO Auto-generated method stub
		System.out.println("Load Started on random region server ......");
//		ZooKeeperConnection conn=new ZooKeeperConnection();
//		ZooKeeper =conn.connect("localhost");
		String ip_port=ip+":"+port;
		boolean success= true;
		
		try{
		String metatablePath = Node.zoo.create("/hbase/Meta"+tableName, ip_port.getBytes(),
					ZooDefs.Ids.OPEN_ACL_UNSAFE,
					CreateMode.EPHEMERAL);
		
		
//		System.out.println("Tables siize is "+tables.size());
//		for(int i=0;i<tables.size();i++)
//			System.out.println(tables.get(i));
//		
			System.out.println("[Process: " + id
					+ "] Process node created with path: " + metatablePath);
			List<String> tables = Node.zoo.getChildren("/hbase/Meta",true);
			
			}
			catch(Exception e){
				System.out.println(e);
				success=false;
			
			}
		String walname="RS_"+id+"WAL";
		
		try{
			
			String metatablePath = Node.zoo.create("/hbase/wal"+tableName, walname.getBytes(),
						ZooDefs.Ids.OPEN_ACL_UNSAFE,
						CreateMode.PERSISTENT);
				System.out.println("[Process: " + id
						+ "] Process node created with path: " + metatablePath);
				}
				catch(Exception e){
					
					System.out.println("Wal path already created");
					System.out.println("Setting Wal file name with region sever id.....");
					try {
						Node.zoo.setData("/hbase/wal"+tableName,walname.getBytes(),-1);
					} catch (KeeperException e1) {
						// TODO Auto-generated catch block
						e1.printStackTrace();
					}
					
					
				}
		//create ephemeral table in hbase/meta/employee
		// data is ip:port
		// hbase/WAL/employee - update data as wal file name;
		if(!success)
			return false;
		else
		return true;
	}

	@Override
	public byte[] create(byte[] inp) throws RemoteException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public byte[] put(byte[] inp) throws RemoteException {
		// TODO Auto-generated method stub
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

	@Override
	public byte[] loadRegion(byte[] inp) throws RemoteException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public byte[] unloadRegion(byte[] inp) throws RemoteException {
		// TODO Auto-generated method stub
		return null;
	}

	
	
}
