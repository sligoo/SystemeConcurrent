package linda.shm;

import linda.Callback;
import linda.Linda;
import linda.SynchronousCallback;
import linda.Tuple;

import javax.management.monitor.Monitor;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Random;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.stream.Collectors;

/**
 * Created by rhiobet on 08/01/17.
 */
public class MultiThreadLinda implements Linda {

  private CentralizedLinda[] centralizedLinda;
  private int threadsNumber;
  private Random random;
  private Lock callbacksLock;
  private List<CallbackRef> callbacks;
  private Collection<SynchronousCallback> synchronousCallbacks;

  public MultiThreadLinda(int threadsNumber) {
    this.threadsNumber = threadsNumber;
    this.centralizedLinda = new CentralizedLinda[threadsNumber];
    for (int i = 0; i < threadsNumber; i++) {
      this.centralizedLinda[i] = new CentralizedLinda();
    }
    this.random = new Random();
    this.callbacks = new ArrayList<>();
    this.synchronousCallbacks = new ArrayList<>();
    this.callbacksLock = new ReentrantLock();
  }

  @Override
  public void write(Tuple t) {
    this.centralizedLinda[this.random.nextInt(this.threadsNumber)].write(t.deepclone());
    for (SynchronousCallback callback : this.synchronousCallbacks) {
      callback.call(t);
    }
    this.checkCallbacks(t);
  }

  @Override
  public Tuple take(Tuple template) {
    Tuple found = null;

    while (found == null) {
      found = this.checkAll(template, eventMode.TAKE);

      if (found == null) {
        // If template not found, wait for another tuple to be added
        // note: the lock must be released before this point if a new
        // tuple is to be stored
        try {
          SynchronousCallback callback = new SynchronousCallback(template);
          this.synchronousCallbacks.add(callback);
          synchronized (callback) {
            callback.wait();
          }
          this.synchronousCallbacks.remove(callback);
        } catch (InterruptedException ignored) {
        }
      }
    }

    return found;
  }

  @Override
  public Tuple read(Tuple template) {
    Tuple found = null;

    while (found == null) {
      found = this.checkAll(template, eventMode.READ);

      if (found == null) {
        // If template not found, wait for another tuple to be added
        // note: the lock must be released before this point if a new
        // tuple is to be stored
        try {
          SynchronousCallback callback = new SynchronousCallback(template);
          this.synchronousCallbacks.add(callback);
          synchronized (callback) {
            callback.wait();
          }
          this.synchronousCallbacks.remove(callback);
        } catch (InterruptedException ignored) {
        }
      }
    }

    return found;
  }

  @Override
  public Tuple tryTake(Tuple template) {
    return this.checkAll(template, eventMode.TAKE);
  }

  @Override
  public Tuple tryRead(Tuple template) {
    return this.checkAll(template, eventMode.READ);
  }

  @Override
  public Collection<Tuple> takeAll(Tuple template) {
    return this.checkAllCollection(template, eventMode.TAKE);
  }

  @Override
  public Collection<Tuple> readAll(Tuple template) {
    return this.checkAllCollection(template, eventMode.READ);
  }

  private Tuple checkAll(Tuple template, eventMode mode) {
    int depart = this.random.nextInt(this.threadsNumber), i;
    Tuple found = null;

    i = depart;

    do {
      if (mode == eventMode.READ) {
        found = this.centralizedLinda[i].tryRead(template);
      } else if (mode == eventMode.TAKE) {
        found = this.centralizedLinda[i].tryTake(template);
      }

      if (found != null) {
        found = found.deepclone();
        break;
      }

      i = (i+1) % this.threadsNumber;
    } while (i != depart);

    return found;
  }

  private Collection<Tuple> checkAllCollection(Tuple template, eventMode mode) {
    int depart = this.random.nextInt(this.threadsNumber), i;
    Collection<Tuple> found = new ArrayList<>();

    i = depart;

    do {
      if (mode == eventMode.READ) {
        found.addAll(this.centralizedLinda[i].readAll(template));
      } else if (mode == eventMode.TAKE) {
        found.addAll(this.centralizedLinda[i].takeAll(template));
      }

      i = (i+1) % this.threadsNumber;
    } while (i != depart);

    return found;
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
    Tuple template = c.getTemplate(), t;

    if ((t = this.checkAll(c.getTemplate(), eventMode.READ)) != null) {
      c.getCallback().call(t);
      this.callbacks.remove(c);
    }
  }

  @Override
  public void debug(String prefix) {
    for (int i = 0; i < this.threadsNumber; i++) {
      this.centralizedLinda[i].debug(prefix);
    }
  }
}
