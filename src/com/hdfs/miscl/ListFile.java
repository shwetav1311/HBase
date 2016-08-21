package com.hdfs.miscl;

import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.util.ArrayList;
import java.util.List;

import com.google.protobuf.InvalidProtocolBufferException;
import com.hdfs.miscl.Hdfs.ListFilesRequest;
import com.hdfs.miscl.Hdfs.ListFilesResponse;
import com.hdfs.namenode.INameNode;

/**
 * 
 * @author shweta
 *
 */
public class ListFile {

	public String wildCard;
	
	public ListFile(String wild)
	{
		this.wildCard = wild;
	}


	public List<String> list()
	{
		ListFilesRequest.Builder listFileReqObj = ListFilesRequest.newBuilder();
		
		
		listFileReqObj.setDirName(wildCard);
		
		Registry registry;
		try {
			registry = LocateRegistry.getRegistry(Constants.NAME_NODE_IP,Registry.REGISTRY_PORT);
			INameNode nameStub;
			nameStub=(INameNode) registry.lookup(Constants.NAME_NODE);
			
			byte[] responseArray = nameStub.list(listFileReqObj.build().toByteArray());
			try {
				ListFilesResponse listFileResObj = ListFilesResponse.parseFrom(responseArray);
				List<String> fileNames = listFileResObj.getFileNamesList();
				
				return fileNames;
				
				
			} catch (InvalidProtocolBufferException e) {
				 
				System.out.println("InvalidProtocolBufferException caught in callListBlocks: ClientDriverClass");
			}
			
		} catch (RemoteException | NotBoundException e) {
			
			System.out.println("Exception caught in callListBlocks: ClientDriverClass");
			
		}
		
		
		return new ArrayList<String>();
	}
}
