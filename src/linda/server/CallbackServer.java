package linda.server;

import linda.Callback;
import linda.Tuple;

import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;

/**
 * Created by rhiobet on 18/01/17.
 */
public class CallbackServer implements Callback {

  private CallbackRemote callback;

  public CallbackServer(CallbackRemote callback) throws RemoteException {
    this.callback = callback;
  }

  @Override
  public void call(Tuple t) {
    try {
      this.callback.call(t);
    } catch (RemoteException e) {
      e.printStackTrace();
    }
  }
}
