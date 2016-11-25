package com.hbase.miscl;

import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Map.Entry;
import java.util.Vector;

import com.google.protobuf.InvalidProtocolBufferException;
import com.hbase.miscl.HBase.Cell;
import com.hbase.miscl.HBase.Column;
import com.hbase.miscl.HBase.ColumnFamily;
import com.hbase.miscl.HBase.PutRequest;
import com.hbase.miscl.HBase.PutResponse;
import com.hbase.rs.IRegionServer;
import com.hbase.rs.PutRow;
import com.hdfs.miscl.Constants;

public class TestHBasePutThread implements Runnable{

	static IRegionServer rsStub;
	String tableName;
	String[] args;
	
	
	@SuppressWarnings("static-access")
	public TestHBasePutThread(IRegionServer rsStub,String tableName,String args[]) {
		// TODO Auto-generated constructor stub
		this.rsStub = rsStub;
		this.tableName = tableName;
		this.args = args;		
	}
	
	
	@Override
	public void run() {
		// TODO Auto-generated method stub
		putTable(tableName, args);
	}
	
	public static void putTable(String tableName,String[] args)
	{
		PutRequest.Builder putRequest = PutRequest.newBuilder();
		putRequest.setTableName(tableName);
		putRequest.setRowkey(args[2]);
		
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
		
		byte[] res;
		try {
			res = rsStub.put(putRequest.build().toByteArray());
			PutResponse putResponse = PutResponse.parseFrom(res);
			if (putResponse.getStatus() == Constants.STATUS_SUCCESS)
			{
//				System.out.println("Success");
				//increment the put response count to meausre the throughput
				TestPutAndGet.updatePutCounter(1);
				
			}else
			{
				System.out.println("Failed");
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
