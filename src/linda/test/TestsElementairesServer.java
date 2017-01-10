package linda.test;

import linda.Linda;
import linda.server.LindaClient;
import linda.server.LindaServer;
import linda.server.LindaServerImpl;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.MalformedURLException;
import java.rmi.Naming;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.server.UnicastRemoteObject;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Project: SysConc
 * Created by lucien on 10/1/2017
 */
public class TestsElementairesServer {

    private final int PORT = 4000;
    private final String URI = "//localhost:" + PORT + "/LindaServer";
    private LindaServer server;

    @Before
    public void initializeServer() {
        try {
            server = new LindaServerImpl();
            LocateRegistry.createRegistry(PORT);
            Naming.rebind(URI, server);
        } catch (RemoteException | MalformedURLException e) {
            e.printStackTrace();
        }
    }

    public void restartServer() {
        shutdownServer();
        LindaServerImpl server = null;
        try {
            server = new LindaServerImpl();
        } catch (RemoteException e) {
            e.printStackTrace();
        }
        try {
            Naming.rebind(URI, server);
        } catch (RemoteException e) {
            e.printStackTrace();
        } catch (MalformedURLException e) {
            e.printStackTrace();
        }
    }

    @After
    public void shutdownServer() {
        try {
            UnicastRemoteObject.unexportObject(server, false);
            Naming.unbind(URI);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Test
    public void testMonoClient() {
        List<Method> methods = Arrays.asList(TestsElementairesCentralized.class.getDeclaredMethods());

        for(Method m : methods)
        {
            LindaClient client = new LindaClient(URI);
            TestsElementairesCentralized test = new TestsElementairesCentralized(client);

            if (m.getName().startsWith("test")) {
                try {
                    restartServer();
                    System.out.println(m.getName());
                    m.invoke(test);
                } catch (IllegalAccessException e) {
                    e.printStackTrace();
                } catch (InvocationTargetException e) {
                    e.printStackTrace();
                } finally {
                    shutdownServer();
                }
            }
        }

    }

    public static void main(String[] args) {
        org.junit.runner.JUnitCore.main(TestsElementairesServer.class.getName());
    }
}
