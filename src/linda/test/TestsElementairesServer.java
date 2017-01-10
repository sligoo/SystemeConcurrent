package linda.test;

import linda.AsynchronousCallback;
import linda.Linda;
import linda.Tuple;
import linda.shm.CentralizedLinda;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Collection;

import static org.junit.Assert.*;

/**
 * Project: SystemesConcurrents
 * Created by sacha on 30/11/2016.
 */
public class TestsElementairesServer {

  private Linda linda;
  private int readImmediate, takeImmediate, readFuture, takeFuture;

  @Before
  public void initialize() {
    linda = new CentralizedLinda();
  }

  @Test
  public void testALaCon() {
  }

  public static void main(String[] args) {
    org.junit.runner.JUnitCore.main(TestsElementairesServer.class.getName());
  }
}
