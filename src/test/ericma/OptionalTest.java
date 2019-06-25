package test.ericma;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.function.BinaryOperator;
import java.util.function.Supplier;

public class OptionalTest {

    public static void main(String[] args) {
        OptionalTest simple = new OptionalTest();
//        Integer sum = simple.sum(Optional.of(new Integer(10)), Optional.ofNullable(null));
//        System.out.println(sum);
        simple.compareOrElseGet();
    }

    public Integer sum(Optional<Integer> a, Optional<Integer> b){
        List<Integer> testList = Arrays.asList(123,234,352);
        Integer result = testList.stream().reduce(Integer::sum).get();
        System.out.println("test reduce: " + result);
        System.out.println("a is present: " +a.isPresent());
        System.out.println("b is present: " +b.isPresent());
        Integer aValue = a.orElse(new Integer(10));
        Integer bValue = b.orElseGet(new Supplier<Integer>() {
            @Override
            public Integer get() {
                return new Integer(0);
            }
        });
        //供给型函数式接口：无参数，返回一个结果。可以替换为如下lambda表达式
        Integer cValue = b.orElseGet(()-> new Integer(123));
        System.out.println(cValue);
        return aValue + bValue;
    }

    //Optional中orElseGet()的延迟评估
    /*
    * 1、当orElse、orElseGet都有值时，orElse依然会执行，orElseGet不会执行；
    * 2、当orElse、orElseGet均为null时，两者都会执行
    * -->也即orElseGet只有在值为null时才评估，允许延迟评估
    * */
    public void compareOrElseGet(){
        List<Integer> list = Arrays.asList(10,20,30);
        int a = list.stream().reduce(Integer::sum).orElse(get("a"));
        int b = list.stream().reduce(Integer::sum).orElseGet(() ->get("b"));
        System.out.println("a: " + a);
        System.out.println("b: " + b);

        System.out.println(Optional.empty().orElse(get("null")));
        System.out.println(Optional.empty().orElseGet(() ->get("null")));
    }

    private static int get(String name){
        System.out.println(name+"执行了get方法");
        return 1;
    }
}
