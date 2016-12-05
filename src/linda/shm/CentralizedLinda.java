package linda.shm;

import linda.Callback;
import linda.Linda;
import linda.Tuple;

import java.util.Collection;
import java.util.stream.Collectors;
import java.util.ArrayList;
import java.util.List;
//import javax.management.lock.Monitor;
import java.util.concurrent.locks.ReentrantLock;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.Condition;

/** Shared memory implementation of Linda. */
public class CentralizedLinda implements Linda {

    private List<Tuple> tuples;
    private Lock lock;
    private Condition signaler;

    public CentralizedLinda() {
        this.tuples = new ArrayList<Tuple>();
        this.lock = new ReentrantLock();
    }

    @Override
    public void write(Tuple t) {
        // Mutual exclusion for accessing stored tuples
        try {
            this.lock.lock();
            this.tuples.add(t);
            // Signals waiting reads/takes that an new tuple has been received
            synchronized (this) {
                notifyAll();
            }
        } finally {
            this.lock.unlock();
        }
    }

    @Override
    public Tuple take(Tuple template) {
        boolean found = false;
        Tuple result = null;

        while (!found) {
            // Mutual exclusion for iterating through stored tuples
            try {
                this.lock.lock();
                for (Tuple t : this.tuples) {
                    if (t.matches(template)) {
                        result = t;
                        this.tuples.remove(t);
                        found = true;
                        // We're returning the first match found
                        break;
                    }
                }
            } finally {
                this.lock.unlock();
            }

            if (result == null) {
                // If template not found, wait for another tuple to be added
                // note: the lock must be released before this point if a new
                // tuple is to be stored
                try {
                    synchronized (this) {
                        wait();
                    }
                } catch (InterruptedException e) {
                    e.printStackTrace();
                    System.exit(1);
                }
            }
        }
        return result;
    }

    @Override
    public Tuple read(Tuple template) {
        boolean found = false;
        Tuple result = null;

        while (!found) {
            // Mutual exclusion for iterating through stored tuples
            this.lock.lock();
            try {
                for (Tuple t : this.tuples) {
                    if (t.matches(template)) {
                        result = t;
                        found = true;
                        // We're returning the first match found
                        break;
                    }
                }
            } finally {
                this.lock.unlock();
            }

            if (result == null) {
                // If template not found, wait for another tuple to be added
                // note: the lock must be released before this point if a new
                // tuple is to be stored
                try {
                    synchronized (this) {
                        wait();
                    }
                } catch (InterruptedException e) {
                    e.printStackTrace();
                    System.exit(1);
                }
            }
        }
        return result;
    }

    @Override
    public Tuple tryTake(Tuple template) {
        Tuple result = null;

        // Mutual exclusion for iterating through stored tuples
        try {
            this.lock.lock();
            for (Tuple t : this.tuples) {
                if (t.matches(template)) {
                    result = t;
                    this.tuples.remove(t);
                    // We're returning the first match found
                    break;
                }
            }
        } finally {
            this.lock.unlock();
        }
        return result;
    }

    @Override
    public Tuple tryRead(Tuple template) {
        Tuple result = null;

        // Mutual exclusion for iterating through stored tuples
        try {
            this.lock.lock();
            for (Tuple t : this.tuples) {
                if (t.matches(template)) {
                    result = t;
                    // We're returning the first match found
                    break;
                }
            }
        } finally {
            this.lock.unlock();
        }
        return result;
    }

    @Override
    public Collection<Tuple> takeAll(Tuple template) {
        Collection<Tuple> result = new ArrayList<Tuple>();

        // Mutual exclusion for iterating through stored tuples
        try {
            this.lock.lock();
            // Get all tuples matching the template
            result = this.tuples.stream()
                .filter(t -> t.matches(template))
                .collect(Collectors.toList());
            // Remove the collected tuples from ones stored
            this.tuples.removeAll(result);
        } finally {
            this.lock.unlock();
        }

        return result;
    }

    @Override
    public Collection<Tuple> readAll(Tuple template) {
        Collection<Tuple> result = new ArrayList<Tuple>();

        // Mutual exclusion for iterating through stored tuples
        try {
            this.lock.lock();
            // Get all tuples matching the template
            result = this.tuples.stream()
                .filter(t -> t.matches(template))
                .collect(Collectors.toList());
        } finally {
            this.lock.unlock();
        }

        return result;
    }

    @Override
    public void eventRegister(eventMode mode, eventTiming timing, Tuple template, Callback callback) {

    }

    @Override
    public void debug(String prefix) {

    }

    // TO BE COMPLETED

}
