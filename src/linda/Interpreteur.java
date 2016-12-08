package linda;

import linda.centralized_applications.whiteboard.Whiteboard;
import linda.centralized_applications.Application;
import linda.shm.CentralizedLinda;

import java.util.ArrayList;
import java.util.InputMismatchException;
import java.util.List;
import java.util.Scanner;

/**
 * Created by rhiobet on 08/12/16.
 */
public class Interpreteur {

  private static CentralizedLinda linda;
  private static List<Application> applications;

  public static void main(String args[]) {
    linda = new CentralizedLinda();

    applications = new ArrayList<>();
    applications.add(new Whiteboard(linda));

    waitCommand();
  }

  private static void waitCommand() {
    boolean end = false, retour;
    int choice;
    Scanner scanner = new Scanner(System.in);
    Tuple tuple;

    while (!end) {
      System.out.println("Que souahitez-vous faire ?");
      System.out.println(" 1. Ajouter un tuple");
      System.out.println(" 2. Rechercher un match");
      System.out.println(" 3. Supprimer un match");
      System.out.println(" 4. Lancer une application");
      System.out.println(" 5. Quitter l'interpréteur");
      System.out.print("Choix : ");
      try {
        choice = scanner.nextInt();
      } catch (InputMismatchException e) {
        choice = -1;
      }
      System.out.println();
      scanner.nextLine();

      switch (choice) {
        case 1:
          System.out.println("Saisir le tuple à ajouter :");
          tuple = Tuple.valueOf(scanner.nextLine());
          linda.write(tuple);
          System.out.println("Ajout effectué\n");
          break;

        case 2:
          System.out.println("Saisir le tuple recherché :");
          tuple = Tuple.valueOf(scanner.nextLine());
          System.out.println("Résultats :");
          for (Tuple t : linda.readAll(tuple)) {
            System.out.println(" " + t);
          }
          System.out.println();
          break;

        case 3:
          System.out.println("Saisir le tuple à supprimer :");
          tuple = Tuple.valueOf(scanner.nextLine());
          linda.takeAll(tuple);
          System.out.println("Suppression effectuée\n");
          break;

        case 4:
          retour = false;

          while (!retour) {
            System.out.println("Quelle application souhaitez-vous exécuter ?");
            for (int i = 1; i <= applications.size(); i++) {
              System.out.println(" " + i + ". " + applications.get(i-1).getClass().getName());
            }
            System.out.println(" 0. Retour");
            System.out.print("Choix : ");
            try {
              choice = scanner.nextInt();
            } catch (InputMismatchException e) {
              choice = -1;
            }
            scanner.nextLine();

            if (choice == 0) {
              retour  = true;
            } else if (choice >= 1 && choice <= applications.size()) {
              System.out.print("Arguments : ");

              final int num = choice-1;
              final String[] args = scanner.nextLine().split(" ");
              new Thread(() ->
                      applications.get(num).run(args)
              ).start();
            }
            System.out.println();
          }
          break;

        case 5:
          end = true;
          break;

        default:
          System.out.println("Commande non reconnue");
          System.out.println();
      }
    }

    scanner.close();
  }

}
