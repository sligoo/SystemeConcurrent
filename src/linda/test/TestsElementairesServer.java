package linda.test;

import linda.Linda;
import linda.Tuple;
import linda.server.LindaClient;
import linda.server.LindaServer;
import linda.server.LindaServerImpl;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

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
import java.lang.Runnable;

/**
 * Project: SysConc
 * Created by lucien on 10/1/2017
 */
public class TestsElementairesServer {

    private final int PORT = 4000;
    private final String URI = "//localhost:" + PORT + "/LindaServer";
    private LindaServer server;
    private static boolean registryCreated = false;
    private int readImmediate, takeImmediate, readFuture, takeFuture;

    @Before
    public void initializeServer() {
        try {
            server = new LindaServerImpl();
            if(!registryCreated) {
                registryCreated = true;
                LocateRegistry.createRegistry(PORT);
            }
            Naming.rebind(URI, server);
        } catch (RemoteException | MalformedURLException e) {
            e.printStackTrace();
        }
    }

    public void restartServer() {
        try {
            server = new LindaServerImpl();
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
            if (m.getName().startsWith("test") && !m.getName().contains("Lock")) {
                try {
                    restartServer();
                    LindaClient client = new LindaClient(URI);
                    TestsElementairesCentralized test = new TestsElementairesCentralized(client);
                    System.out.println(m.getName());
                    m.invoke(test);
                } catch (IllegalAccessException e) {
                    e.printStackTrace();
                } catch (InvocationTargetException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    private class MultiClientTester implements Runnable {
        private Linda client;
        private String task;
        private Tuple template;

        MultiClientTester(String URI, String task, Tuple template) {
            super();
            this.client = new LindaClient(URI);
            this.task = task;
            this.template = template;
        }

        public void run() {
            switch (this.task) {
                case "write":
                    this.client.write(this.template);
                    break;
                case "take":
                    this.client.take(this.template);
                    break;
                case "read":
                    this.client.read(this.template);
                    break;
            }
        }
    }

    @Test
    public void testMultiClient() {
        Thread writeTester = new Thread(new MultiClientTester(URI, "write", new Tuple(0)));
        Thread readTester = new Thread(new MultiClientTester(URI, "read", new Tuple(0)));

        readTester.start();
        try {
            Thread.sleep(10000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        assertEquals(Thread.State.WAITING, readTester.getState());

        writeTester.start();
        try {
            Thread.sleep(500);
        } catch (InterruptedException e) {}
        assertEquals(Thread.State.RUNNABLE, readTester.getState());

        try {
            writeTester.join();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        try {
            readTester.join();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    public static void main(String[] args) {
        org.junit.runner.JUnitCore.main(TestsElementairesServer.class.getName());
    }
}
