package linda.test;

import linda.Linda;
import linda.Tuple;
import linda.shm.CentralizedLinda;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.util.*;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

/**
 * Project: SystemesConcurrents
 * Created by sacha on 30/11/2016.
 */
public class TestsElementairesCentralized {

  private Linda linda;

  @Before
  public void initialize() {
    linda = new CentralizedLinda();
  }

  @Test
  public void testWriteAndTryRead() {
    Tuple tupleWritten = new Tuple("coucou", 5, 3, false);
    linda.write(tupleWritten);
    Tuple tupleRead = linda.tryRead(new Tuple(String.class, Integer.class,
            Integer.class, Boolean.class));
    if (tupleRead != null)
      Assert.assertTrue("Read tuple should match 'coucou'|5|3|false",
              tupleRead.matches(tupleWritten));
    else
      Assert.fail("Read tuple shouldn't be null");

    tupleWritten = new Tuple(new Tuple("shittyFlute", 3456), "troubalourds",
            "japan7", true);
    linda.write(tupleWritten);
    tupleRead = linda.tryRead(new Tuple(new Tuple(String.class, Integer.class), String.class,
            String.class, Boolean.class));
    if (tupleRead != null)
      Assert.assertTrue("Read tuple should match ['shittyFlute'|3456]|'troubalourds'|'japan7'|true",
              tupleRead.matches(tupleWritten));
    else
      Assert.fail("Read tuple shouldn't be null");

    tupleRead = linda.tryRead(new Tuple(new Tuple(String.class, Integer.class), String.class,
            String.class, Boolean.class));
    Assert.assertNull("TryRead() shouldn't have made the tuple disappear", tupleRead);

    Assert.assertNull("Read tuple should be null", linda.tryRead(new Tuple(Integer.class)));

    Tuple tupleWrite = new Tuple(new Tuple("fluteShitty", 3564), "troubalourds",
            "japan7", false);
    linda.write(tupleWrite);
    tupleRead = linda.tryRead(new Tuple(Tuple.class, "troubalourds", "japan7", false));
    if (tupleRead != null)
      Assert.assertTrue("Read tuple should match ['fluteShitty'|3564]|'troubalourds'|'japan7'|false",
              tupleRead.matches(tupleWritten));
    else
      Assert.fail("Read tuple shouldn't be null");
  }

  @Test
  public void testTake(){
    Tuple tupleWritten = new Tuple(42);
    linda.write(tupleWritten);
    assertTrue("Take a value tuple template", linda.take
            (new Tuple(42)).matches(tupleWritten));
    assertNull("After a take, tuple is empty",linda.tryRead(tupleWritten));
  }

  @Test
  public void testRead(){
    Tuple tupleWritten = new Tuple(42);
    linda.write(tupleWritten);
    assertTrue("Read a value tuple template", linda.read(new Tuple(42)).matches(tupleWritten));
    assertTrue("After a read, tuple is as before", linda.tryRead(tupleWritten)
            .matches(tupleWritten));
  }

  @Test
  public void testTakeLock() throws InterruptedException {
    Tuple tupleWritten = new Tuple("empty");
    linda.write(tupleWritten);
    Thread mainRunner = new Thread(() -> linda.take(new Tuple(5)));

    mainRunner.start();

    Thread.sleep(1000);

    assertEquals(Thread.State.WAITING, mainRunner.getState());
    mainRunner.interrupt();
  }

  @Test
  public void testReadLock() throws InterruptedException {
    Tuple tupleWritten = new Tuple("empty");
    linda.write(tupleWritten);
    Thread mainRunner = new Thread(() -> linda.read(new Tuple(5)));

    mainRunner.start();

    Thread.sleep(1000);

    assertEquals(Thread.State.WAITING, mainRunner.getState());
    mainRunner.interrupt();
  }

  @Test
  public void testTryTake(){
    Tuple tupleWritten = new Tuple("empty");
    linda.write(tupleWritten);
    Assert.assertNull("tryTake a none existing object return null", linda
            .tryTake(new Tuple(5)));


    assertEquals("tryTake a Class tuple template",tupleWritten, linda.tryTake
            (new Tuple(String.class)));

    tupleWritten = new Tuple(42);
    linda.write(tupleWritten);
    assertEquals("tryTake a value tuple template", tupleWritten,linda.tryTake
            (new Tuple(42)));
  }

