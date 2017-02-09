package com.hdfs.miscl;

import java.nio.charset.StandardCharsets;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.util.List;

import com.google.protobuf.InvalidProtocolBufferException;
import com.hdfs.datanode.IDataNode;
import com.hdfs.miscl.Hdfs.BlockLocationRequest;
import com.hdfs.miscl.Hdfs.BlockLocationResponse;
import com.hdfs.miscl.Hdfs.BlockLocations;
import com.hdfs.miscl.Hdfs.DataNodeLocation;
import com.hdfs.miscl.Hdfs.OpenFileRequest;
import com.hdfs.miscl.Hdfs.OpenFileResponse;
import com.hdfs.miscl.Hdfs.ReadBlockRequest;
import com.hdfs.miscl.Hdfs.ReadBlockResponse;
import com.hdfs.namenode.INameNode;

public class GetFile implements Runnable{

	
	String fileName;
	String localOutFile;
	int status;
	
	
	public GetFile(String fileName,String localOut) {
		// TODO Auto-generated constructor stub
		
		this.fileName = fileName;
		this.localOutFile = localOut;
	}
	
	 public int getStatus() {
		return status;
	}
	
	@Override
	public void run() {
		// TODO Auto-generated method stub
		
		this.status=Constants.STATUS_SUCCESS;
		OpenFileRequest.Builder openFileReqObj = OpenFileRequest.newBuilder();
		openFileReqObj.setFileName(fileName);		
		openFileReqObj.setForRead(true);
		
		
		FileWriterClass fileWriteObj = new FileWriterClass(localOutFile);
		fileWriteObj.createFile();
		
		byte[] responseArray;
		
		try {
			//here obtain the IP, port of the namenode, so that we can register to the services 
			//provided by the namenode
			Registry registry=LocateRegistry.getRegistry(Constants.NAME_NODE_IP,Registry.REGISTRY_PORT);
			INameNode nameStub;
			nameStub=(INameNode) registry.lookup(Constants.NAME_NODE);
			responseArray = nameStub.openFile(openFileReqObj.build().toByteArray());
			
			try {
				//Get is the read functionality from the HDFS file system
				OpenFileResponse responseObj = OpenFileResponse.parseFrom(responseArray);
				if(responseObj.getStatus()==Constants.STATUS_NOT_FOUND)
				{
					System.out.println("File not found fatal error");
					System.exit(0);
				}
				
				//receives all the block numbers associated with a given file
				List<String> blockNums = responseObj.getBlockNumsList();
				BlockLocationRequest.Builder blockLocReqObj = BlockLocationRequest.newBuilder();
				
				/**Now, all blocks related to that file is present with us  
				 * in blockNums
				 * perform Read Block Request  from all the blockNums**/
				
				blockLocReqObj.addAllBlockNums(blockNums);
												
				try {
					responseArray = nameStub.getBlockLocations(blockLocReqObj.build().toByteArray());
				} catch (RemoteException e1) {
					// TODO Auto-generated catch block
					e1.printStackTrace();
				}
				
				
				
				BlockLocationResponse blockLocResObj = BlockLocationResponse.parseFrom(responseArray);
				
				/**block location response returns all the block locations of each of the 
				 * blocks that were sent by blockNums
				 */
				
				if(blockLocResObj.getStatus()==Constants.STATUS_FAILED)
				{
					System.out.println("Fatal error!");
					System.exit(0);
				}
				
				/** We might have many blocks per file, so we get the datanode locations of each
				 * block as a response to the request nameStub.getBlockLocations
				 */
				List<BlockLocations> blockLocations =  blockLocResObj.getBlockLocationsList();
				
				
				for(int i=0;i<blockLocations.size();i++)
				{
					BlockLocations thisBlock = blockLocations.get(i);
					
					String blockNumber = thisBlock.getBlockNumber();					
					List<DataNodeLocation> dataNodes = thisBlock.getLocationsList();
					
					if(dataNodes==null || dataNodes.size()==0)
					{
						System.out.println("All nodes are down :( ");
						System.exit(0);
					}
					
					int dataNodeCounter=0;
					
					DataNodeLocation thisDataNode = null;//dataNodes.get(dataNodeCounter);					
					String ip;// = thisDataNode.getIp();
					int port ; //= thisDataNode.getPort();
					
					
					IDataNode dataStub=null;
					
					boolean gotDataNodeFlag=false;
					
					/**
					 * The following do while loop tries to retrieve the data block that we are looking
					 * for, in case it is not present in one of the data locations that we first queried for.
					 * it goes to check with another data node that was sent in the response,
					 * this continues until we find the block or all data node locations exhaust
					 */
					do
					{
						try
						{
							thisDataNode = dataNodes.get(dataNodeCounter);
							ip = thisDataNode.getIp();
							port = thisDataNode.getPort();
														
							Registry registry2=LocateRegistry.getRegistry(ip,port);					
							dataStub = (IDataNode) registry2.lookup(Constants.DATA_NODE_ID);
							gotDataNodeFlag=true;
						}
						catch (RemoteException e) {
							
							gotDataNodeFlag=false;
//							System.out.println("Remote Exception");
							dataNodeCounter++;
						} 
					}					
					while(gotDataNodeFlag==false && dataNodeCounter<dataNodes.size());
					
					/**This is an indication to say that even after checking all the datanodes
					 * for that particular block we couldn't get the block since all the nodes were down
					 * so we exit ( we may discuss and change it)
					 */
					if(dataNodeCounter == dataNodes.size())
					{
						System.out.println("All data nodes are down :( ");
						System.exit(0);
					}
					

					/**Construct Read block request **/
					ReadBlockRequest.Builder readBlockReqObj = ReadBlockRequest.newBuilder();
					readBlockReqObj.setBlockNumber(blockNumber);
					
					/**Read block request call **/
					
					responseArray = dataStub.readBlock(readBlockReqObj.build().toByteArray());
					ReadBlockResponse readBlockResObj = ReadBlockResponse.parseFrom(responseArray);
					
					if(readBlockResObj.getStatus()==Constants.STATUS_FAILED)
					{
						System.out.println("In method openFileGet(), readError");
						System.exit(0);
					}
					
//					responseArray = readBlockResObj.getData(0).toByteArray();	
					
					byte [] dataOut = new byte[readBlockResObj.getDataList().size()];
					
					for(int k=0;k<readBlockResObj.getDataList().size();k++)
					{
						readBlockResObj.getDataList().get(k).copyTo(dataOut,k);
					}
					
					
					String str = new String(dataOut);			
					System.out.print(str);
					//fileWriteObj.writeonly(str);
					fileWriteObj.writeBytes(dataOut);

				}
				
				
			} catch (InvalidProtocolBufferException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		} catch (NotBoundException e) {
			System.out.println("Exception caught: NotBoundException ");			
		} catch (RemoteException e2) {
			// TODO Auto-generated catch block
			e2.printStackTrace();
		}
		fileWriteObj.closeFile();
		
		
	}

}
