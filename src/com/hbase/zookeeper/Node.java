package com.hbase.zookeeper;

import java.io.IOException;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.StringTokenizer;
import java.util.Map.Entry;

import org.apache.zookeeper.CreateMode;
import org.apache.zookeeper.KeeperException;
import org.apache.zookeeper.WatchedEvent;
import org.apache.zookeeper.Watcher;
import org.apache.zookeeper.Watcher.Event.EventType;
import org.apache.zookeeper.ZooDefs;
import org.apache.zookeeper.ZooKeeper;

import com.google.protobuf.InvalidProtocolBufferException;
import com.hbase.miscl.HBaseConstants;
import com.hbase.miscl.HBase.LoadRegionRequest;
import com.hbase.miscl.HBase.LoadRegionResponse;
import com.hbase.rs.IRegionServer;
import com.hbase.zookeeper.ZookeeperConstants;
import com.hdfs.miscl.HDFSConstants;

public class Node implements Runnable {

	private final int id;
	private String hbaseNodePath;
	private String masterNodePath;
	private String regionNodePath;
	private String metaNodePath;
	private String walNodePath;
	private String watchedNodePath;
	private String processNodePath;
	private String dataNodePath;
	
	public static ZooKeeper zoo;
	public int leader = 0; // 0=slave, 1= leader
	String ip;
	String port;
	public Set<String> followers = new HashSet<String>();
//	private static List<String> tableList = new ArrayList<>(); /* only used by master */
	
//	public static void addTable(String tableName) {
//		
//		tableList.add(tableName);
//	}
	
	private boolean triggerFlag=true;
	
