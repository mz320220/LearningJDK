package test.ericma;

import java.util.function.Consumer;

public class ConsumerTest {
    public static void main(String[] args) {
        Consumer<Integer> consumer = x -> {
            int num = x *2;
            System.out.println(num);
        };
        Consumer<Integer> consumer1 = x -> {
          System.out.println(x *3);
        };
        consumer.accept(2);
        consumer.andThen(consumer1).accept(10);
    }
}
