package linda.server;

import linda.AsynchronousCallback;
import linda.Linda;
import linda.Tuple;
import linda.server.LindaClient;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.util.ArrayList;

/**
 * Created by rhiobet on 09/01/17.
 */
public class TestEvents {

  static int readImmediate = 0;

  public static void main(String args[]) {
    LindaClient linda = new LindaClient("//127.0.0.1:9000/Server");

    Tuple tuplesImmediate[] = new Tuple[] {
            new Tuple(345, 762, "coucou", true),
            new Tuple(53782, 65442, "allo", "test"),
            new Tuple(345, 762, 7863853, false),
            new Tuple(53782, 65442, new ArrayList<>(), true),
            new Tuple(345, "test", "coucou", true),
            new Tuple("allo", 65442, "coucou", true)
    };

    for (int i = 0; i < 2; i++) {
      linda.eventRegister(Linda.eventMode.READ, Linda.eventTiming.FUTURE,
              new Tuple(Integer.class, Integer.class, String.class, Boolean.class),
              new AsynchronousCallback(t -> {
                System.out.println("TTT");
                if (t.matches(tuplesImmediate[0])) {
                  readImmediate++;
                }
              })
      );
    }
    try {
      Thread.sleep(1000);
      System.out.println(readImmediate);
      linda.write(new Tuple(345, 762, "coucou", true));
      Thread.sleep(1000);
      System.out.println(readImmediate);
      linda.write(new Tuple(345, 762, "coucou", true));
      Thread.sleep(1000);
      System.out.println(readImmediate);
    } catch (InterruptedException e) {
      e.printStackTrace();
    }
  }

}
