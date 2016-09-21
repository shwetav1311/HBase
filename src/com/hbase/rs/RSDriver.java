package com.hbase.rs;

import java.rmi.AlreadyBoundException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;

import com.hbase.miscl.HBaseConstants;
import com.hdfs.namenode.INameNode;

public class RSDriver implements IRegionServer {
	
	public static void main(String[] args)
	{
		System.out.println("Region server Binding to Registry...");
		bindToRegistry();
	}
	
	/** 
	 * export registry object
	 */
	public static void bindToRegistry()
	{
		System.setProperty("java.rmi.server.hostname",HBaseConstants.RS_DRIVER_IP);
		RSDriver obj = new RSDriver();
		try {
			
			Registry register=LocateRegistry.createRegistry(Registry.REGISTRY_PORT);
			INameNode stub = (INameNode) UnicastRemoteObject.exportObject(obj,Registry.REGISTRY_PORT);
			try {
				register.bind(HBaseConstants.RS_DRIVER, stub);
				
				System.out.println("Region server started succesfully");
			} catch (AlreadyBoundException e) {
				// TODO Auto-generated catch block
				System.out.println("Region server failed to bind");
				e.printStackTrace();
			}
			
		} catch (RemoteException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}		
	}

	@Override
	public byte[] create(byte[] inp) throws RemoteException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public byte[] put(byte[] inp) throws RemoteException {
		// TODO Auto-generated method stub
		/** Put request will be written here
		 * Some program will identify the region and give its reference
		 *  **/
		return null;
	}

	@Override
	public byte[] get(byte[] inp) throws RemoteException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public byte[] scan(byte[] inp) throws RemoteException {
		// TODO Auto-generated method stub
		return null;
	}
	
	

}
