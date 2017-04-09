package com.hbase.rs;

import java.io.IOException;
import java.rmi.Remote;
import java.rmi.RemoteException;

import com.hbase.miscl.HBase.LoadRegionRequest;

public interface IRegionServer extends Remote {

	/** create table **/
	byte[] create(byte[] inp) throws RemoteException;
	
	/** put table **/
	byte[] put(byte[] inp) throws RemoteException;
	
	/** get table **/
	byte[] get(byte[] inp) throws RemoteException;
	
	/** scan table **/
	byte[] scan(byte[] inp) throws RemoteException;
	
	/** load region 
	 * @throws InterruptedException 
	 * @throws IOException **/
	
	byte[] loadRegion(byte[] inp) throws RemoteException;
}
