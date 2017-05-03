package com.hbase.client;

public class TestPut {
	
	public static void main(String[] args) {
		
    
		Integer clients = 4;
		
		for(int i=0;i<clients;i++)
		{
			ClientDriverTestPut testPut = new ClientDriverTestPut(i);
			testPut.start();
		}
		
	}

}
