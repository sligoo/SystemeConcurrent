package linda.test;

import linda.Linda;
import linda.Tuple;
import linda.shm.CentralizedLinda;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.util.Date;

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
        Tuple tupleRead = linda.tryRead(new Tuple(String.class, Integer.class, Integer
                .class, Boolean.class));
        Assert.assertTrue(tupleRead.matches(tupleWritten));

        tupleWritten = new Tuple(new Tuple("shittyFlute", 3456), "troubalourds",
                "japan7", true);
    }


}
