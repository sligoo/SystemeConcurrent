package linda.centralized_applications.semaphore;

import linda.centralized_applications.Application;
import linda.shm.CentralizedLinda;

/**
 * Project: SystemesConcurrents
 * Created by sacha on 03/01/2017.
 */
public class Semaphore implements Application{

    private CentralizedLinda linda;

    public Semaphore(CentralizedLinda linda){
        this.linda = linda;
    }

    @Override
    public void run(String[] args) {

    }
}
