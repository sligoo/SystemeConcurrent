package linda.server;

import linda.Callback;
import linda.Linda;
import linda.Tuple;
import linda.shm.CentralizedLinda;

import java.rmi.AlreadyBoundException;
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
 * Multi-Server Linda implementation
 * Created by jayjader on 1/18/17.
 */
public class LindaMultiServer extends UnicastRemoteObject implements LindaServer {
    public static final long serialVersionUID = 1L;

    private String name;
    private String namingURI;
    private Linda linda;
    private  List<Worker> workers;
    private RemoteList<String> serverRegistry;
    private BlockingQueue<Task> tasks;

    /**
     * Multi-Server implementation of Linda
     * @param namingURI the URI for the Naming server containing the Multi-Server registry
     * @param PORT int
     * @throws RemoteException
     */
    public LindaMultiServer (String namingURI, int PORT) throws RemoteException {
        super();
        this.namingURI = namingURI;
        this.linda = new CentralizedLinda();
        this.tasks = new LinkedBlockingQueue<>();

        LocateRegistry.createRegistry(PORT);
        try {
            // Get the server registry, create it if nonexistent
            this.serverRegistry = (RemoteList<String>) Naming.lookup(this.namingURI + "/ServerRegistry");
            if (this.serverRegistry == null) {
                this.serverRegistry = new RemoteList<>();
                Naming.rebind(this.namingURI + "/ServerRegistry", this.serverRegistry);
            }

            // Add ourselves to the registry & Naming server
            this.name = "Server" + this.serverRegistry.size();
            Naming.bind(this.namingURI + "/" + this.name, this);
            this.serverRegistry.add(namingURI + "/" +  this.name);
        } catch (MalformedURLException | NotBoundException e) {
            e.printStackTrace();
        } catch (AlreadyBoundException e) {
            e.printStackTrace();
            System.exit(1);
        }

        // Launch main "overseer" thread which consumes tasks, spawns workers, and directly handles WRITEs
        Thread overseer = new Thread(() -> {
            while (true) {
                // Consume task. If the queue is empty wait for a new task to appear
                Task currentTask = this.tasks.poll();

                if (currentTask.getInstruction() == Task.Instruction.WRITE) {
                    // Writes are handled by the main thread
                    this.linda.write(currentTask.getTuple());
                    this.notifyServersTupleWritten();
                } else {
                    // Spawn worker
                    Worker w = new Worker(currentTask, linda, namingURI + "/ServerRegistry");
                    w.start();

                    this.workers.add(w);
                }
            }
        });
        overseer.start();
    }

    /**
     * Notifies the other servers a Tuple has been added to the tuplespace
     */
    private void notifyServersTupleWritten() {
        // Fetch size once
        int size = 0;
        try {
            size = this.serverRegistry.size();
        } catch (RemoteException e) {
            e.printStackTrace();
        }

        // Notify the other servers
        for (int i = 0; i < size; i++) {
            try {
                LindaMultiServer server = (LindaMultiServer) Naming.lookup(this.serverRegistry.get(i));
                server.notifyTupleWritten();
            } catch (NotBoundException | MalformedURLException | RemoteException e) {
                e.printStackTrace();
            }
        }
    }

    /** Creates a Task and adds it to the task queue
     */
    public Task createTask(Task.Instruction instruction, Tuple template) {
        Task task = new Task(instruction, template);
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

        return task;
    }

    /**
     * Adds a task to write a Tuple t to the Linda
     * @param t Tuple
     * @throws RemoteException
     */
    @Override
    public void write(Tuple t) throws RemoteException {
        Task task = this.createTask(WRITE, t);
    }

    /**
     * Adds a blocking task to take a Tuple template from the Linda
     * @param template Tuple
     * @return Tuple
     * @throws RemoteException
     */
    @Override
    public Tuple take(Tuple template) throws RemoteException {
        Task task = this.createTask(TAKE, template);
        return task.getResult();
    }

    /**
     * Adds a blocking task to read a Tuple template from the Linda
     * @param template
     * @return
     * @throws RemoteException
     */
    @Override
    public Tuple read(Tuple template) throws RemoteException {
        Task task = this.createTask(READ, template);
        return task.getResult();
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

    /**
     * Notifies all the workers that a tuple has been written
     */
    public void notifyTupleWritten() {
        for (Worker w : this.workers) {
            w.notify();
        }
    }
}
