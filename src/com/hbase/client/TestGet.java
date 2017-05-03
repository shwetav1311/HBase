package com.hbase.client;

public class TestGet {
	
	public static void main(String[] args) {
		
    
		Integer clients = 4;
		
		for(int i=0;i<clients;i++)
		{
			ClientDriverTestGet testGet = new ClientDriverTestGet(i);
			testGet.start();
		}
				
	}

}
