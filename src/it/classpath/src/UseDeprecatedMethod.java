public class UseDeprecatedMethod {
  public void use() {
    new DeprecatedExample().deprecatedMethod(); // violation will be here, if libraries were analysed by Sonar
  }
}
