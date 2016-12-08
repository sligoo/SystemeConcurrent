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
    private Lock tuplesLock;
    private Lock callbacksLock;
    private List<CallbackRef> callbacks;

    public CentralizedLinda() {
        this.tuples = new ArrayList<Tuple>();
        this.tuplesLock = new ReentrantLock();
        this.callbacks = new ArrayList<CallbackRef>();
        this.callbacksLock = new ReentrantLock();
    }

    @Override
    public void write(Tuple t) {
        // Mutual exclusion for accessing stored tuples
        try {
            this.tuplesLock.lock();
            Tuple tuple = t.deepclone();
            this.tuples.add(tuple);
            // Signals waiting reads/takes that an new tuple has been received
            synchronized (this) {
                notifyAll();
            }
        } finally {
            this.tuplesLock.unlock();
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
                this.tuplesLock.lock();
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
                this.tuplesLock.unlock();
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
            this.tuplesLock.lock();
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
                this.tuplesLock.unlock();
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
            this.tuplesLock.lock();
            for (Tuple t : this.tuples) {
                if (t.matches(template)) {
                    result = t;
                    this.tuples.remove(t);
                    // We're returning the first match found
                    break;
                }
            }
        } finally {
            this.tuplesLock.unlock();
        }
        return result;
    }

    @Override
    public Tuple tryRead(Tuple template) {
        Tuple result = null;

        // Mutual exclusion for iterating through stored tuples
        try {
            this.tuplesLock.lock();
            for (Tuple t : this.tuples) {
                if (t.matches(template)) {
                    result = t;
                    // We're returning the first match found
                    break;
                }
            }
        } finally {
            this.tuplesLock.unlock();
        }
        return result;
    }

    @Override
    public Collection<Tuple> takeAll(Tuple template) {
        Collection<Tuple> result = new ArrayList<Tuple>();

        // Mutual exclusion for iterating through stored tuples
        try {
            this.tuplesLock.lock();
            // Get all tuples matching the template
            result = this.tuples.stream()
                    .filter(t -> t.matches(template))
                    .collect(Collectors.toList());
            // Remove the collected tuples from ones stored
            this.tuples.removeAll(result);
        } finally {
            this.tuplesLock.unlock();
        }

        return result;
    }

    @Override
    public Collection<Tuple> readAll(Tuple template) {
        Collection<Tuple> result = new ArrayList<Tuple>();

        // Mutual exclusion for iterating through stored tuples
        try {
            this.tuplesLock.lock();
            // Get all tuples matching the template
            result = this.tuples.stream()
                    .filter(t -> t.matches(template))
                    .collect(Collectors.toList());
        } finally {
            this.tuplesLock.unlock();
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

        try {
            this.callbacksLock.lock();
            List<CallbackRef> registered = this.callbacks.stream()
                    .filter(c -> c.getTemplate().contains(t))
                    .collect(Collectors.toList());
            for (CallbackRef c : registered) {
                c.getCallback().call(t);

                if (c.getMode() == eventMode.TAKE) {
                    this.tryTake(t);
                }

                this.callbacks.remove(c);
            }
        } finally {
            this.callbacksLock.unlock();
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
            this.tuplesLock.lock();
            for (Tuple t : this.tuples) {
                if (t.matches(template)) {
                    c.getCallback().call(t);
                    this.callbacks.remove(c);
                    break;
                }
            }
        } finally {
            this.tuplesLock.unlock();
        }
    }

    @Override
    public void debug(String prefix) {
        System.out.println(prefix + " Tuples : [ ");
        try {
            this.tuplesLock.lock();
            for (Tuple t : this.tuples) {
                System.out.println("        " + t + ",");
            }
        } finally {
            this.tuplesLock.unlock();
        }
        System.out.println("    ]" + " ; Callbacks [");
        try {
            this.callbacksLock.lock();
            for (CallbackRef c : this.callbacks) {
                System.out.print("        ");
                if (c.getTiming() == eventTiming.FUTURE) {
                    System.out.print("FUTURE");
                } else {
                    System.out.print("IMMEDIATE");
                }
                System.out.print(" / ");
                if (c.getMode() == eventMode.READ) {
                    System.out.print("READ");
                } else {
                    System.out.print("TAKE");
                }
                System.out.print(" : ");
                System.out.print(c.getTemplate());
                System.out.println(",");
            }
            System.out.println("    ]");
        } finally {
            this.callbacksLock.unlock();
        }
    }
}
