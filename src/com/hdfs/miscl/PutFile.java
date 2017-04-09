package com.hdfs.miscl;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.util.List;

import com.google.protobuf.ByteString;
import com.google.protobuf.InvalidProtocolBufferException;
import com.hdfs.datanode.IDataNode;
import com.hdfs.miscl.Hdfs.AssignBlockRequest;
import com.hdfs.miscl.Hdfs.AssignBlockResponse;
import com.hdfs.miscl.Hdfs.BlockLocations;
import com.hdfs.miscl.Hdfs.CloseFileRequest;
import com.hdfs.miscl.Hdfs.CloseFileResponse;
import com.hdfs.miscl.Hdfs.DataNodeLocation;
import com.hdfs.miscl.Hdfs.OpenFileRequest;
import com.hdfs.miscl.Hdfs.OpenFileResponse;
import com.hdfs.miscl.Hdfs.WriteBlockRequest;
import com.hdfs.namenode.INameNode;

public class PutFile implements Runnable {

	public static String fileName;
	public static long FILESIZE;
	public FileInputStream fis;
	public String threadName;
	private Thread t;
	
	public int status;
	
	public PutFile(String fileNameArgs) {
		super();
		fileName = fileNameArgs;
	}

	@Override
	public void run() {
		// TODO Auto-generated method stub
		openFilePut(); //PUT to HDFS
	}

	public int getPutStatus() {
		
		return status;
	}
	
