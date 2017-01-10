package linda.centralized_applications.philosophes;

import linda.Tuple;
import linda.shm.CentralizedLinda;

/**
 * Created by rhiobet on 04/01/17.
 */
public class StrategieLinda implements StrategiePhilo {
  CentralizedLinda linda = new CentralizedLinda();

  public StrategieLinda(int nbPhilosophes) {
    for (int i = 0; i < Main.nbPhilosophes; i++) {
      linda.write(new Tuple("Fourchette", i));
    }
  }

  @Override
  public void demanderFourchettes(int no) throws InterruptedException {
    if (no == 0) {
      linda.take(new Tuple("Fourchette", Main.FourchetteGauche(no)));
      IHMPhilo.poser (Main.FourchetteGauche(no), EtatFourchette.AssietteDroite);
      linda.take(new Tuple("Fourchette", Main.FourchetteDroite(no)));
      IHMPhilo.poser (Main.FourchetteDroite(no), EtatFourchette.AssietteGauche);
    } else {
      linda.take(new Tuple("Fourchette", Main.FourchetteDroite(no)));
      IHMPhilo.poser (Main.FourchetteDroite(no), EtatFourchette.AssietteGauche);
      linda.take(new Tuple("Fourchette", Main.FourchetteGauche(no)));
      IHMPhilo.poser (Main.FourchetteGauche(no), EtatFourchette.AssietteDroite);

    }
  }

  @Override
  public void libererFourchettes(int no) throws InterruptedException {
    IHMPhilo.poser (Main.FourchetteGauche(no), EtatFourchette.Table);
    IHMPhilo.poser (Main.FourchetteDroite(no), EtatFourchette.Table);

    linda.write(new Tuple("Fourchette", Main.FourchetteGauche(no)));
    linda.write(new Tuple("Fourchette", Main.FourchetteDroite(no)));
  }

  @Override
  public String nom() {
    return "Stratégie Linda";
  }
}
