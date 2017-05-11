package com.hbase.zookeeper;

public final class ZookeeperConstants {


	public static final String  NAME_NODE = "NameNode";  //added by shweta
	public static final String  NAME_NODE_IP = "127.0.0.1";  //added by shweta
	public static final String LEADER_ELECTION_ROOT_NODE = "/hbase/region";
	public static final String HBASE_META="/hbase/Meta"; // ephemeral node
	public static final String HBASE = "/hbase";
	public static final String HBASE_MASTER = "/hbase/master";
	public static final String HBASE_WAL = "/hbase/wal"; // persistent node
	public static final Integer LOAD_BALANCE_DURATION = 60000; //1 min 
}