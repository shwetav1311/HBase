package com.hbase.zookeeper;

import java.io.IOException;

import org.apache.zookeeper.KeeperException;




public class RSDriver {
	public static String ip;
	public static String port;
	public static int id;

	public static void main(String[] args) throws IOException, InterruptedException, KeeperException {
		
		
		ip="127.0.0.1";
		port="10001";
		id=1;
		
		Runner.start(ip,port,id);
		
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
	
	
	
}
