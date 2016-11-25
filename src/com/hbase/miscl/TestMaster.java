package com.hbase.miscl;

import java.util.Vector;

public class TestMaster {

	public static Integer putResponseCounter = 0;
	public static Integer putBytes = 0;
	public static Integer getResponseCounter = 0;
	public static Integer gettBytes = 0;
	
	public static void main(String[] args)
	{
		int noOfClients = 10;
		int choice = 0; //put or get
		int noOfOps = 10000;
		
		for(int i=0;i<noOfClients;i++)
		{
			Thread myThread = new Thread( new TestPutGet(i, noOfOps, choice));
			myThread.start();
		}
	}
	
	
	public static synchronized Vector<Integer> updatePutCounter(Integer incr,Integer bytesWritten)
	{
		int val=putResponseCounter;
		int size = putBytes;
		Vector<Integer> myVector = new Vector<>();
		
		if(incr!=0)
		{
			putResponseCounter = putResponseCounter + incr;			
			val=putResponseCounter;
			size = size + putBytes;			
		}
		else
		{
			val=putResponseCounter;
			size = putBytes;
			putResponseCounter = 0;
			putBytes = 0;
		}
		
		
		myVector.addElement(val);
		myVector.addElement(size);
		
		return myVector;
	}
	
	/** Get response Counter, increments on every get and resets by a thread **/
	public static synchronized Integer updateGetCounter(Integer incr)
	{
		int val=getResponseCounter;
		
		if(incr!=0)
		{
			getResponseCounter = getResponseCounter + incr;
			val=getResponseCounter;
			
		}
		else
		{
			val=getResponseCounter;
			getResponseCounter = 0;
		}
				
		return val;
	}

	
}
