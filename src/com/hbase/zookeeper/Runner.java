package com.hbase.zookeeper;

import java.io.IOException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import org.apache.zookeeper.KeeperException;

import com.hbase.miscl.HBaseConstants;


/*
 * Start the thread for creating znodes
 *  */

public class Runner {
	@SuppressWarnings("static-access")
	public static void start (String ip,String port,int id_) throws IOException, InterruptedException, KeeperException {
		final int id = id_;
		final String zkURL = HBaseConstants.ZOOKEEPER_IP;
		final ExecutorService service = Executors.newSingleThreadExecutor();
		
		Node node = new Node(id, zkURL, ip, port);
		
		final Future<?> status = service.submit(node);
		new Thread().sleep(1000);
				
		try {
			status.get();
		} catch (InterruptedException | ExecutionException e) {
			service.shutdown();
		}
	}
}
