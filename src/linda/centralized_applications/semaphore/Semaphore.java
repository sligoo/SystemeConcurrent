package linda.centralized_applications.semaphore;

import linda.Tuple;
import linda.shm.CentralizedLinda;

import java.util.List;

/**
 * Project: SystemesConcurrents
 * Created by sacha on 04/01/2017.
 */
public class Semaphore {

    private CentralizedLinda linda;
    private String sema;

    public Semaphore(CentralizedLinda linda, String sema){
        this.linda = linda;
        this.sema = sema;
        this.linda.write(new Tuple(sema));
    }

    public void P(){
        this.linda.take(new Tuple(sema));
    }

    public void V(){
        this.linda.write(new Tuple(sema));
    }
}
