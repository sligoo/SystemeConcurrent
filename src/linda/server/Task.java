package linda.server;

import linda.Tuple;

import javax.print.DocFlavor;

/**
 * A class used as a wrapper for a Linda server "instruction" & accompanying tuple.
 * Is given to worker threads for execution.
 *
 * Created by jayjader on 1/18/17.
 */
class Task {

    private final Instruction instruction;
    private final Tuple tuple;
    private Tuple result;

    Task(Instruction instr, Tuple t) {
        this.instruction = instr;
        this.tuple = t;
    }

    void setResult(Tuple t) {
        this.result = t;
    }

    Tuple getResult() {
        return this.result;
    }

    Instruction getInstruction() {
        return this.instruction;
    }

    Tuple getTuple() {
        return this.tuple;
    }

    enum Instruction {
        WRITE,
        READ,
        TAKE,
        TRYREAD,
        TRYTAKE
    }
}