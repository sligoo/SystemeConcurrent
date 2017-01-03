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
    private String id;

    public Semaphore(CentralizedLinda linda, String id){
        this.linda = linda;
        this.id = id;
        this.linda.write(new Tuple(id));
    }

    public void P(){
        this.linda.take(new Tuple(id));
    }

    public void V(){
        this.linda.write(new Tuple(id));
    }
}
