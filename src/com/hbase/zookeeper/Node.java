package com.hbase.zookeeper;

import java.io.IOException;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.zookeeper.CreateMode;
import org.apache.zookeeper.KeeperException;
import org.apache.zookeeper.WatchedEvent;
import org.apache.zookeeper.Watcher;
import org.apache.zookeeper.Watcher.Event.EventType;
import org.apache.zookeeper.ZooDefs;
import org.apache.zookeeper.ZooKeeper;
import org.apache.zookeeper.data.Stat;

public class Node implements Runnable {

	private final int id;
	private String processNodePath;
	private String watchedNodePath;
	public String LEADER_ELECTION_ROOT_NODE = "/";
	public ZooKeeper zoo;
	public int leader = 0; // 0=slave, 1= leader
	String ip;
	String port;
	public Set<String> followers = new HashSet<String>();

	public Node(final int id, final String zkURL,final String ip_,final String port_) throws IOException {
		this.id = id;
		ip=ip_;
		port=port_;
		zoo = new ZooKeeper(zkURL, 5000, new ProcessNodeWatcher());
	}

	public void run() {
		System.out.println("Process with id: " + id + " has started!");
		try {
			processNodePath = zoo.create("hbase/regions/<"+ip+":"+port+">", "somedata".getBytes(),
					ZooDefs.Ids.OPEN_ACL_UNSAFE,
					CreateMode.EPHEMERAL_SEQUENTIAL);
			System.out.println("[Process: " + id
					+ "] Process node created with path: " + processNodePath);
			leaderElection();
		} catch (KeeperException | InterruptedException e) {
			e.printStackTrace();
		}
	}

	public void leaderElection() throws KeeperException, InterruptedException {
		List<String> childNodePaths = zoo.getChildren(LEADER_ELECTION_ROOT_NODE, false);
		Collections.sort(childNodePaths);
		
		System.out.println("I am the znode with path :" + processNodePath);
		int index = childNodePaths.indexOf(processNodePath.substring(processNodePath.lastIndexOf("/") + 1));
		
		if (index == 0) {
			System.out.println("I am leader");
			leader = 1;
			
			/*Creating follower list*/
			for (int i = 0; childNodePaths.size() > i; i++)
				followers.add(childNodePaths.get(i));
			
			/*Master putting watch on root node*/
			System.out.println("Setting watch on ROOT_NODE ");
			zoo.getChildren(LEADER_ELECTION_ROOT_NODE, true);
		} else {
			System.out.println("The leader is "+childNodePaths.get(0));
			System.out.println("I am follower");
			leader = 0;			
			for (int i = 0; childNodePaths.size()-1 > i; i++)
			{
			/*Follower putting watch on all smallest node*/
				
			watchedNodePath = "/" + childNodePaths.get(i);
			if(i==index)
			{
				i++;
			continue;
			}
			
			zoo.exists(watchedNodePath, true);
			}
		}
	}

	public class ProcessNodeWatcher implements Watcher {

		public void process(WatchedEvent event) {
			System.out.println("Event received: " + event);
			 
			if (leader == 0) 
			{
				if (EventType.NodeDeleted.equals(event.getType())) {
					try {
						leaderElection();
					} catch (KeeperException e) {
						e.printStackTrace();
					} catch (InterruptedException e) {
						e.printStackTrace();
					}
				}
			} 
			else if (leader == 1)
			{
				/*if leader receive event of "NodeChildrenChanges" than he has to figure out 
				 * which added and deleted. For that it uses his copy of list of active nodes with 
				 * new set of active nodes on zookeeper*/
				if (EventType.NodeChildrenChanged.equals(event.getType())) 
				{
					try {
						List<String> childNodeList = zoo.getChildren(LEADER_ELECTION_ROOT_NODE, false);
						
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
								followers.remove(node_child);
							}
						}
						
						followers.clear();
						childNodeList = zoo.getChildren(
								LEADER_ELECTION_ROOT_NODE, false);
						for(int i=0;i<childNodeList.size();i++)
							followers.add(childNodeList.get(i));

					} catch (KeeperException | InterruptedException e) {
						e.printStackTrace();
					}
					
					try {
						
						zoo.getChildren("/", true);
					} catch (KeeperException | InterruptedException e) {
						
						e.printStackTrace();
					}


				}
			

			}
		}
	}

	public class slavewatcher implements Watcher {

		@Override
		public void process(WatchedEvent event) {
			final EventType eventType = event.getType();

			if (EventType.NodeDeleted.equals(eventType)) {
				System.out.println(event.getPath() + "deleted");
			}
			if (EventType.NodeCreated.equals(eventType)) {
				System.out.println(event.getPath() + "created");
			}

		}

	}
}