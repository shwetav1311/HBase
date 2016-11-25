package com.hbase.miscl;

import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Map.Entry;

import com.google.protobuf.InvalidProtocolBufferException;
import com.hbase.miscl.HBase.Cell;
import com.hbase.miscl.HBase.Column;
import com.hbase.miscl.HBase.ColumnFamily;
import com.hbase.miscl.HBase.GetRequest;
import com.hbase.miscl.HBase.GetResponse;
import com.hbase.rs.IRegionServer;
import com.hdfs.miscl.Constants;

public class TestHBaseGet {

	static IRegionServer rsStub;
	String tableName;
	String[] args;
	
	@SuppressWarnings("static-access")
	public TestHBaseGet(IRegionServer rsStub,String tableName,String[] args) {
		// TODO Auto-generated constructor stub
		this.rsStub = rsStub;
		this.tableName = tableName;
		this.args = args;
	}
	
	
	public void callGetMethod()
	{
		getTable(tableName, args);
	}
	
	
	public static void getTable(String tableName,String[] args)
	{
		GetRequest.Builder getRequest = GetRequest.newBuilder();
		getRequest.setTableName(tableName);
		getRequest.setRowkey(args[2]);
		
		/* club all column families together */
		HashMap<String, ArrayList<Column>> map = new HashMap<>();
		
		
		
		for(int i=3;i<args.length;i++)
		{
			String[] col = args[i].split(":");
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
		
		
		byte[] res;
		try {
			res = rsStub.get(getRequest.build().toByteArray());
			GetResponse getResponse = GetResponse.parseFrom(res);
			
			if(getResponse.getStatus()==Constants.STATUS_SUCCESS)
			{
//				System.out.print("GET KEY SUCCESS");
//				System.out.println(getResponse.getColFamilyCount());
				if(getResponse.getColFamilyCount()==0)
				{
//					System.out.println("GET: KEY NOT FOUND");
//					System.exit(0);
				}
				TestMaster.updateGetCounter(1,getResponse.toByteArray().length);
				
//				System.out.print(getResponse.getColFamily(0));
			}else
			{
				System.out.print("GET KEY FAILED");
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
