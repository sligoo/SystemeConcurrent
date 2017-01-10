import linda.*;

import java.util.concurrent.Callable;

public class Task implements Callable<Tuple> {
    private Linda linda;
    private Tuple target;
    private Tuple result = null;

    public Task(Linda linda, Tuple target) {
        this.linda = linda;
        this.target = target;
    }
    @Override
    public Tuple call() {
        Tuple courant;
        courant = linda.tryTake(new Tuple("BD", String.class, String.class));
        if (courant != null) {
            int score = AlignementSeq.similitude(((String)courant.get(1)).toCharArray(),
                                             ((String)target.get(1)).toCharArray());
            result = new Tuple(score, courant);
        }
        return result;
    }
}
