package linda.server;

import java.net.MalformedURLException;
import java.rmi.Naming;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;

/**
 * Created by rhiobet on 09/01/17.
 */
public class StartServer {

  public static final int PORT = 9000;

  public static void main(String args[]) {
    try {
      LindaServerImpl server = new LindaServerImpl();
      LocateRegistry.createRegistry(PORT);
      Naming.rebind("//127.0.0.1:" + PORT + "/Server", server);
    } catch (RemoteException | MalformedURLException e) {
      e.printStackTrace();
    }
  }

}
