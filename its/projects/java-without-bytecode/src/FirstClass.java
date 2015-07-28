public class FirstClass {
  enum MyEnum { // PMD will stop work on this line if Java version set to value lower than 1.5
  }

  public void foo() {
    new Integer(0); // violation will be here, if version of Java was configured
  }
}
