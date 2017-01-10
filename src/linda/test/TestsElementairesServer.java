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
            if (m.getName().startsWith("test") && m.getName() != "testEvents" && !m.getName().contains("Lock")) {
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

    @Test
    public void test2() {

    }

    @Test
    public void testEvents() {
        int expectedReadImmediate = 3, expectedTakeImmediate = 2, expectedReadFuture = 1, expectedTakeFuture = 1;

        readImmediate = 0;
        takeImmediate = 0;
        readFuture = 0;
        takeFuture = 0;

        LindaClient client = new LindaClient(URI);

        Tuple tuplesImmediate[] = new Tuple[] {
                new Tuple(345, 762, "coucou", true),
                new Tuple(53782, 65442, "allo", "test"),
                new Tuple(345, 762, 7863853, false),
                new Tuple(53782, 65442, new ArrayList<>(), true),
                new Tuple(345, "test", "coucou", true),
                new Tuple("allo", 65442, "coucou", true)
        };

        for (Tuple tuple : tuplesImmediate) {
            client.write(tuple);
        }

        // Tests for Immediate/Read
        for (int i = 0; i < 2; i++) {
            client.eventRegister(Linda.eventMode.READ, Linda.eventTiming.IMMEDIATE,
                    new Tuple(Integer.class, Integer.class, String.class, Boolean.class),
                    t -> {
                        if (t.matches(tuplesImmediate[0])) {
                            readImmediate++;
                        }
                    }
            );
        }

        Tuple tupleImmediateRead = new Tuple(345, 762, "coucou", 43722);
        client.eventRegister(Linda.eventMode.READ, Linda.eventTiming.IMMEDIATE,
                new Tuple(Integer.class, Integer.class, String.class, Integer.class),
                t -> {
                    if (t.matches(tupleImmediateRead)) {
                        readImmediate++;
                    }
                }
        );
        try {
            Thread.sleep(500);
            client.write(tupleImmediateRead);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        // Tests for Immediate/Take
        for (int i = 0; i < 2; i++) {
            client.eventRegister(Linda.eventMode.TAKE, Linda.eventTiming.IMMEDIATE,
                    new Tuple(Integer.class, Integer.class, String.class, String.class),
                    t -> {
                        if (t.matches(tuplesImmediate[1])) {
                            takeImmediate++;
                        }
                    }
            );
        }

        Tuple tupleImmediateWrite = new Tuple(345, 762, 43722);
        client.eventRegister(Linda.eventMode.READ, Linda.eventTiming.IMMEDIATE,
                new Tuple(Integer.class, Integer.class, Integer.class),
                t -> {
                    if (t.matches(tupleImmediateWrite)) {
                        takeImmediate++;
                    }
                }
        );
        try {
            Thread.sleep(500);
            client.write(tupleImmediateRead);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        // Tests for Future/Read
        Tuple tupleFutureRead = new Tuple(345, 9543, "coucou", true);
        client.eventRegister(Linda.eventMode.READ, Linda.eventTiming.FUTURE,
                new Tuple(Integer.class, Integer.class, String.class, Boolean.class),
                t -> {
                    if (t.matches(tupleFutureRead)) {
                        readFuture++;
                    }
                }
        );
        try {
            Thread.sleep(500);
            client.write(tupleFutureRead);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        Assert.assertNotNull("Event for Future/Read shouldn't destroy the tuple",
                client.tryRead(tupleFutureRead));

        // Tests for Future/Take
        Tuple tupleFutureTake = new Tuple(34543, 76322, 7853, false, true);
        client.eventRegister(Linda.eventMode.TAKE, Linda.eventTiming.FUTURE,
                new Tuple(Integer.class, Integer.class, Integer.class, Boolean.class, Boolean.class),
                t -> {
                    if (t.matches(tupleFutureTake)) {
                        takeFuture++;
                    }
                }
        );
        try {
            Thread.sleep(500);
            client.write(tupleFutureTake);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        Assert.assertNull("Event for Future/Take should destroy the tuple", client.tryRead(tupleFutureTake));

        // Checks
        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        Assert.assertEquals("Immediate/Read is not working correctly",
                expectedReadImmediate, readImmediate);
        Assert.assertEquals("Immediate/Take is not working correctly",
                expectedTakeImmediate, takeImmediate);
        Assert.assertEquals("Future/Read is not working correctly",
                expectedReadFuture, readFuture);
        Assert.assertEquals("Future/Take is not working correctly",
                expectedTakeFuture, takeFuture);
    }

    public static void main(String[] args) {
        org.junit.runner.JUnitCore.main(TestsElementairesServer.class.getName());
    }
}
