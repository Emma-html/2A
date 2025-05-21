package linda.shm;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;

import linda.AsynchronousCallback;
import linda.Callback;
import linda.Linda;
import linda.Tuple;

/** Shared memory implementation of Linda. */
public class CentralizedLinda implements Linda {

	private final List<Tuple> tuplespace;
    private final ReentrantLock lock;
    private final Condition newTupleCondition;
    private final Map<Tuple, List<Callback>> pendingReadCallbacks;
    private final Map<Tuple, List<Callback>> pendingTakeCallbacks;

    public CentralizedLinda() {
        this.tuplespace = new ArrayList<>();
        this.lock = new ReentrantLock();
        this.newTupleCondition = lock.newCondition();
        this.pendingReadCallbacks = new HashMap<>();
        this.pendingTakeCallbacks = new HashMap<>();
    }

    /** Adds a tuple t to the tuplespace. */
    public void write(Tuple t) {
        lock.lock();
        try {
            // Priorité aux callbacks TAKE
            for (Map.Entry<Tuple, List<Callback>> entry : pendingTakeCallbacks.entrySet()) {
                if (t.matches(entry.getKey())) {
                    Callback cb = entry.getValue().remove(0);
                    cb.call(t); // Appel du callback, ex. via AsynchronousCallback
                    if (entry.getValue().isEmpty()) {
                        pendingTakeCallbacks.remove(entry.getKey());
                    }
                    return; // Ne pas stocker le tuple
                }
            }

            // Ensuite les callbacks READ
            for (Map.Entry<Tuple, List<Callback>> entry : pendingReadCallbacks.entrySet()) {
                if (t.matches(entry.getKey())) {
                    Callback cb = entry.getValue().remove(0);
                    cb.call(t);
                    if (entry.getValue().isEmpty()) {
                        pendingReadCallbacks.remove(entry.getKey());
                    }
                    break; // continue quand même à stocker le tuple
                }
            }

            // Aucun callback → stocke le tuple normalement
            tuplespace.add(t);
            newTupleCondition.signalAll();
        } finally {
            lock.unlock();
        }
    }

