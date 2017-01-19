package linda;

public class SynchronousCallback implements Callback {

    private Tuple template;

    public SynchronousCallback(Tuple template) {
        this.template = template;
    }

    public void call(final Tuple t) {
        if (t.matches(template)) {
            synchronized (this) {
                this.notify();
            }
        }
    }
}
