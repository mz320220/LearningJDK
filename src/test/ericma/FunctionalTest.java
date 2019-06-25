package test.ericma;

import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;

public class FunctionalTest {
    public static void main(String[] args) {

    }

    //1、Function<T,R>:接受一个参数T，返回一个结果R：apply执行程序
    private static int modifyTheValue(int param, Function<Integer,Integer> function){
        return function.apply(param);
    }

    //2、Function<T,R> - andThen，先执行function1.apply(param),结果作为参数执行function2.apply()
    private static int mofidyTestAndThen(int param, Function<Integer,Integer> function1, Function<Integer,Integer> function2){
        return function1.andThen(function2).apply(param);
    }

    //3、Consumer<T>:接受一个参数T，无返回操作
    private static void modifyTestConsumer(int param, Consumer<Integer> consumer){
        consumer.accept(param);
    }

    //4、Predicate<T>:断言型函数接口，接受一个参数T，返回boolen类型结果
    private static boolean testPredicate(int param, Predicate<Integer> predicate){
        return predicate.test(param);
    }

    //5、Supplier<T>:无参数，返回一个结果,举例： () -> new Integer(10);
}


