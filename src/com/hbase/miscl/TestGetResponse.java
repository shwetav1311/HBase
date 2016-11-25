package com.hbase.miscl;

public class TestGetResponse implements Runnable{

	@Override
	public void run() {
		// TODO Auto-generated method stub
		int cnt=0;
		try {
			while(true)
			{
				Thread.sleep(1000);
				System.out.println("The number of get responses after 1s are "+ TestMaster.updateGetCounter(0));
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
