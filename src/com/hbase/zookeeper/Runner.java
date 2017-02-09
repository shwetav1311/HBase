package com.hbase.zookeeper;

import java.io.IOException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import org.apache.zookeeper.KeeperException;

public class Runner {
	public static void start (String ip,String port,int id_) throws IOException, InterruptedException, KeeperException {
		 final int id = id_;
		final String zkURL = "localhost";
		final ExecutorService service = Executors.newSingleThreadExecutor();
		final Future<?> status = service.submit(new Node(id, zkURL,ip,port));
		
		try {
			status.get();
		} catch (InterruptedException | ExecutionException e) {
			service.shutdown();
		}
	}
}
