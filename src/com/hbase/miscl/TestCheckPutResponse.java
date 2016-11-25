package com.hbase.miscl;

import java.util.Vector;

public class TestCheckPutResponse implements Runnable{

	@Override
	public void run() {
		// TODO Auto-generated method stub
		int cnt=0;
		try {
			while(true)
			{
				Thread.sleep(1000);
				Vector<Integer> throughputVec = TestPutAndGet.updatePutCounter(0, 0);
				System.out.println("Number of put Responses: "+ throughputVec.get(0)+ " Number of bytes Written are "+throughputVec.get(1));
				cnt++;
				if(cnt==20)
					break;
				
			}
			
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

}
