package futures;

public class Example {
  static int staticVar = initializeStaticVar();

  static {
    System.out.println("Static Block 1");
  }

  static {
    System.out.println("Static Block 2");
  }

  static int initializeStaticVar() {
    System.out.println("Static Variable Initialization");
    return 10;
  }

  public static void main(String[] args) {
    System.out.println("Hello world");
  }
}
