package linda.server;

import linda.Callback;
import linda.Tuple;

import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;

/**
 * Created by rhiobet on 10/01/17.
 */
public class CallbackRemoteImpl extends UnicastRemoteObject implements CallbackRemote {

  private Callback callback;

  public CallbackRemoteImpl(Callback callback) throws RemoteException {
    this.callback = callback;
  }

  @Override
  public void call(Tuple t) throws RemoteException {
    this.callback.call(t);
  }

  @Override
  public Callback getCallback() throws RemoteException {
    return this.callback;
  }

}
