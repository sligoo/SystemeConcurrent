package linda.server;

import linda.Callback;
import linda.Linda;
import linda.Tuple;

import java.rmi.NotBoundException;
import java.rmi.registry.LocateRegistry;
import java.rmi.Naming;
import java.net.MalformedURLException;
import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;


/**
 * Created by jayjader on 1/18/17.
 */
public class LindaMultiServer extends UnicastRemoteObject implements LindaServer, Runnable {
    public static final long serialVersionUID = 1L;

    private List<Tuple> tuplespace;
    private List<Callback> callbacks;
    private List<LindaServer> serverRegistry;
    private BlockingQueue<Task> tasks;

    /**
     * Multi-Server implementation of Linda
     * @param namingURI the URI for the Naming server containing the Multi-Server registry
     * @param PORT
     * @throws RemoteException
     */
    public LindaMultiServer (String namingURI, int PORT) throws RemoteException {
        super();
        this.tuplespace = new ArrayList<>();
        this.callbacks = new ArrayList<>();
        this.tasks = new LinkedBlockingQueue<>();

        LocateRegistry.createRegistry(PORT);
        try {
            // Add ourselves to the registry
            List<LindaServer> serverRegistry = (List<LindaServer>) Naming.lookup(namingURI + "/ServerRegistry");
            serverRegistry.add(this);
        } catch (MalformedURLException | NotBoundException e) {
            e.printStackTrace();
        }
    }

    /**
     * Adds a task to write a Tuple t to the tuplespace
     * @param t Tuple
     * @throws RemoteException
     */
    @Override
    public void write(Tuple t) throws RemoteException {
        Task task = new Task(Task.Instruction.WRITE, t);
        try {
            this.tasks.put(task);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    /**
     * Adds a blocking task to take a Tuple template from the tuplespace
     * @param template Tuple
     * @return Tuple
     * @throws RemoteException
     */
    @Override
    public Tuple take(Tuple template) throws RemoteException {
        Task task = new Task(Task.Instruction.TAKE, template);
        try {
            this.tasks.put(task);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        try {
            task.wait();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        return task.result();
    }

    @Override
    public Tuple read(Tuple template) throws RemoteException {
        return null;
    }

    @Override
    public Tuple tryTake(Tuple template) throws RemoteException {
        return null;
    }

    @Override
    public Tuple tryRead(Tuple template) throws RemoteException {
        return null;
    }

    @Override
    public Collection<Tuple> takeAll(Tuple template) throws RemoteException {
        return null;
    }

    @Override
    public Collection<Tuple> readAll(Tuple template) throws RemoteException {
        return null;
    }

    @Override
    public void eventRegister(Linda.eventMode mode, Linda.eventTiming timing, Tuple template, CallbackRemote callback) throws RemoteException {

    }

    @Override
    public void debug(String prefix) throws RemoteException {

    }

    @Override
    public void run() {

    }
}
