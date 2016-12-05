package linda.shm;

import linda.Callback;
import linda.Linda;
import linda.Tuple;

import java.util.Collection;
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
        this.signaler = this.lock.newCondition();
    }

    @Override
    public void write(Tuple t) {
        // Mutual exclusion for accessing stored tuples
        this.lock.lock();
        this.tuples.add(t);
        // Signals waiting reads/takes that an new tuple has been received
        this.signaler.signal();
        this.lock.unlock();
    }

    @Override
    public Tuple take(Tuple template) {
        boolean found = false;
        Tuple result;

        while (!found) {
            // Mutual exclusion for iterating through stored tuples
            this.lock.lock();
            for (Tuple t : this.tuples) {
                if (Tuple.matches(t, template)) {
                    result = t;
                    this.tuples.remove(t);
                    found = true;
                    // We're returning the first match found
                    break;
                }
            }
            this.lock.unlock();

            if (result == null) {
                // If template not found, wait for another tuple to be added
                // note: the lock must be released before this point if a new
                // tuple is to be stored
                this.signaler.await();
            }
        }
        return result;
    }

    @Override
    public Tuple read(Tuple template) {
        boolean found = false;
        Tuple result;

        while (!found) {
            // Mutual exclusion for iterating through stored tuples
            this.lock.lock();
            for (Tuple t : this.tuples) {
                if (Tuple.matches(t, template)) {
                    result = t;
                    found = true;
                    // We're returning the first match found
                    break;
                }
            }
            this.lock.unlock();

            if (result == null) {
                // If template not found, wait for another tuple to be added
                // note: the lock must be released before this point if a new
                // tuple is to be stored
                this.signaler.await();
            }
        }
        return result;
    }

    @Override
    public Tuple tryTake(Tuple template) {
        return null;
    }

    @Override
    public Tuple tryRead(Tuple template) {
        return null;
    }

    @Override
    public Collection<Tuple> takeAll(Tuple template) {
        return null;
    }

    @Override
    public Collection<Tuple> readAll(Tuple template) {
        return null;
    }

    @Override
    public void eventRegister(eventMode mode, eventTiming timing, Tuple template, Callback callback) {

    }

    @Override
    public void debug(String prefix) {

    }

    // TO BE COMPLETED

}