  @SuppressWarnings("unchecked")
  @Test
  public void testWriteWithEditions() {
    ArrayList<Integer> list = new ArrayList<>();
    list.add(345);
    list.add(7346);
    ArrayList<Integer> listCopy = new ArrayList<>(list);

    ArrayList<ArrayList<Integer>> listLvl2 = new ArrayList<>();
    listLvl2.add(list);
    listLvl2.add(new ArrayList<>());
    ArrayList<ArrayList<Integer>> listLvl2Copy = new ArrayList<>();
    listLvl2Copy.add(listCopy);
    listLvl2Copy.add(new ArrayList<>());

    Tuple tupleWritten = new Tuple(listLvl2);
    Tuple tupleCopy = new Tuple(listLvl2Copy);
    linda.write(tupleWritten);

    list.add(43);
    list.add(6369);
    ArrayList<Integer> newList = new ArrayList<>();
    newList.add(43);
    listLvl2.add(1, newList);

    Tuple tupleRead = linda.read(new Tuple(Object.class));
    Assert.assertTrue("The tuple should be deep copied at write()", tupleRead.matches(tupleCopy));
    ((ArrayList<ArrayList<Integer>>) tupleRead.get(0)).get(0).add(45);
    tupleRead = linda.take(new Tuple(Object.class));
    Assert.assertTrue("The tuple should be deep copied at read()", tupleRead.matches(tupleCopy));
  }

  @Test
  public void testReadAll() {
    Tuple tuples[] = new Tuple[]{
            new Tuple(345, 762, "coucou", true),
            new Tuple(53782, 65442, "allo", "test"),
            new Tuple(345, 762, 7863853, false),
            new Tuple(53782, 65442, new ArrayList<>(), true),
            new Tuple(345, 762, "coucou", true),
            new Tuple("allo", 65442, "coucou", true)
    };

    for (Tuple tuple : tuples) {
      linda.write(tuple);
    }

    Collection<Tuple> read1 = linda.readAll(new Tuple(345, 762, "coucou", true));
    Assert.assertEquals("2 tuples should have been read", 2, read1.size());
    for (Tuple tuple : read1) {
      Assert.assertTrue("The tuple should be 345|762|'coucou'|true", tuple.matches(tuples[0]));
    }

    Tuple match = new Tuple(Integer.class, Integer.class, Object.class, Boolean.class);
    Collection<Tuple> read2 = linda.readAll(match);
    Assert.assertEquals("4 tuples should have been read", 2, read2.size());
    Assert.assertTrue("The collection should contain tuples[0]", read2.contains(tuples[0]));
    Assert.assertTrue("The collection should contain tuples[3]", read2.contains(tuples[3]));
    Assert.assertTrue("The collection should contain tuples[4]", read2.contains(tuples[4]));
    Assert.assertTrue("The collection should contain tuples[5]", read2.contains(tuples[5]));

    Assert.assertTrue("The collection should be empty",
            linda.readAll(new Tuple(Object.class)).isEmpty());
  }

  @Test
  public void testTakeAll() {
    Tuple tuples[] = new Tuple[]{
            new Tuple(345, 762, "coucou", true),
            new Tuple(53782, 65442, "allo", "test"),
            new Tuple(345, 762, 7863853, false),
            new Tuple(53782, 65442, new ArrayList<>(), true),
            new Tuple(345, 762, "coucou", true),
            new Tuple("allo", 65442, "coucou", true)
    };

    for (Tuple tuple : tuples) {
      linda.write(tuple);
    }

    Collection<Tuple> read1 = linda.takeAll(new Tuple(345, 762, "coucou", true));
    Assert.assertEquals("2 tuples should have been read", 2, read1.size());
    for (Tuple tuple : read1) {
      Assert.assertTrue("The tuple should be 345|762|'coucou'|true", tuple.matches(tuples[0]));
    }

    Tuple match = new Tuple(Integer.class, Integer.class, Object.class, Boolean.class);
    Collection<Tuple> read2 = linda.takeAll(match);
    Assert.assertEquals("4 tuples should have been read", 2, read2.size());
    Assert.assertTrue("The collection should contain tuples[3]", read2.contains(tuples[3]));
    Assert.assertTrue("The collection should contain tuples[4]", read2.contains(tuples[4]));

    Assert.assertTrue("The collection should be empty",
            linda.takeAll(new Tuple(Object.class)).isEmpty());
  }

}
