package linda.server;

import linda.Linda;
import linda.Tuple;

import java.net.MalformedURLException;
import java.rmi.Naming;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.Collection;


/**
 * Created by lucien on 18/01/17.
 */
public class Worker extends Thread {

    private Task task;
    private Linda linda;
    private String uriServerRegistry;

    public Worker(Task task, Linda linda, String uriServerRegistry) {
        this.task = task;
        this.linda = linda;
        this.uriServerRegistry = uriServerRegistry;
    }

    public void start() {
        switch (task.getInstruction()) {
            case READ:
                while(task.getResult() == null) {
                    Tuple result = readFromAllServers();
                    checkResult(result);
                }
                break;

            case TAKE:
                while(task.getResult() == null) {
                    Tuple result = takeFromAllServers();
                    checkResult(result);
                }
                break;

            case TRYREAD:
                task.setResult(readFromAllServers());
                break;

            case TRYTAKE:
                task.setResult(takeFromAllServers());
                break;

            case READALL:
                task.setResultAll(readAll());
                break;

            case TAKEALL:
                task.setResultAll(takeAll());
                break;
            default:
                break;
        }
    }

    private void checkResult(Tuple result) {
        if(result == null) {
            try {
                this.wait();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        } else {
            task.setResult(result);
        }
    }

    private Tuple readFromAllServers() {
        RemoteList<String> serverRegistry = null;
        int size = 0;
        Tuple result = linda.tryRead(task.getTuple());

        // If tuple not in memory, try read from all servers
        if(result == null) {
            // get serverRegistry
            try {
                serverRegistry = (RemoteList<String>) Naming.lookup(uriServerRegistry);
                size = serverRegistry.size();
            } catch (NotBoundException | RemoteException | MalformedURLException e) {
                e.printStackTrace();
            }

            // try read on all servers
            for (int i = 0; i < size; i++) {
                try {
                    LindaMultiServer server = (LindaMultiServer) Naming.lookup(serverRegistry.get(i));
                    result = server.tryRead(task.getTuple());
                } catch (NotBoundException | MalformedURLException | RemoteException e) {
                    e.printStackTrace();
                }
                if (result != null)
                    break;
            }
        }

        return result;
    }

    private Tuple takeFromAllServers() {
        RemoteList<String> serverRegistry = null;
        int size = 0;
        Tuple result = linda.tryTake(task.getTuple());

        // If tuple not in memory, try read from all servers
        if(result == null) {
            // get serverRegistry
            try {
                serverRegistry = (RemoteList<String>) Naming.lookup(uriServerRegistry);
                size = serverRegistry.size();
            } catch (NotBoundException | RemoteException | MalformedURLException e) {
                e.printStackTrace();
            }
            // try take on all servers
            for (int i = 0; i < size; i++) {
                try {
                    LindaMultiServer server = (LindaMultiServer) Naming.lookup(serverRegistry.get(i));
                    result = server.tryTake(task.getTuple());
                } catch (NotBoundException | MalformedURLException | RemoteException e) {
                    e.printStackTrace();
                }
                if (result != null)
                    break;
            }
        }

        return result;
    }

    private Collection<Tuple> readAll() {
        Collection<Tuple> result = new ArrayList<>();
        RemoteList<String> serverRegistry = null;
        int size = 0;

        try {
            serverRegistry = (RemoteList<String>) Naming.lookup(uriServerRegistry);
            size = serverRegistry.size();
        } catch (NotBoundException | RemoteException | MalformedURLException e) {
            e.printStackTrace();
        }

        for(int i = 0 ; i < size ; i ++) {
            try {
                LindaMultiServer server = (LindaMultiServer) Naming.lookup(serverRegistry.get(i));
                result.addAll(server.readAll(task.getTuple()));
            } catch (RemoteException | NotBoundException | MalformedURLException e) {
                e.printStackTrace();
            }
        }

        return result;
    }

    private Collection<Tuple> takeAll() {
        Collection<Tuple> result = new ArrayList<>();
        RemoteList<String> serverRegistry = null;
        int size = 0;

        try {
            serverRegistry = (RemoteList<String>) Naming.lookup(uriServerRegistry);
            size = serverRegistry.size();
        } catch (NotBoundException | RemoteException | MalformedURLException e) {
            e.printStackTrace();
        }

        for(int i = 0 ; i < size ; i ++) {
            try {
                LindaMultiServer server = (LindaMultiServer) Naming.lookup(serverRegistry.get(i));
                result.addAll(server.takeAll(task.getTuple()));
            } catch (RemoteException | NotBoundException | MalformedURLException e) {
                e.printStackTrace();
            }
        }

        return result;
    }


}