	public static List<String > getTableList()
	{
		
		List<String> childNodeList = new ArrayList<>();
		try {
			childNodeList = zoo.getChildren(ZookeeperConstants.HBASE_WAL, false);
		} catch (KeeperException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return childNodeList;
	}

	public int getLeader()
	{
		return leader;
	}

	public Node(final int id, final String zkURL,final String ip_,final String port_) throws IOException {
		this.id = id;
		this.ip=ip_;
		this.port=port_;
		zoo = new ZooKeeper(zkURL, 5000, new ProcessNodeWatcher());
	}

	public void run() {
		System.out.println("Process with id: " + id + " has started!");
		try {
			try{
			hbaseNodePath = zoo.create(ZookeeperConstants.HBASE, "".getBytes(),
					ZooDefs.Ids.OPEN_ACL_UNSAFE,
					CreateMode.PERSISTENT);
			System.out.println("[Process: " + id
					+ "] Process node created with path: " + hbaseNodePath);
			}
			catch(Exception e)
			{}
			try{
			
			masterNodePath = zoo.create(ZookeeperConstants.HBASE_MASTER, "".getBytes(),
					ZooDefs.Ids.OPEN_ACL_UNSAFE,
					CreateMode.PERSISTENT);
			System.out.println("[Process: " + id
					+ "] Process node created with path: " + masterNodePath);
			}
			catch(Exception e){}
			try{
			regionNodePath = zoo.create(ZookeeperConstants.LEADER_ELECTION_ROOT_NODE, "".getBytes(),
					ZooDefs.Ids.OPEN_ACL_UNSAFE,
					CreateMode.PERSISTENT);
			System.out.println("[Process: " + id
					+ "] Process node created with path: " + regionNodePath);
			}
			catch(Exception e)
			{}
			try{
			metaNodePath = zoo.create(ZookeeperConstants.HBASE_META, "".getBytes(),
					ZooDefs.Ids.OPEN_ACL_UNSAFE,
					CreateMode.PERSISTENT);
			System.out.println("[Process: " + id
					+ "] Process node created with path: " + metaNodePath);
			}
			catch(Exception e){}
			try{
			walNodePath = zoo.create(ZookeeperConstants.HBASE_WAL, "".getBytes(),
					ZooDefs.Ids.OPEN_ACL_UNSAFE,
					CreateMode.PERSISTENT);
			System.out.println("[Process: " + id
					+ "] Process node created with path: " + walNodePath);
			}
			catch(Exception e){}
			
			try{
				dataNodePath = zoo.create(ZookeeperConstants.HBASE_MASTER, "".getBytes(),
						ZooDefs.Ids.OPEN_ACL_UNSAFE,
						CreateMode.PERSISTENT);
				System.out.println("[Process: " + id
						+ "] Process node created with path: " + dataNodePath);
				}
				catch(Exception e){}
			String ip_port=ip+":"+port;
			
			processNodePath = zoo.create(ZookeeperConstants.LEADER_ELECTION_ROOT_NODE+"/"+"R", ip_port.getBytes(),
					ZooDefs.Ids.OPEN_ACL_UNSAFE,
					CreateMode.EPHEMERAL_SEQUENTIAL);
			System.out.println("[Process: " + id
					+ "] Process node created with path: " + processNodePath);
			
			System.out.println("Step1 in my creation");
			leaderElection();
			
			if(leader==1)
			{
				allocateTableOnStart();
			}
			
			
		} catch (KeeperException | InterruptedException e) {
			e.printStackTrace();
		}
	}

	
	private void allocateTableOnStart()
	{
		
		
		try {
			new Thread().sleep(20000);
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
//		List<String> tableList = getTableList();
//		for (String table : tableList)
//		{
//			try {
//				assignTableAfterFailure(table);
//			} catch (KeeperException | InterruptedException e) {
//				// TODO Auto-generated catch block
//				e.printStackTrace();
//			}
//		}
		
		Node.createNode(ZookeeperConstants.HBASE_META,"@##@@@","", 0);
		
		
	}
	
	public void leaderElection() throws KeeperException, InterruptedException {
		System.out.println("Step2 in my creation");
		List<String> childNodePaths = zoo.getChildren(ZookeeperConstants.LEADER_ELECTION_ROOT_NODE, false);
		Collections.sort(childNodePaths);
		
		System.out.println("I am the znode with path :" + processNodePath);
		int index = childNodePaths.indexOf(processNodePath.substring(processNodePath.lastIndexOf("/") + 1));
		
		if (index == 0) {
			System.out.println("I am leader");
//			zoo.getChildren(metaNodePath, true);
			String ip_port=ip+":"+port;
			zoo.setData(ZookeeperConstants.HBASE_MASTER,ip_port.getBytes(),-1);
			
			leader = 1;
			
			/*Creating follower list*/
			for (int i = 0; childNodePaths.size() > i; i++)
				followers.add(childNodePaths.get(i));
			
			/*Master putting watch on root node*/
			System.out.println("Setting watch on ROOT_NODE ");
			zoo.getChildren(ZookeeperConstants.HBASE_META,new MetaWatcher());
			zoo.getChildren(ZookeeperConstants.LEADER_ELECTION_ROOT_NODE, true);
			
			//if i am the leader call load balancer 
			runLoadBalancer();
			
			
			
		} else {
			
			System.out.println("The leader is "+childNodePaths.get(0));
			System.out.println("I am follower");
			leader = 0;			
			for (int i = 0; childNodePaths.size()-1 > i; i++)
			{
				/* Follower putting watch on all smallest node */

				watchedNodePath = ZookeeperConstants.LEADER_ELECTION_ROOT_NODE + childNodePaths.get(i);
				if (i == index) {
					i++;
					break;
				}
				// zoo.getChildren(TABLE_ROOT_NODE,new MetaWatcher());
				// System.out.println(" "+watchedNodePath+" ");
				// System.out.println(zoo.exists(watchedNodePath, true));
				zoo.exists(watchedNodePath, true);
			
			}
			/* added watch on the master */
			zoo.getChildren(ZookeeperConstants.LEADER_ELECTION_ROOT_NODE+"/"+childNodePaths.get(0),true);
		}
		
		
	}

	public class ProcessNodeWatcher implements Watcher {
		
		public void process(WatchedEvent event) {
			
			System.out.println("Event  Type : "+event.getType() +" "+ event.getPath());
			 
			if (leader == 0) 
			{
				
				if (EventType.NodeDeleted.equals(event.getType())) {
					System.out.println("Leader has died !!!!!");
					try {
						zoo.getChildren(ZookeeperConstants.HBASE_META,new MetaWatcher());
						leaderElection();
					} catch (KeeperException e) {
						e.printStackTrace();
					} catch (InterruptedException e) {
						e.printStackTrace();
					}
				}
				
				/* If this region server is the leader then tables allocated to the previous leader must be 
				 * reallocated*/
				
				if(leader==1)
					getDifferenceAndAllocateTables();
			} 
			else if (leader == 1)
			{
				/*if leader receive event of "NodeChildrenChanges" than he has to figure out 
				 * which added and deleted. For that it uses his copy of list of active nodes with 
				 * new set of active nodes on zookeeper*/
				if (EventType.NodeChildrenChanged.equals(event.getType())) 
				{
					try {
						List<String> childNodeList = zoo.getChildren(ZookeeperConstants.LEADER_ELECTION_ROOT_NODE, false);
						
						for (String child : childNodeList) 
						{
							if (!followers.contains(child)) {
								System.out.println("New Follower added:" + child);
						}
							else
							{
								followers.remove(child);
							}
						}
						
						if (followers.size() > 0) {
							Iterator<String> nodes = followers.iterator();
							while (nodes.hasNext()) {
								String node_child = nodes.next();
								System.out.println("Follower deleted:"
										+ node_child);
								//System.out.println("table size"+zoo.getChildren(TABLE_ROOT_NODE,false).size());
								//System.out.println(zoo.getChildren(TABLE_ROOT_NODE,false));
								followers.remove(node_child);
							}
						}
						
						followers.clear();
						childNodeList = zoo.getChildren(
								ZookeeperConstants.LEADER_ELECTION_ROOT_NODE, false);
						for(int i=0;i<childNodeList.size();i++)
							followers.add(childNodeList.get(i));

					} catch (KeeperException | InterruptedException e) {
						e.printStackTrace();
					}
					
					try {
						

						zoo.getChildren(ZookeeperConstants.LEADER_ELECTION_ROOT_NODE, true);
						
						
					} catch (KeeperException | InterruptedException e) {
						
						e.printStackTrace();
					}


				}
			

			}
		}
	}
	
	public class MetaWatcher implements Watcher {

		@Override
		public void process(WatchedEvent event) {

			System.out.println("Trigger flag" + getNoTriggerFlag());
			
			if (leader == 1 && getNoTriggerFlag()==true) {

				System.out.println("Event received:   " + event.getType());
				getDifferenceAndAllocateTables();
			} // end-if
			try {

				zoo.getChildren(ZookeeperConstants.HBASE_META, new MetaWatcher());

			} catch (KeeperException | InterruptedException e) {

				e.printStackTrace();
			}

		}
		//
	}
	
	/** If trigger value is false then load balancing is happening 
	 * ignore all other delete events **/
	private synchronized void setNoTriggerFlag(boolean val)
	{
		triggerFlag=val;
	}
	
	private synchronized boolean getNoTriggerFlag()
	{
		return triggerFlag;
	}
	
	
	public void assignTableAfterFailure(String tableName) throws KeeperException, InterruptedException {
		// TODO Auto-generated method stub
		
		System.out.println("Assigning after failure Tablename : "+tableName);
		int result = 0;
		List<String> childNodePaths = zoo.getChildren(ZookeeperConstants.LEADER_ELECTION_ROOT_NODE, false);
	
		
//		int size=childNodePaths.size()-1;
		
		int size=childNodePaths.size();
		Collections.sort(childNodePaths);
		
		Random rand = new Random();

//		int  n = rand.nextInt(size) +1;
		int  n = rand.nextInt(size);
		
		String Assigned_reg=childNodePaths.get(n);
		
		Assigned_reg=ZookeeperConstants.LEADER_ELECTION_ROOT_NODE+"/"+Assigned_reg;
		byte[] bs=zoo.getData(Assigned_reg,false,null);
		String str = new String(bs);
		System.out.println("The random ip and port of Region server:-      "+str);
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
		try {
			registry = LocateRegistry.getRegistry(Assignedreg_ip,Integer.parseInt(Assignedreg_port));
			rsStub = (IRegionServer) registry.lookup(HBaseConstants.RS_DRIVER);
			
			LoadRegionRequest.Builder reqObj = LoadRegionRequest.newBuilder();
			reqObj.setTableName(tableName);
			reqObj.setIsCreate(false);
			
			byte[] responseArray = rsStub.loadRegion(reqObj.build().toByteArray());
			
			LoadRegionResponse respObj = null;
			
			try {
				respObj = LoadRegionResponse.parseFrom(responseArray);
			} catch (InvalidProtocolBufferException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			
			result = respObj.getStatus();
		}
		catch (RemoteException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
			result = HDFSConstants.STATUS_FAILED;
			
			
		} catch (NotBoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			result = HDFSConstants.STATUS_FAILED;
		}
		
		
		System.out.println("Load Region Status : "+ result);
		
	}

	/**
	 * 
	 * @param base
	 * @param node
	 * @param data : ephemeral node, data means ip_port, for persistent node, it is walname
	 * @param nodeType : 0 stands for ephemeral node, 1 stands for persistent node
	 */
	public static void createNode(String base,String node,String data,int nodeType)
	{
		List<String> tables = new ArrayList<>(); // needed to trigger an event
		boolean success = true;
			
		System.out.println("Path    "+base+"/"+node);
		
		if(nodeType==0) //ephemeral node
		{
			try
			{
				
				
				String metatablePath = Node.zoo.create(base+"/"+node, data.getBytes(),
						ZooDefs.Ids.OPEN_ACL_UNSAFE,
						CreateMode.EPHEMERAL);
			
				System.out.println("Ephenode created for table "+ node + " : " + metatablePath);
				tables=zoo.getChildren(base,true);// needed to trigger an event
			}
			catch(Exception e)
			{
				success = false;
				e.printStackTrace();
			}
		}
		else //persistent node
		{
			try
			{
				String metatablePath = Node.zoo.create(base+"/"+node, data.getBytes(),
						ZooDefs.Ids.OPEN_ACL_UNSAFE,
						CreateMode.PERSISTENT);
			
				System.out.println("Persistent node created for table "+ node + " : " + metatablePath);
				tables=zoo.getChildren(base,false);// needed to trigger an event
			}
			catch(Exception e) //trying to create an already existing persistent node  
			{
				System.out.println("Wal path already created");
				System.out.println("Setting Wal file name with region sever id.....");
				try {
					zoo.setData(base+"/"+node,data.getBytes(),-1);
				} catch (Exception e1) {
					// TODO Auto-generated catch block
					success = false;
					e1.printStackTrace();
				}
			}
		}
			
				
	}
	
	
	/* when a table does not have a region */
	private void getDifferenceAndAllocateTables()
	{
		List<String> tableList = getTableList();
	
		
		
		try {
			List<String> childNodeList = zoo.getChildren(ZookeeperConstants.HBASE_META, false);

			for (String child : childNodeList) {

				// System.out.println(child);
				if (!tableList.contains(child)) {
					System.out.println("New table added:     " + child);
				} else {
					if (childNodeList.size() != 0)
						tableList.remove(child);
				}
			}

			int table_size = tableList.size();
			if (tableList.size() > 0) {
				for (int i = 0; i < table_size; i++) {
					String node_child = tableList.get(0);
					System.out.println("table deleted in meta:   " + node_child);

					System.out.println("The Wal filename present in:--" + "/hbase/wal/" + node_child);
					System.out.println("Assigning the delted table to another region server");
					assignTableAfterFailure(node_child);
					tableList.remove(node_child);
					
				}
			}

			tableList.clear();
			childNodeList = zoo.getChildren(ZookeeperConstants.HBASE_META, false);
			for (int i = 0; i < childNodeList.size(); i++)
				tableList.add(childNodeList.get(i));

		} catch (KeeperException | InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		
		
	}
	
	/* get the tables for the ip:port passed */
	public List<String> getTablesForRegion(String str) throws KeeperException, InterruptedException
	{
		List<String> Region_tble=new ArrayList<String>();
		List<String> tables = null ;
		byte[] bs = null;
		
		try {
			tables = Node.zoo.getChildren(ZookeeperConstants.HBASE_META,false);
		} catch (KeeperException | InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		for (int i = 0; i < tables.size(); i++) {
			String tble=tables.get(i);
			tble="/"+tble;
			bs=zoo.getData(ZookeeperConstants.HBASE_META+tble,false,null);
			String res = new String(bs);
			if(res.equals(str))
				Region_tble.add(tble);
			
		}
		
		System.out.println("Table add, "+Region_tble);
		return Region_tble;
		
	}
	
	/* get average number of tables in each region */
	public int getAvg( HashMap<String,List<String>> Map)
	{
		float avg=0;
		int count=0;
		for(Entry<String, List<String>> entry : Map.entrySet()){
			avg+=entry.getValue().size();
			count++;
          
        }
		
        avg=avg/count;
		
		return (int) Math.ceil(avg);
		
	}
	
	/* assign table to region */
	public int loadRegion(String first_q,String tble){
		
		StringTokenizer st = new StringTokenizer(first_q,":"); 
		String Assignedreg_ip=st.nextToken();
		String Assignedreg_port=st.nextToken();
		
		IRegionServer rsStub=null;
		Registry registry = null;
		
		int result=1;
		try {
			registry = LocateRegistry.getRegistry(Assignedreg_ip,Integer.parseInt(Assignedreg_port));
		} catch (RemoteException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		
		}
		try {
			registry = LocateRegistry.getRegistry(Assignedreg_ip,Integer.parseInt(Assignedreg_port));
			rsStub = (IRegionServer) registry.lookup(HBaseConstants.RS_DRIVER);
			
			LoadRegionRequest.Builder reqObj = LoadRegionRequest.newBuilder();
			reqObj.setTableName(tble);
			reqObj.setIsCreate(false);
			
			byte[] responseArray = rsStub.loadRegion(reqObj.build().toByteArray());
			
			LoadRegionResponse respObj = null;
			
			try {
				respObj = LoadRegionResponse.parseFrom(responseArray);
			} catch (InvalidProtocolBufferException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			
			result = respObj.getStatus();
		}
		catch (RemoteException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
			result = HDFSConstants.STATUS_FAILED;
			
			
		} catch (NotBoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			result = HDFSConstants.STATUS_FAILED;
		}
		
		return result;
	
	}
	
public int unLoadRegion(String first_q,String tble){
		
		StringTokenizer st = new StringTokenizer(first_q,":"); 
		String Assignedreg_ip=st.nextToken();
		String Assignedreg_port=st.nextToken();
		
		IRegionServer rsStub=null;
		Registry registry = null;
		
		int result=1;
		try {
			registry = LocateRegistry.getRegistry(Assignedreg_ip,Integer.parseInt(Assignedreg_port));
		} catch (RemoteException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		
		}
		try {
			registry = LocateRegistry.getRegistry(Assignedreg_ip,Integer.parseInt(Assignedreg_port));
			rsStub = (IRegionServer) registry.lookup(HBaseConstants.RS_DRIVER);
			
			LoadRegionRequest.Builder reqObj = LoadRegionRequest.newBuilder();
			reqObj.setTableName(tble);
			reqObj.setIsCreate(false);
			
			byte[] responseArray = rsStub.unloadRegion(reqObj.build().toByteArray());
			
			LoadRegionResponse respObj = null;
			
			try {
				respObj = LoadRegionResponse.parseFrom(responseArray);
			} catch (InvalidProtocolBufferException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			
			result = respObj.getStatus();
		}
		catch (RemoteException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
			result = HDFSConstants.STATUS_FAILED;
			
			
		} catch (NotBoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			result = HDFSConstants.STATUS_FAILED;
		}
		
		return result;
	
	}
	
	
	public void loadBalancer() throws InterruptedException, KeeperException
	{
		byte[] bs = null;
//		  List<String> list = new ArrayList<>();
		  int avg;
		  
		  System.out.println("INside load -----------");
		 HashMap<String,List<String>> Map=new HashMap<String,List<String>>();  
		try {
			List<String> Regions = Node.zoo.getChildren(ZookeeperConstants.LEADER_ELECTION_ROOT_NODE,false);
			for (int i = 0; i < Regions.size(); i++) {
				List<String> list = new ArrayList<>();
				String RS=Regions.get(i);
				RS="/"+RS;
				bs=zoo.getData(ZookeeperConstants.LEADER_ELECTION_ROOT_NODE+RS,false,null);
				String str = new String(bs);
				list=getTablesForRegion(str);
				Map.put(str,list);
				
				
			}
			
			System.out.println("Final Map "+Map);
		} catch (KeeperException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		
		avg=getAvg(Map);
		System.out.println("Average "+avg);
		
		 List<String> lessThanAvg = new ArrayList<>();///to put all lesser than avg in this queue,and assing greter than avg to this queue
		 
		
		for(Entry<String, List<String>> entry : Map.entrySet()){
			if(entry.getValue().size()<avg)
			{
				lessThanAvg.add(entry.getKey());
				
			}
			
		}
		String first_q;
		for(Entry<String, List<String>> entry : Map.entrySet()){
			
			if (entry.getValue().size() > avg) {
				int to_assign = entry.getValue().size() - avg;
				while (to_assign > 0) {
					System.out.println("Before MAP : "+Map);

					String tble = entry.getValue().get(0);
					tble = tble.substring(1);
					
					String ipPort = entry.getKey();

					for (int i = 0; i < lessThanAvg.size(); i++) {
						first_q = lessThanAvg.get(i);
						if (Map.get(first_q).size() <= avg) {
							
							entry.getValue().remove(0);
							unLoadRegion(ipPort, tble);

							setNoTriggerFlag(false);
							/** point of concern if RS fails here **/
							zoo.delete(ZookeeperConstants.HBASE_META + "/" + tble, -1);
							loadRegion(first_q, tble);
							
							Map.get(first_q).add("/"+tble);
							setNoTriggerFlag(true);
							System.out.println("after MAP : "+Map);
							break;
						}

					}

					to_assign--;
				}
				
			}
		}
		
	}
	
	
	private void runLoadBalancer()
	{
		
		 new Thread(new Runnable() {
             @Override
             public void run() {
            	 while(true)
         		{
            		 System.out.println("Load Balancer called");
            		 
            		 try {
         				Thread.sleep(ZookeeperConstants.LOAD_BALANCE_DURATION);
         			} catch (InterruptedException e) {
         				// TODO Auto-generated catch block
         				e.printStackTrace();
         			}
         			
         			
         			
         			try {
         				loadBalancer();
         			} catch (InterruptedException | KeeperException e) {
         				// TODO Auto-generated catch block
         				e.printStackTrace();
         			}
         		}
             }
		
		
	}).start();
	}
		
		
}
	

	

	
	
	