	private void openFilePut() {
		// TODO Auto-generated method stub
		
		int fileHandle;
		this.status=HDFSConstants.STATUS_SUCCESS;
		byte[] responseArray;
		
		OpenFileRequest.Builder openFileReqObj = OpenFileRequest.newBuilder();
		openFileReqObj.setFileName(fileName);
		openFileReqObj.setForRead(false);		

		
		try 
		{			
			Registry registry=LocateRegistry.getRegistry(HDFSConstants.NAME_NODE_IP,Registry.REGISTRY_PORT);
			INameNode nameStub;
//			int status;
			
				try 
				{
					nameStub = (INameNode) registry.lookup(HDFSConstants.NAME_NODE);
					responseArray = nameStub.openFile(openFileReqObj.build().toByteArray());
					
					/**The response Array will contain the FileHandle status and the block numbers **/
					
					OpenFileResponse responseObj = OpenFileResponse.parseFrom(responseArray);
					
					fileHandle = responseObj.getHandle();
//					System.out.println("The file handle is "+fileHandle);
					
					status = responseObj.getStatus();
					if(status==HDFSConstants.STATUS_FAILED )//status failed change it
					{
						System.out.println("Fatal Error!");
						//System.exit(0);
						return;
					}
					else if(status==HDFSConstants.STATUS_NOT_FOUND)
					{
//						System.out.println("Duplicate File");
						//System.exit(0);
						return;
					}
					
					AssignBlockRequest.Builder assgnBlockReqObj = AssignBlockRequest.newBuilder(); 
					
					
					/**required variables **/

					
					int offset=0;
					
					/**calculate block size **/
					int no_of_blocks=getNumberOfBlocks();					
					try {
						/**open the input stream **/
						fis = new FileInputStream(fileName);
					} catch (FileNotFoundException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}

//					System.out.println("No of blocks are "+no_of_blocks);
					if(no_of_blocks==0)
						no_of_blocks=1;
					
					/**FOR LOOP STARTS HERE **/
					for(int i=0;i<no_of_blocks;i++)
					{
						WriteBlockRequest.Builder writeBlockObj = WriteBlockRequest.newBuilder();
						AssignBlockResponse assignResponseObj ;
						BlockLocations blkLocation ;
						List<DataNodeLocation> dataNodeLocations;
						DataNodeLocation dataNode;
						/**need to call assign block and write blocks **/
						
						assgnBlockReqObj.setHandle(fileHandle);
						
						/**Calling assign block **/
						responseArray = nameStub.assignBlock(assgnBlockReqObj.build().toByteArray());
						
						assignResponseObj = AssignBlockResponse.parseFrom(responseArray);
						
						status = assignResponseObj.getStatus();
						if(status==HDFSConstants.STATUS_FAILED)
						{
							System.out.println("Fatal Error!");
							//System.exit(0);
							return;
						}
						
						blkLocation = assignResponseObj.getNewBlock();
						
						String blockNumber = blkLocation.getBlockNumber();
						System.out.println("Block number retured is "+blockNumber);
						
						dataNodeLocations = blkLocation.getLocationsList();
						
						dataNode = dataNodeLocations.get(0);
//						dataNodeLocations.remove(0);
						
						
						Registry registry2=LocateRegistry.getRegistry(dataNode.getIp(),dataNode.getPort());

						System.out.println(dataNode);
						IDataNode dataStub = (IDataNode) registry2.lookup(HDFSConstants.DATA_NODE_ID);

						/**read 32MB from file, send it as bytes, this fills in the byteArray**/
						
						byte[] byteArray = read32MBfromFile(offset);
						
						offset=offset+(int)HDFSConstants.BLOCK_SIZE;
						
//						System.out.println("---------------------"+byteArray.length);
						
						for(int j=0;j<byteArray.length;j++)
						{
							writeBlockObj.addData(ByteString.copyFrom(byteArray,j,1));
						}
						
						String s = new String(byteArray);
//						System.out.println("*************The new char array is "+s);
						System.out.println("data count"+writeBlockObj.getDataCount());
//						writeBlockObj.addData(ByteString.copyFrom(byteArray));
						
						
						writeBlockObj.setBlockInfo(blkLocation);
						
						dataStub.writeBlock(writeBlockObj.build().toByteArray());
												
					}
					
					CloseFileRequest.Builder closeFileObj = CloseFileRequest.newBuilder();
					closeFileObj.setHandle(fileHandle);
					closeFileObj.setDecision(HDFSConstants.STATUS_NOT_FOUND);
					
					byte[] receivedArray = nameStub.closeFile(closeFileObj.build().toByteArray());
					CloseFileResponse closeResObj = CloseFileResponse.parseFrom(receivedArray);
					if(closeResObj.getStatus()==HDFSConstants.STATUS_FAILED)
					{
						System.out.println("Close File response Status Failed");
//						System.exit(0);
					}
					
					try {
						/**Close the input Stream **/
						fis.close();
					} catch (IOException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
					
				}
				catch (NotBoundException | InvalidProtocolBufferException e) {
					// TODO Auto-generated catch block
					System.out.println("Could not find NameNode");
					e.printStackTrace();
				}
				
			
		}catch (RemoteException e) {
			// TODO Auto-generated catch block
				e.printStackTrace();
		}			
		
		
	}
	

	public static int getNumberOfBlocks()
	{
		File inputFile = new File(fileName);
		if(!inputFile.exists())
		{
			System.out.println("File Does not exist");
//			System.exit(0);
		}
		
		long fileSize = inputFile.length();
		FILESIZE=inputFile.length();
		double noOfBlocks = Math.ceil((double)fileSize*1.0/(double)HDFSConstants.BLOCK_SIZE*1.0);
		
//		System.out.println("The length of the file is "+fileSize+ " Number of blocks are "+(int)noOfBlocks);
		
		return (int)noOfBlocks;
	}
	
	/**Read 32MB size of data from the provided input file **/
	public static byte[] read32MBfromFile(int offset)
	{
		
		System.out.println("offset is "+offset);
		
		FileInputStream breader = null;
		try {
			breader = new FileInputStream(fileName);
		} catch (FileNotFoundException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
	
		int bytesToBeRead = (int)HDFSConstants.BLOCK_SIZE;
		
		int limit =offset+(int)HDFSConstants.BLOCK_SIZE; 
		
		if(limit >= (int) FILESIZE)
		{
			bytesToBeRead = (int)FILESIZE - offset;
		}
		else
		{
			bytesToBeRead = (int)HDFSConstants.BLOCK_SIZE;			
		}
		
		
		
		byte[] newCharArray = new byte[bytesToBeRead];
		
		try {
			breader.skip(offset);
//			breader.read(newCharArray, 0, bytesToBeRead);
			breader.read(newCharArray);
			
			
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}		
		try {
			breader.close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		 String s = new String(newCharArray);
		System.out.println("The new char array is "+s.length());
		return newCharArray;
		
	}
	
	

	
	
	
	
}
