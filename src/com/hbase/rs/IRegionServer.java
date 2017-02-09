package com.hbase.rs;

import java.rmi.Remote;
import java.rmi.RemoteException;

public interface IRegionServer extends Remote {

	/** create table **/
	byte[] create(byte[] inp) throws RemoteException;
	
	/** put table **/
	byte[] put(byte[] inp) throws RemoteException;
	
	/** get table **/
	byte[] get(byte[] inp) throws RemoteException;
	
	/** scan table **/
	byte[] scan(byte[] inp) throws RemoteException;
	
	/** load region **/
	
	boolean loadRegion(String tableName) throws RemoteException;
}
