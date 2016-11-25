package com.hbase.miscl;

import java.util.Vector;

public class TestPutResponse implements Runnable{

	@Override
	public void run() {
		// TODO Auto-generated method stub
		int cnt=0;
		try {
			while(true)
			{
				Thread.sleep(1000);
				Vector<Integer> throughputVec = TestMaster.updatePutCounter(0, 0);
				System.out.println("PReq: "+ throughputVec.get(0)+ " Data: "+throughputVec.get(1));
				cnt++;
//				if(cnt==20)
//					break;
				
			}
			
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

}
