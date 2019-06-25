package test.ericma;

import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.stream.Collector;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static java.util.stream.Collectors.mapping;
import static java.util.stream.Collectors.toSet;

public class StreamTest {
    public static void main(String[] args) {
        StreamTest test = new StreamTest();
        //test.testConcurrentException();
        //test.testMiddleAdd();
        //test.testConcurrent();
        //test.testReductionOperation();
        test.testReduceOperation();



        //test.testReduceDiff();
        //test.testMutableReduction();
    }

    static void testConcurrentException() {
        List<String> list = new LinkedList<>(Arrays.asList("a", "b", "c"));
        Stream<String> stream = list.stream();
        //当流“的管道”执行时，非concurrent的数据源不可以被改变。
        stream.forEach(s -> list.add("test"));
    }

    static void testMiddleAdd() {
        //tips:Arrays.aslist直接返回的是java.util.Arrays.ArrayList,继承来自AbstractList，add方法直接抛出UnSupport异常
        List<String> list = new LinkedList<>(Arrays.asList("a", "b", "c"));
        Stream<String> stream = list.stream();
        //中间操作，stream此时尚未执行终操作
        list.add("test");
        //中间操作对数据源的修改会反射到stream流当中：输出的最终结果是包含test字段的
        stream.forEach(System.out::println);
    }

    static void testConcurrent() {
        List<String> list = new CopyOnWriteArrayList<>(Arrays.asList("a", "b", "c"));
        Stream<String> streamList = list.stream();
        //线程安全的数据源是可以再stream终操作中进行修改的,然而内容不会反射到stream中
        streamList.forEach(s -> {
            list.add("test");
            System.out.printf(s);
        });
    }

    static void testReductionOperation() {
        List<Integer> numbers = Arrays.asList(12, 23, 65, 34);
        //identity：循环计算的初始值 & 无结果的默认值
        int sum = numbers.stream().reduce(0, (x, y) -> x + y);
        int sum1 = numbers.stream().reduce(0, Integer::sum);
        System.out.printf("sum: " + sum);
    }

    static void testReduceOperation() {
        List<Integer> nums = Arrays.asList(1, 2, 3, 4, 5);
        int sum1 = nums.stream().reduce(5, (x, y) -> x + y);
        int sum11 = nums.stream().reduce(5, Integer::sum);
        int sum2 = nums.parallelStream().reduce(5, (x, y) -> x + y);
        int sum22 = nums.parallelStream().reduce(5, Integer::sum);
        //sum1:20,sum2:40,原因是并行处理为多个线程分别处理identity+param
        System.out.println("sum1: " + sum1); // -> ((((5+1)+2)+3)+4)+5
        System.out.println("sum11: " + sum11);
        System.out.println("sum2: " + sum2);// -> (5+1)+(5+2)+(5+3)+(5+4)+(5+5)
        System.out.println("sum22: " + sum22);
    }

    static void testSpliterator(){

    }






    //测试入参与结果不同的情况:只有reduce三参数的方法可以支持
    /*
    1、一个参数返回的为optional，需要get结果，入参与结果保持一致。
    2、两个参数的identity为初始值，accumulator累加器的参数、结果均需要和identity一致。（见上一个方法）
    3、三参数中identity为初始值，accumulator累加器第一个参数也即结果和identity一致，第二个参数可为其他类型，本例子中为String
        combiner：拼接器在并行时才会用到，为了合并各个线程执行的结果，并作出声明两个入参和出参必须一致:U->BinaryOperator<U>
    * */
    static void testReduceDiff() {
        List<String> strings = Arrays.asList("abc", "test", "apple");

        int result = strings.stream().reduce(0,
                (sum, param) -> sum + param.length(),
                (a, b) -> a + b);
        System.out.printf("result: " + result);
    }

    static void testMutableReduction() {
        Employee person1 = new Employee(1000, "201");
        Employee person2 = new Employee(5000, "201");
        Employee person3 = new Employee(15000, "202");
        Employee person4 = new Employee(6000, "202");
        List<Employee> employees = new ArrayList<>();
        employees.add(person1);
        employees.add(person2);
        employees.add(person3);
        employees.add(person4);
        //返回一个Collector<T, ?, Integer>：T入参-Employee，？-执行函数-getSalary，Integer-返回参数类型（getSalary返回salary为Integer类型）
        Collector<Employee, ?, Integer> summingSalaries = Collectors.summingInt(Employee::getSalary);
        /****************************
         Collectors.groupingBy接收两个参数：先分类->执行Collector函数
         入参：1、Function<? super T, ? extends K> classifier：分类器, T-Employee, K-String(department)
         2、Collector<? super T, A, D> downstream：下游Collector, T-Employee, A-getSalary, D-Integer(salary)
         ——最终调用的是：groupingBy(classifier, HashMap::new, downstream);
         出参：Collector<T, ?, Map<K, D>>
         collect函数：
         入参：Collector<? super T, A, R>
         出参：R，也即上面的Map<K,D> -> Map<String, Integer>
         *****************************/
        Map<String, Integer> salariesByDept = employees.stream().collect(Collectors.groupingBy(Employee::getDepartment, summingSalaries));
        //Collector<Employee,?,Map<String,Integer>> collector = Collectors.groupingBy(Employee::getDepartment, summingSalaries);
        /**************************
         Collectors.groupingBy接收一个参数：
         入参：Function<? super T, ? extends K> classifier：分类器器， T-Employee，K-String(department)
         阅读源码可知最终调用的是：groupingBy(classifier, HashMap::new, downstream);
         出参：Collector<T, ?, Map<K, List<T>>>, T-Employee, K-String -> Collector<Employee,?,Map<String,List<Employee>> >
         collect函数执行得到结果类型为Map<String,List<Employee>>
         *************************/
        Map<String, List<Employee>> result = employees.stream().collect(Collectors.groupingBy(Employee::getDepartment));
        System.out.println("salariesByDept: " + salariesByDept);
        result.entrySet().forEach(entry -> {
            System.out.println("entryKey: " + entry.getKey());
            entry.getValue().forEach(employee -> {
                System.out.println("employe-depart: " + employee.getDepartment());
                System.out.println("employe-salary: " + employee.getSalary());
            });
        });
        //测试Collectors mapping & toSet函数
        Map<String, Set<Integer>> testMapping = employees.stream().collect(
                Collectors.groupingBy(Employee::getDepartment, mapping(Employee::getSalary, toSet())));
        System.out.println("testMapping: " + testMapping);
    }




}
