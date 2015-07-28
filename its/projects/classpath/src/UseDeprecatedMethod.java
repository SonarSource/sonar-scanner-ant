public class UseDeprecatedMethod {
  public void use() {
    new DeprecatedExample().deprecatedMethod(); // violation will be here, if libraries were analysed by Sonar
    System.exit(33); // violation on findbugs rule: DM_EXIT
  }
}
