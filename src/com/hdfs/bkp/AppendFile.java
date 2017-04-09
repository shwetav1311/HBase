package com.hdfs.bkp;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.util.ArrayList;
import java.util.List;

import com.google.protobuf.ByteString;
import com.google.protobuf.InvalidProtocolBufferException;
import com.hdfs.datanode.IDataNode;
import com.hdfs.miscl.HDFSConstants;
import com.hdfs.miscl.Hdfs.AssignBlockRequest;
import com.hdfs.miscl.Hdfs.AssignBlockResponse;
import com.hdfs.miscl.Hdfs.BlockLocationRequest;
import com.hdfs.miscl.Hdfs.BlockLocationResponse;
import com.hdfs.miscl.Hdfs.BlockLocations;
import com.hdfs.miscl.Hdfs.CloseFileRequest;
import com.hdfs.miscl.Hdfs.DataNodeLocation;
import com.hdfs.miscl.Hdfs.OpenFileRequest;
import com.hdfs.miscl.Hdfs.OpenFileResponse;
import com.hdfs.miscl.Hdfs.WriteBlockRequest;
import com.hdfs.miscl.Hdfs.WriteBlockResponse;
import com.hdfs.namenode.INameNode;

/** @author sheshadri - Chaman - You have to implement this 
 *	Done waste body -_-  
 *
 */
public class AppendFile implements Runnable {
	
	public static String fileName;
	
	public AppendFile(String fileNameArgs) {
		
		fileName = fileNameArgs;
	}

	@Override
	public void run() {
		// TODO Auto-generated method stub
		appendTesting();
	}
	
