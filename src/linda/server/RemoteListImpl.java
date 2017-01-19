package linda.server;

import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by lucien on 18/01/17.
 */

public class RemoteListImpl<T> extends UnicastRemoteObject implements RemoteList<T> {
    private List<T> list;

    public RemoteListImpl() throws RemoteException
    {
        list = new ArrayList<>();
    }

    @Override
    public void add(T obj) throws RemoteException {
        list.add(obj);
    }

    @Override
    public T get(int i) throws RemoteException {
        return list.get(i);
    }

    @Override
    public int size() throws RemoteException {
        return list.size();
    }
}
