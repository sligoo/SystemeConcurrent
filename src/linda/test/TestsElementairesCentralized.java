package linda.test;

import linda.AsynchronousCallback;
import linda.Linda;
import linda.Tuple;
import linda.shm.CentralizedLinda;
import linda.shm.MultiThreadLinda;
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
    private int readImmediate, takeImmediate, readFuture, takeFuture;

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
            Assert.assertTrue("Read tuple should match ['coucou'|5|3|false]",
                    tupleRead.matches(tupleWritten));
        else
            Assert.fail("Read tuple shouldn't be null");

        tupleWritten = new Tuple(new Tuple("shittyFlute", 3456), "troubalourds",
                "japan7", true);
        linda.write(tupleWritten);
        tupleRead = linda.tryRead(new Tuple(new Tuple(String.class, Integer.class), String.class,
                String.class, Boolean.class));

        if (tupleRead != null)
            Assert.assertTrue("Read tuple should match [['shittyFlute'|3456]|'troubalourds'|'japan7'|true]",
                    tupleRead.matches(tupleWritten));
        else
            Assert.fail("Read tuple shouldn't be null");

        tupleRead = linda.tryRead(new Tuple(new Tuple(String.class, Integer.class), String.class,
                String.class, Boolean.class));
        Assert.assertTrue("TryRead() shouldn't have made the tuple disappear", tupleRead.matches(tupleWritten));

        Assert.assertNull("Read tuple should be null", linda.tryRead(new Tuple(Integer.class)));

        tupleWritten = new Tuple(new Tuple("fluteShitty", 3564), "troubalourds",
                "japan7", false);
        linda.write(tupleWritten);
        tupleRead = linda.tryRead(new Tuple(Tuple.class, "troubalourds", "japan7", false));

        if (tupleRead != null)
            Assert.assertTrue("Read tuple should match [['fluteShitty'|3564]|'troubalourds'|'japan7'|false]",
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
        assertTrue("Read a value tuple template",
                linda.read(new Tuple(42)).matches(tupleWritten));
        assertTrue("After a read, tuple is as before",
                linda.tryRead(tupleWritten).matches(tupleWritten));
    }

    @Test
    public void testTakeLock() {
        Tuple tupleWritten = new Tuple("empty");
        linda.write(tupleWritten);
        Thread mainRunner = new Thread(() -> linda.take(new Tuple(5)));

        mainRunner.start();

        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        assertEquals(Thread.State.WAITING, mainRunner.getState());
        mainRunner.interrupt();
    }

    @Test
    public void testReadLock() {
        Tuple tupleWritten = new Tuple("empty");
        linda.write(tupleWritten);
        Thread mainRunner = new Thread(() -> linda.read(new Tuple(5)));

        mainRunner.start();

        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

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
        System.out.println(tupleRead.toString());
        System.out.println(tupleCopy.toString());
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
        Assert.assertEquals("4 tuples should have been read", 4, read2.size());
        Assert.assertTrue("The collection should contain tuples[0]", read2.contains(tuples[0]));
        Assert.assertTrue("The collection should contain tuples[2]", read2.contains(tuples[2]));
        Assert.assertTrue("The collection should contain tuples[3]", read2.contains(tuples[3]));
        Assert.assertTrue("The collection should contain tuples[4]", read2.contains(tuples[4]));

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
        Assert.assertEquals("2 tuples should have been read", 2, read2.size());
        Assert.assertTrue("The collection should contain tuples[3]", read2.contains(tuples[2]));
        Assert.assertTrue("The collection should contain tuples[4]", read2.contains(tuples[3]));

        Assert.assertTrue("The collection should be empty",
                linda.takeAll(new Tuple(Object.class)).isEmpty());
    }

    @Test(timeout = 5000)
    public void testReadTakeSynchronized() {
        Tuple template1 = new Tuple(Integer.class, Integer.class, String.class, Boolean.class);
        Tuple test1 = new Tuple(683, 94763, "coucou", true);
        Tuple template2 = new Tuple(Tuple.class, String.class);
        Tuple test2 = new Tuple(new Tuple(4683, 98734), "chaine");

        new Thread(() -> {
            Tuple tuples[] = new Tuple[] {
                    new Tuple(573, "lala"),
                    test1,
                    new Tuple(false),
                    new Tuple(4673, new Tuple(false, true))
            };
            try {
                Thread.sleep(1000);

                for (Tuple tuple : tuples) {
                    linda.write(tuple);
                }
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }).start();

        Tuple tupleRead = linda.read(template1);
        Assert.assertTrue("The tuple read should match 683|94763|'coucou'|true", tupleRead.matches(test1));

        new Thread(() -> {
            Tuple tuples[] = new Tuple[] {
                    new Tuple(794873, "lala"),
                    test2,
                    new Tuple(false),
                    new Tuple(false, new Tuple(342, true))
            };
            try {
                Thread.sleep(1500);

                for (Tuple tuple : tuples) {
                    linda.write(tuple);
                }
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }).start();

        Tuple tupleTaken = linda.take(template2);
        Assert.assertTrue("The tuple taken should match (4683|98734)|'chaine'", tupleTaken.matches(test2));
    }

    @Test
    public void testEvents() {
        int expectedReadImmediate = 3, expectedTakeImmediate = 2, expectedReadFuture = 1, expectedTakeFuture = 1;

        readImmediate = 0;
        takeImmediate = 0;
        readFuture = 0;
        takeFuture = 0;

        Tuple tuplesImmediate[] = new Tuple[] {
                new Tuple(345, 762, "coucou", true),
                new Tuple(53782, 65442, "allo", "test"),
                new Tuple(345, 762, 7863853, false),
                new Tuple(53782, 65442, new ArrayList<>(), true),
                new Tuple(345, "test", "coucou", true),
                new Tuple("allo", 65442, "coucou", true)
        };

        for (Tuple tuple : tuplesImmediate) {
            linda.write(tuple);
        }

        // Tests for Immediate/Read
        for (int i = 0; i < 2; i++) {
            linda.eventRegister(Linda.eventMode.READ, Linda.eventTiming.IMMEDIATE,
                    new Tuple(Integer.class, Integer.class, String.class, Boolean.class),
                    new AsynchronousCallback(t -> {
                        if (t.matches(tuplesImmediate[0])) {
                            readImmediate++;
                        }
                    })
            );
        }

        Tuple tupleImmediateRead = new Tuple(345, 762, "coucou", 43722);
        linda.eventRegister(Linda.eventMode.READ, Linda.eventTiming.IMMEDIATE,
                new Tuple(Integer.class, Integer.class, String.class, Integer.class),
                new AsynchronousCallback(t -> {
                    if (t.matches(tupleImmediateRead)) {
                        readImmediate++;
                    }
                })
        );
        try {
            Thread.sleep(500);
            linda.write(tupleImmediateRead);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        // Tests for Immediate/Take
        for (int i = 0; i < 2; i++) {
            linda.eventRegister(Linda.eventMode.TAKE, Linda.eventTiming.IMMEDIATE,
                    new Tuple(Integer.class, Integer.class, String.class, String.class),
                    new AsynchronousCallback(t -> {
                        if (t.matches(tuplesImmediate[1])) {
                            takeImmediate++;
                        }
                    })
            );
        }

        Tuple tupleImmediateWrite = new Tuple(345, 762, 43722);
        linda.eventRegister(Linda.eventMode.READ, Linda.eventTiming.IMMEDIATE,
                new Tuple(Integer.class, Integer.class, Integer.class),
                new AsynchronousCallback(t -> {
                    if (t.matches(tupleImmediateWrite)) {
                        takeImmediate++;
                    }
                })
        );
        try {
            Thread.sleep(500);
            linda.write(tupleImmediateRead);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        // Tests for Future/Read
        Tuple tupleFutureRead = new Tuple(345, 9543, "coucou", true);
        linda.eventRegister(Linda.eventMode.READ, Linda.eventTiming.FUTURE,
                new Tuple(Integer.class, Integer.class, String.class, Boolean.class),
                new AsynchronousCallback(t -> {
                    if (t.matches(tupleFutureRead)) {
                        readFuture++;
                    }
                })
        );
        try {
            Thread.sleep(500);
            linda.write(tupleFutureRead);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        Assert.assertNotNull("Event for Future/Read shouldn't destroy the tuple",
                linda.tryRead(tupleFutureRead));

        // Tests for Future/Take
        Tuple tupleFutureTake = new Tuple(34543, 76322, 7853, false, true);
        linda.eventRegister(Linda.eventMode.TAKE, Linda.eventTiming.FUTURE,
                new Tuple(Integer.class, Integer.class, Integer.class, Boolean.class, Boolean.class),
                new AsynchronousCallback(t -> {
                    if (t.matches(tupleFutureTake)) {
                        takeFuture++;
                    }
                })
        );
        try {
            Thread.sleep(500);
            linda.write(tupleFutureTake);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        Assert.assertNull("Event for Future/Take should destroy the tuple", linda.tryRead(tupleFutureTake));

        // Checks
        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        Assert.assertEquals("Immediate/Read is not working correctly",
                expectedReadImmediate, readImmediate);
        Assert.assertEquals("Immediate/Take is not working correctly",
                expectedTakeImmediate, takeImmediate);
        Assert.assertEquals("Future/Read is not working correctly",
                expectedReadFuture, readFuture);
        Assert.assertEquals("Future/Take is not working correctly",
                expectedTakeFuture, takeFuture);
    }

    public static void main(String[] args) {
        org.junit.runner.JUnitCore.main(TestsElementairesCentralized.class.getName());
    }
}
