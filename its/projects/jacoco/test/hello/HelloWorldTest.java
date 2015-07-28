package hello;

public class HelloWorldTest extends junit.framework.TestCase {

  public void testWillIncreaseCoverage() {
    new HelloWorld().isPositive(0);
  }

  public void testWillAlwaysFail() {
    fail("An error message");
  }

}
