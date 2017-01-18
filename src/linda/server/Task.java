package linda.server;

import linda.Tuple;

import javax.print.DocFlavor;

/**
 * A class used as a wrapper for a Linda server "instruction" & accompanying tuple.
 * Is given to worker threads for execution.
 *
 * Created by jayjader on 1/18/17.
 */
public class Task {

    private final Instruction instruction;
    private final Tuple tuple;

    public Task(Instruction instr, Tuple t) {
        this.instruction = instr;
        this.tuple = t;
    }

    public void setResult() {

    }

    // TODO
    public Tuple result() {
        return null;
    }

    public enum Instruction {
        WRITE,
        READ,
        TAKE,
        TRYREAD,
        TRYTAKE
    }
}