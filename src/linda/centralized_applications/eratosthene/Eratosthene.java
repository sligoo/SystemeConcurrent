package linda.centralized_applications.eratosthene;

import linda.Tuple;
import linda.centralized_applications.Application;
import linda.shm.CentralizedLinda;

/**
 * Created by rhiobet on 09/12/16.
 */
public class Eratosthene implements Application {

  private CentralizedLinda linda;
  private int k;

  public Eratosthene(CentralizedLinda linda) {
    this.linda = linda;
  }

  @Override
  public void run(String[] args) {
    boolean initOk = false;

    try {
      k = Integer.parseInt(args[0]);
      initOk = true;
    } catch (Exception e) {
      System.out.println("Arguments : k");
    }

    if (initOk) {
      System.out.println("Nombres premiers :");
      for (int i = 2; i <= this.k; i++) {
        if (this.linda.tryRead(new Tuple("eratosthene", i, false)) == null) {
          System.out.print("| " + i + " ");

          if (this.linda.tryRead(new Tuple("eratosthene", i, true)) == null) {
            for (int j = 2 * i; j <= this.k; j += i) {
              this.linda.write(new Tuple("eratosthene", j, false));
            }
            this.linda.write(new Tuple("eratosthene", i, true));
          }
        }
      }
    }
  }

}
