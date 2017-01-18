package linda.server;

import linda.Linda;

/**
 * Created by lucien on 18/01/17.
 */
public class Worker extends Thread {

    private Task task;
    private Linda linda;

    public Worker(Task task, Linda linda) {
        this.task = task;
        this.linda = linda;
    }

    public void start() {
    }
}
