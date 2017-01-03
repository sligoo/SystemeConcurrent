package linda;

import java.util.Collection;

/** Public interface to a Linda implementation.
 * @author philippe.queinnec@enseeiht.fr
 */
public interface Linda {

    /** Adds a tuple t to the tuplespace. */
    void write(Tuple t);

    /** Returns a tuple matching the template and removes it from the tuplespace.
     * Blocks if no corresponding tuple is found. */
    Tuple take(Tuple template);

    /** Returns a tuple matching the template and leaves it in the tuplespace.
     * Blocks if no corresponding tuple is found. */
    Tuple read(Tuple template);

    /** Returns a tuple matching the template and removes it from the tuplespace.
     * Returns null if none found. */
    Tuple tryTake(Tuple template);

    /** Returns a tuple matching the template and leaves it in the tuplespace.
     * Returns null if none found. */
    Tuple tryRead(Tuple template);

    /** Returns all the tuples matching the template and removes them from the tuplespace.
     * Returns an empty collection if none found (never blocks).
     * Note: there is no atomicity or consistency constraints between takeAll and other methods;
     * for instance two concurrent takeAll with similar templates may split the tuples between the two results.
     */
    Collection<Tuple> takeAll(Tuple template);

    /** Returns all the tuples matching the template and leaves them in the tuplespace.
     * Returns an empty collection if none found (never blocks).
     * Note: there is no atomicity or consistency constraints between readAll and other methods;
     * for instance (write([1]);write([2])) || readAll([?Integer]) may return only [2].
     */
    Collection<Tuple> readAll(Tuple template);

    enum eventMode { READ, TAKE }
    enum eventTiming { IMMEDIATE, FUTURE }

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
    void eventRegister(eventMode mode, eventTiming timing, Tuple template, Callback callback);

    /** To debug, prints any information it wants (e.g. the tuples in tuplespace or the registered callbacks), prefixed by <code>prefix</code. */

    void debug(String prefix);

}
