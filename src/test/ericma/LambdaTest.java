package test.ericma;

public class LambdaTest {
    public static void main(String[] args) {
        LambdaTest tester = new LambdaTest();
        MathOperation addOpr = (a, b) -> a + b;
        MathOperation subOpr = (int a, int b) -> a - b;
        MathOperation multOpr = (a, b) -> {return a * b; };
        MathOperation divOpr = (a, b) -> a/b;

        System.out.println(tester.operate(10, 5, addOpr));
        System.out.println(tester.operate(10, 5, subOpr));

        GreetingService greetingService = message -> System.out.println(message + " Hello!");

        greetingService.sayMessage("LambaTest");

        final int num =1;
        Converter<Integer, String> con = a -> System.out.println(a + num);
        con.convert(2);
    }

    interface MathOperation{
        int operateion(int a, int b);
    }

    interface GreetingService{
        void sayMessage(String message);
    }

    interface Converter<T1,T2>{
        void convert(int i);
    }

    private int operate(int a, int b, MathOperation operationType){
        return operationType.operateion(a, b);
    }
}
