package linda.server;

import java.rmi.Remote;
import java.rmi.RemoteException;

/**
 * Created by lucien on 19/01/17.
 */
public interface RemoteList<T> extends Remote {
    void add(T obj) throws RemoteException;
    T get(int i) throws RemoteException;
    int size() throws RemoteException;
}