	public static void appendTesting()
	{
		OpenFileRequest.Builder openFileReqObj = OpenFileRequest.newBuilder();
		openFileReqObj.setFileName(fileName);
		openFileReqObj.setForRead(false);
		openFileReqObj.setIsAppend(true);
		int fileHandle = 0;
		
		try {
			Registry registry = LocateRegistry.getRegistry(HDFSConstants.NAME_NODE_IP,Registry.REGISTRY_PORT);
			INameNode nameStub;
			
			nameStub = (INameNode) registry.lookup(HDFSConstants.NAME_NODE);
			byte[] responseArray = nameStub.openFile(openFileReqObj.build().toByteArray());
			
			/**The response Array will contain the FileHandle status and the block numbers **/
			
			OpenFileResponse responseObj = OpenFileResponse.parseFrom(responseArray);
			fileHandle = responseObj.getHandle();
//			System.out.println("The file handle is "+fileHandle);
			
			if(responseObj.getStatus()==HDFSConstants.STATUS_NOT_FOUND||responseObj.getStatus()==HDFSConstants.STATUS_FAILED)
			{
				System.out.println("fatal error");
				System.exit(0);
			}
			
//			List<String> blockNums = responseObj.getBlockNumsList();
//			String last_blocknum=blockNums.get(0);
//			String newBlockNum = responseObj.getNewBlockNum();		
			
			int size=(int) responseObj.getSize();
			System.out.println("size of the file is "+size);
			/**remaining size **/
			int remainSize=(HDFSConstants.BLOCK_SIZE)-size;//1,000,000 - 841
			
			System.out.println("The remaining size  is "+remainSize);
		    BufferedReader breader = null;
		    breader = new BufferedReader(new FileReader("test.txt") );
		    File myFile = new File("test.txt");
		    int bytesToRead = 0;
		    if(myFile.exists())
		    {
		      bytesToRead = (int)myFile.length();
		    }
		    
		    /*Handles all cases */
		    sendFileAsAppendCaseThree(responseObj,remainSize,bytesToRead);
		    
		    breader.close();
		      
			
			
		} catch (RemoteException | NotBoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (InvalidProtocolBufferException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		
	}
	
	
	/**
	 * bytes to read are more than the available free space in the last block
	 * so new append blocks have to be sent
	 * 1. append last block
	 * 2. ask for new assign blocks
	 */
	private static void sendFileAsAppendCaseThree(OpenFileResponse resObj,int remainSize,int totalBytes) {
		// TODO Auto-generated method stub
		/**
		 * 1. prepare a write block request, for that get block locations 
		 */
		
		
		byte[] firstData;

		/**if totalbytes is less than remaining then we only need to append
		 * if totalbytes is greater then append and and create new block
		 */
		INameNode nameStub=null;
		
		boolean isException = false;
		
		if(totalBytes<remainSize)
		{
			firstData = new byte[totalBytes];
			remainSize=totalBytes;
		}else
		{
			firstData = new byte[remainSize];
		}

		
		try
		{
			
			InputStream in = new FileInputStream("test.txt");
			Registry registry = LocateRegistry.getRegistry(HDFSConstants.NAME_NODE_IP,Registry.REGISTRY_PORT);
			nameStub = (INameNode) registry.lookup(HDFSConstants.NAME_NODE);

			WriteBlockRequest.Builder writeReqObj = WriteBlockRequest.newBuilder();


			//block locations object need to send request on the new block
			BlockLocationRequest.Builder blockReqObj = BlockLocationRequest.newBuilder();
			List<String> myBlocks = new ArrayList<String>();
			myBlocks.add(resObj.getBlockNums(0));
			blockReqObj.addAllBlockNums(myBlocks);

			byte[] responseArray = nameStub.getBlockLocations(blockReqObj.build().toByteArray());

			BlockLocationResponse myResponse = BlockLocationResponse.parseFrom(responseArray);
			if(myResponse.getStatus()==HDFSConstants.STATUS_FAILED)
			{
				System.out.print("block response not got bye bye");
				System.exit(0);				
			}

			BlockLocations lastBlock = myResponse.getBlockLocations(0);

			//read data of size remain size

			in.read(firstData, 0, remainSize);

			writeReqObj.setBlockInfo(lastBlock); // added the details of last blocks
			writeReqObj.setIsAppend(true);
			writeReqObj.setNewBlockNum(resObj.getNewBlockNum());
			writeReqObj.setCount(0);
			writeReqObj.addData(ByteString.copyFrom(firstData));// initial set of data is sent


			List<BlockLocations> blockLocations =  myResponse.getBlockLocationsList();				
			BlockLocations thisBlock = blockLocations.get(0); //get the location of the block that we are about to append			
			List<DataNodeLocation> dataNodes = thisBlock.getLocationsList();//location of all data nodes that contain this block

			int dataNodeCounter=0;			
			DataNodeLocation thisDataNode = null;				
			String ip;
			int port ; 
			
			IDataNode dataStub=null;


			thisDataNode = dataNodes.get(dataNodeCounter);
			ip = thisDataNode.getIp();
			port = thisDataNode.getPort();

			Registry registry2=LocateRegistry.getRegistry(ip,port);					
			dataStub = (IDataNode) registry2.lookup(HDFSConstants.DATA_NODE_ID);

			byte[] writeReqResponse = dataStub.writeBlock(writeReqObj.build().toByteArray());
			WriteBlockResponse writeBlkRes = WriteBlockResponse.parseFrom(writeReqResponse);
			if(writeBlkRes.getStatus()!=HDFSConstants.STATUS_FAILED)
			{
				if(writeBlkRes.getCount()>=2)
				{
					System.out.println("Okay! first phase of append done, now its normal writes");
				}
				else
				{
					System.out.println("Aborting due to lack of votes");
					CloseFileRequest.Builder closeFileObj = CloseFileRequest.newBuilder();
					closeFileObj.setDecision(0);//abort
					closeFileObj.setHandle(resObj.getHandle()); //handle
					nameStub.closeFile(closeFileObj.build().toByteArray());
				}
			}


			int amountBytesRemaining = totalBytes - remainSize;

			boolean success = true;
			
			if(amountBytesRemaining>0)
			{
				success = sendRemainingBytesAppend(resObj,amountBytesRemaining,nameStub,in);
			}

			in.close();//close the file
			
			

			CloseFileRequest.Builder closeFileObj = CloseFileRequest.newBuilder();
			if(success)
				closeFileObj.setDecision(1);//commit
			else
				closeFileObj.setDecision(0);//abort
				
			
			closeFileObj.setHandle(resObj.getHandle()); //handle
			nameStub.closeFile(closeFileObj.build().toByteArray());
		}
		catch(Exception e)
		{
			e.printStackTrace();
			System.out.println("Whats up exception caught!");
			isException=true;
		}
		
		if(isException)
		{
			CloseFileRequest.Builder closeFileObj = CloseFileRequest.newBuilder();
			closeFileObj.setDecision(0);//abort
			closeFileObj.setHandle(resObj.getHandle()); //handle
			try {
				nameStub.closeFile(closeFileObj.build().toByteArray());
			} catch (RemoteException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
				
				
	}
	
	/**method to send remaining bytes as one by one in blocks
	 * @return **/
	private static boolean sendRemainingBytesAppend(OpenFileResponse resObj, int amountBytesRemaining, INameNode nameStub,
			InputStream in) {
		// TODO Auto-generated method stub
	
		System.out.println("Sending new block");
		
		while(amountBytesRemaining>0)
		{
			AssignBlockRequest.Builder assgnBlk = AssignBlockRequest.newBuilder();
			assgnBlk.setHandle(resObj.getHandle());
			assgnBlk.setIsAppend(true); 
			
			byte[] assignResponse;
			try {
				
				assignResponse = nameStub.assignBlock(assgnBlk.build().toByteArray());
				AssignBlockResponse assgnResponse = AssignBlockResponse.parseFrom(assignResponse);	
				
				if(assgnResponse.getStatus()!=HDFSConstants.STATUS_FAILED)
				{
					/** we get the new block number **/
					System.out.println("new block returned from NN");
				}
				
				BlockLocations thisBlock =  assgnResponse.getNewBlock();			
				 //get the location of the block that we are about to append			
				List<DataNodeLocation> dataNodes = thisBlock.getLocationsList();//location of all data nodes that contain this block
				
				int dataNodeCounter=0;			
				DataNodeLocation thisDataNode = null;				
				String ip;
				int port ; 
				
				IDataNode dataStub=null;
				
				thisDataNode = dataNodes.get(dataNodeCounter);
				ip = thisDataNode.getIp();
				port = thisDataNode.getPort();
											
				Registry registry2=LocateRegistry.getRegistry(ip,port);					
				dataStub = (IDataNode) registry2.lookup(HDFSConstants.DATA_NODE_ID);
				
				
				int sendBytes = 0;
				if(amountBytesRemaining<HDFSConstants.BLOCK_SIZE)
				{
					sendBytes = amountBytesRemaining;
					amountBytesRemaining = 0;
				}
				else
				{
					sendBytes = HDFSConstants.BLOCK_SIZE;
					amountBytesRemaining = amountBytesRemaining - HDFSConstants.BLOCK_SIZE;
				}
				
				byte[] data = new byte[sendBytes];
				
				in.read(data, 0, sendBytes);
				
				WriteBlockRequest.Builder writeBlkReq = WriteBlockRequest.newBuilder();
				writeBlkReq.setCount(0);
				writeBlkReq.setIsAppend(true);
				writeBlkReq.setNewBlockNum("-1");
				writeBlkReq.setBlockInfo(thisBlock);
				writeBlkReq.addData(ByteString.copyFrom(data));
				
				byte[] writeReqResponse = dataStub.writeBlock(writeBlkReq.build().toByteArray());
				WriteBlockResponse writeBlkRes = WriteBlockResponse.parseFrom(writeReqResponse);
				
				if(writeBlkRes.getStatus()!=HDFSConstants.STATUS_FAILED)
				{
					if(writeBlkRes.getCount()>=2)
					{
						System.out.println("Second Phase New Block Success");
					}
					else
					{
						return false;
					}
				}
				
				
			//	skip = skip + sendBytes;
			} catch (IOException | NotBoundException e) {
				// TODO Auto-generated catch block
				System.out.println("exception in send remaining bytes");
				e.printStackTrace();
				return false;
			} 
		}
		
		return true;
		
	}
	
	
}
