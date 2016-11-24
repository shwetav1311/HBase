package com.hbase.miscl;

public class TestCheckPutResponse implements Runnable{

	@Override
	public void run() {
		// TODO Auto-generated method stub
		try {
			while(true)
			{
				Thread.sleep(2500);
				System.out.println("The number of put responses after 5s are "+ TestPutAndGet.updateCounter(0));
				
			}
			
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

}
