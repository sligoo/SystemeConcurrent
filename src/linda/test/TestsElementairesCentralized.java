package linda.test;

import linda.Linda;
import linda.Tuple;
import linda.shm.CentralizedLinda;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import sun.net.www.ApplicationLaunchException;

import java.util.Date;

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
    public void testWrite() {
        Tuple tupleWritten = new Tuple("coucou", 5, 3, false);
        linda.write(tupleWritten);
        Tuple tupleRead = linda.tryRead(new Tuple(String.class, Integer.class,
                    Integer.class, Boolean.class));
        assertTrue(tupleRead.matches(tupleWritten));

        tupleWritten = new Tuple(new Tuple("shittyFlute", 3456), "troubalourds",
                "japan7", true);
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

    @Test
    public void testTake(){
        Tuple tupleWritten = new Tuple(42);
        linda.write(tupleWritten);
        assertTrue("Take a value tuple template", linda.take
                (new Tuple(42)).matches(tupleWritten));
        assertNull("After a take, tuple is empty",linda.tryRead(tupleWritten));

    }

    @Test
    public void testTakeLock() throws InterruptedException {
        Tuple tupleWritten = new Tuple("empty");
        linda.write(tupleWritten);
        Thread mainRunner = new Thread(() -> {
            linda.take(new Tuple(5));
        });

        mainRunner.start();

        Thread.sleep(1000);

        assertEquals(Thread.State.WAITING, mainRunner.getState());
        mainRunner.interrupt();

    }

    @Test
    public void testReadLock() throws InterruptedException {
        Tuple tupleWritten = new Tuple("empty");
        linda.write(tupleWritten);
        Thread mainRunner = new Thread(() -> {
            linda.read(new Tuple(5));
        });

        mainRunner.start();

        Thread.sleep(1000);

        assertEquals(Thread.State.WAITING, mainRunner.getState());
        mainRunner.interrupt();

    }

    @Test
    public void testRead(){
        Tuple tupleWritten = new Tuple(42);
        linda.write(tupleWritten);
        assertTrue("Read a value tuple template", linda.read(new Tuple(42)).matches(tupleWritten));
        assertTrue("After a read, tuple is as before", linda.tryRead(tupleWritten)
                .matches(tupleWritten));

    }


}
