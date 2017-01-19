package linda.server;

import java.rmi.Remote;
import java.rmi.RemoteException;

/**
 * Created by lucien on 19/01/17.
 */
public interface LindaMultiServer extends Remote, LindaServer {
    void notifyTupleWritten() throws RemoteException;
}
