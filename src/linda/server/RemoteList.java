package linda.server;

import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by lucien on 18/01/17.
 */

public class RemoteList<T> extends UnicastRemoteObject {
    private List<T> list;

    public RemoteList() throws RemoteException
    {
        list = new ArrayList<>();
    }

    public void add(T obj) throws RemoteException {
        list.add(obj);
    }

    public T get(int i) throws RemoteException {
        return list.get(i);
    }

    public int size() throws RemoteException {
        return list.size();
    }
}
