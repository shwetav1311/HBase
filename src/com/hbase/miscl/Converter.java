package com.hbase.miscl;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;


import com.hbase.miscl.HBase.Column;

public class Converter {

	public static void main(String[] args) {
		// TODO Auto-generated method stub
		Integer nBytes = 0;
		
		FileOutputStream stream= null;
		try {
			stream = new FileOutputStream("sampleFile");
		} catch (FileNotFoundException e2) {
			// TODO Auto-generated catch block
			e2.printStackTrace();
		}
		
		for(int i=0;i<3;i++)
		{
			Column.Builder colBuilder = Column.newBuilder();
			colBuilder.setColName("Sheshadri"+i);
			colBuilder.setColType("int"+i);
			colBuilder.setColValue("21"+i);
			
			byte[] bytes = colBuilder.build().toByteArray();
			
			nBytes = bytes.length;
			
			System.out.println("The size of the file is "+nBytes);
			String hexNumber = Integer.toHexString(nBytes);
			
			
			if(hexNumber.length()<8)
			{
				int prefix = 8 - hexNumber.length();
				String temp = "";
				while(prefix>0)
				{
					temp = temp + "0";
					prefix--;
				}			
				hexNumber = temp + hexNumber;
			}
			
			System.out.println("The hex number is "+hexNumber);
			
					
			
			try {				
				stream.write(hexNumber.getBytes(Charset.forName("UTF-8")));
				stream.write(bytes);					
			}
			catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		
		try {
			stream.close();
		} catch (IOException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
		
		
		
		
		try {
			
			 @SuppressWarnings("resource")
			FileInputStream inStream = new FileInputStream("sampleFile");

				while(true)
				{
					byte[] lengthOfRow = new byte[8];
					int flag = inStream.read(lengthOfRow, 0, 8);
					
					if(flag==-1)
					{
						System.out.println("File reading complete");
						break;
					}
					
					String str = new String(lengthOfRow, StandardCharsets.UTF_8);
					System.out.println("The length we are looking for is "+str);
					int length = hexToDec(str);
					System.out.println("The length of bytes we need to read is "+ length);
					
					byte[] data = new byte[length];					
					
					inStream.read(data, 0, length);
					
					Column colObj = Column.parseFrom(data);
					System.out.println("After retrieving "+colObj.getColName()+" "+colObj.getColType()+" "+colObj.getColValue());
					
				}//end of while loop
				
				inStream.close();
				
			}
		
			catch (FileNotFoundException e){
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		
			catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}			
					
	}
	
	public static int hexToDec(String str)
	{
		char[] array = str.toCharArray();
		double answer = 0;
		int k=0;		
		for(int i=7;i>=0;i--)
		{
			char ch = array[i];
			int digit = ch - '0';
			
			switch(ch)
			{
				case 'a': digit = 10;
							break;
				case 'b': digit = 11;
							break;
				case 'c': digit = 12;
							break;
				case 'd': digit = 13;
							break;
				case 'e': digit = 14;
							break;
				case 'f': digit = 15;
							break;
							
				default : break;
							
			}
			
			answer = answer + (digit* Math.pow(16, k));
			k++;
		}
		return (int)answer;
	}

}
