package linda.server;

import linda.Callback;
import linda.Tuple;

import java.rmi.Remote;
import java.rmi.RemoteException;

/**
 * Created by rhiobet on 10/01/17.
 */
public interface CallbackRemote extends Remote {

  void call(final Tuple t) throws RemoteException;
  Callback getCallback() throws RemoteException;

  }
