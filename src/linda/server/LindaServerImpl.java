package linda.server;

import linda.Callback;
import linda.Linda;
import linda.Tuple;
import linda.shm.CentralizedLinda;
import linda.shm.MultiThreadLinda;

import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;
import java.util.Collection;

/**
 * Created by rhiobet on 09/01/17.
 */
public class LindaServerImpl extends UnicastRemoteObject implements LindaServer {
    public static final long serialVersionUID = 1L;

  private Linda linda;

  public LindaServerImpl() throws RemoteException {
    super();
    this.linda = new MultiThreadLinda(4);
  }

  @Override
  public void write(Tuple t) throws RemoteException {
    this.linda.write(t);
  }

  @Override
  public Tuple take(Tuple template) throws RemoteException {
    return this.linda.take(template);
  }

  @Override
  public Tuple read(Tuple template) throws RemoteException {
    return this.linda.read(template);
  }

  @Override
  public Tuple tryTake(Tuple template) throws RemoteException {
    return this.linda.tryTake(template);
  }

  @Override
  public Tuple tryRead(Tuple template) throws RemoteException {
    return this.linda.tryRead(template);
  }

  @Override
  public Collection<Tuple> takeAll(Tuple template) throws RemoteException {
    return this.linda.takeAll(template);
  }

  @Override
  public Collection<Tuple> readAll(Tuple template) throws RemoteException {
    return this.linda.readAll(template);
  }

  @Override
  public void eventRegister(Linda.eventMode mode, Linda.eventTiming timing, Tuple template, CallbackRemote callback)
          throws RemoteException {
    this.linda.eventRegister(mode, timing, template, new CallbackServer(callback));
  }

  @Override
  public void debug(String prefix) throws RemoteException {
    this.linda.debug(prefix);
  }
}
