package hello;

import org.testng.annotations.*;
import static org.testng.Assert.fail;

public class HelloWorldTest {

  @Test
  public void testWillIncreaseCoverage() {
    new HelloWorld().isPositive(0);
  }

  @Test
  public void testWillAlwaysFail() {
    fail("An error message");
  }

}