    /** Returns a tuple matching the template and removes it from the tuplespace.
     * Blocks if no corresponding tuple is found. */
    public Tuple take(Tuple template) {
        lock.lock();
        try {
            while (true) {
                for (Tuple t : tuplespace) {
                    if (t.matches(template)) {
                        tuplespace.remove(t);
                        return t;
                    }
                }
                newTupleCondition.await();
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return null;
        } finally {
            lock.unlock();
        }
    }

    /** Returns a tuple matching the template and leaves it in the tuplespace.
     * Blocks if no corresponding tuple is found. */
    public Tuple read(Tuple template) {
        lock.lock();
        try {
            while (true) {
                for (Tuple t : tuplespace) {
                    if (t.matches(template)) {
                        return t;
                    }
                }
                newTupleCondition.await();
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return null;
        } finally {
            lock.unlock();
        }
    }

    /** Returns a tuple matching the template and removes it from the tuplespace.
     * Returns null if none found. */
    public Tuple tryTake(Tuple template) {
        lock.lock();
        try {
            for (Tuple t : tuplespace) {
                if (t.matches(template)) {
                    tuplespace.remove(t);
                    return t;
                }
            }
            return null;
        } finally {
            lock.unlock();
        }
    }

    /** Returns a tuple matching the template and leaves it in the tuplespace.
     * Returns null if none found. */
    public Tuple tryRead(Tuple template) {
        lock.lock();
        try {
            for (Tuple t : tuplespace) {
                if (t.matches(template)) {
                    return t;
                }
            }
            return null;
        } finally {
            lock.unlock();
        }
    }

    /** Returns all the tuples matching the template and removes them from the tuplespace.
     * Returns an empty collection if none found (never blocks).
     * Note: there is no atomicity or consistency constraints between takeAll and other methods;
     * for instance two concurrent takeAll with similar templates may split the tuples between the two results.
     */
    public Collection<Tuple> takeAll(Tuple template) {
        lock.lock();
        try {
            List<Tuple> result = new ArrayList<>();
            for (Tuple t : tuplespace) {
                if (t.matches(template)) {
                    result.add(t);
                }
            }
            tuplespace.removeAll(result);
            return result;
        } finally {
            lock.unlock();
        }
    }

    /** Returns all the tuples matching the template and leaves them in the tuplespace.
     * Returns an empty collection if none found (never blocks).
     * Note: there is no atomicity or consistency constraints between readAll and other methods;
     * for instance (write([1]);write([2])) || readAll([?Integer]) may return only [2].
     */
    public Collection<Tuple> readAll(Tuple template) {
        lock.lock();
        try {
            List<Tuple> result = new ArrayList<>();
            for (Tuple t : tuplespace) {
                if (t.matches(template)) {
                    result.add(t);
                }
            }
            return result;
        } finally {
            lock.unlock();
        }
    }

    /** Registers a callback which will be called when a tuple matching the template appears.
     * If the mode is Take, the found tuple is removed from the tuplespace.
     * The callback is fired once. It may re-register itself if necessary.
     * If timing is immediate, the callback may immediately fire if a matching tuple is already present; if timing is future, current tuples are ignored.
     * Beware: a callback should never block as the calling context may be the one of the writer (see also {@link AsynchronousCallback} class).
     * Callbacks are not ordered: if more than one may be fired, the chosen one is arbitrary.
     * Beware of loop with a READ/IMMEDIATE re-registering callback !
     *
     * @param mode read or take mode.
     * @param timing (potentially) immediate or only future firing.
     * @param template the filtering template.
     * @param callback the callback to call if a matching tuple appears.
     */
    public void eventRegister(eventMode mode, eventTiming timing, Tuple template, Callback callback) {
        // On ne peut pas enregistrer un callback si le template est null
        if (template == null) {
            throw new IllegalArgumentException("Template cannot be null");
        }
        // On ne peut pas enregistrer un callback si le callback est null
        if (callback == null) {
            throw new IllegalArgumentException("Callback cannot be null");
        }

        // On ne peut pas enregistrer un callback si le mode est null
        if (mode == null) {
            throw new IllegalArgumentException("Mode cannot be null");
        }
        // On ne peut pas enregistrer un callback si le timing est null
        if (timing == null) {
            throw new IllegalArgumentException("Timing cannot be null");
        }
       lock.lock();
        try {
            // Cherche un tuple correspondant déjà présent
            for (Tuple t : tuplespace) {
                if (t.matches(template)) {
                    if (timing == Linda.eventTiming.IMMEDIATE) {
                        if (mode == Linda.eventMode.TAKE) {
                            tuplespace.remove(t);
                        }
                        callback.call(t);
                        return;
                    }
                    // FUTURE => on attend une future apparition
                    break;
                }
            }

            // Enregistrer le callback pour plus tard
            if (mode == Linda.eventMode.READ) {
                pendingReadCallbacks
                    .computeIfAbsent(template, k -> new ArrayList<>())
                    .add(callback);
            } else if (mode == Linda.eventMode.TAKE) {
                pendingTakeCallbacks
                    .computeIfAbsent(template, k -> new ArrayList<>())
                    .add(callback);
            }
        } finally {
            lock.unlock();
        }
    }

    /** To debug, prints any information it wants (e.g. the tuples in tuplespace or the registered callbacks), prefixed by <code>prefix</code. */
    public void debug(String prefix) {
        lock.lock();
        try {
            System.out.println(prefix + "Debugging CentralizedLinda:");
            System.out.println(prefix + "Tuplespace: " + tuplespace);
            System.out.println(prefix + "Pending Read Callbacks: " + pendingReadCallbacks);
            System.out.println(prefix + "Pending Take Callbacks: " + pendingTakeCallbacks);
        } finally {
            lock.unlock();
        }
    }

}
