package linda.shm;

import linda.Callback;
import linda.Linda;
import linda.Tuple;

import java.util.Collection;
import java.util.stream.Collectors;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.HashMap;
//import javax.management.lock.Monitor;
import java.util.concurrent.locks.ReentrantLock;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.Condition;

/** Shared memory implementation of Linda. */
public class CentralizedLinda implements Linda {

    private List<Tuple> tuples;
    private Lock lock;
    private Condition signaler;
    private List<CallbackRef> callbacks;

    public CentralizedLinda() {
        this.tuples = new ArrayList<Tuple>();
        this.lock = new ReentrantLock();
        this.callbacks = new ArrayList<CallbackRef>();
    }

    @Override
    public void write(Tuple t) {
        // Mutual exclusion for accessing stored tuples
        try {
            this.lock.lock();
            Tuple tuple = t.deepclone();
            this.tuples.add(tuple);
            // Signals waiting reads/takes that an new tuple has been received
            synchronized (this) {
                notifyAll();
            }
        } finally {
            this.lock.unlock();
        }

        // Check registered callbacks to call any valid ones
        this.checkCallbacks(t);
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
                        result = t.deepclone();
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

    private class CallbackRef {
        private eventMode mode;
        private eventTiming timing;
        private Tuple template;
        private Callback callback;

        private CallbackRef(eventMode em, eventTiming et, Tuple t, Callback c) {
            this.mode = em;
            this.timing = et;
            this.template = t;
            this.callback = c;
        }

        private eventMode getMode() {
            return this.mode;
        }
        private eventTiming getTiming() {
            return this.timing;
        }
        private Tuple getTemplate() {
            return this.template;
        }
        private Callback getCallback() {
            return this.callback;
        }
    }

    @Override
    public synchronized void eventRegister(eventMode mode, eventTiming timing, Tuple template,
            Callback callback) {
        CallbackRef newRef = new CallbackRef(mode, timing, template, callback);
        this.callbacks.add(newRef);

        // Check if the callback should be called immediately
        if (timing == eventTiming.IMMEDIATE) {
            // If so, call it & remove it from the waiting list
            this.checkCallback(newRef);
        }

    }

    /** Executes all callbacks registered to the given template
     * @param t Tuple
     */
    private void checkCallbacks(Tuple t) {
        List<CallbackRef> registered = this.callbacks.stream()
                                .filter(c -> c.getTemplate().matches(t))
                                .collect(Collectors.toList());

        for (CallbackRef c : registered) {
            c.getCallback().call(t);
            this.callbacks.remove(c);
        }
    }

    /** Checks if the given callback should already be called
     * @param c CallbackRef
     *
     * If it should, call it then remove it from the waiting list
     */
    private void checkCallback(CallbackRef c) {
        Tuple template = c.getTemplate();

        try {
            this.lock.lock();
            for (Tuple t : this.tuples) {
                if (template.matches(t)) {
                    c.getCallback().call(t);
                    this.callbacks.remove(c);
                    break;
                }
            }
        } finally {
            this.lock.unlock();
        }
    }

    @Override
    public void debug(String prefix) {

    }

    // TO BE COMPLETED

}
