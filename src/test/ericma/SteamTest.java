package test.ericma;

import java.util.Arrays;
import java.util.IntSummaryStatistics;
import java.util.List;
import java.util.Random;
import java.util.stream.Collectors;

public class SteamTest {
    public static void main(String[] args) {
        //SteamTest.testStream();
        //SteamTest.testParallel();
        //SteamTest.testCollectors();
        SteamTest.testStatistic();
    }

    private static void testStream(){
        List<String> strs = Arrays.asList("abc","","bc","def","","123");
        List<String> filterlist = strs.stream().filter(str -> !str.isEmpty()).collect(Collectors.toList());
        filterlist.forEach(System.out::println);
        System.out.println(filterlist.size());
        filterlist.stream().limit(2).forEach(System.out::println);
        Random random = new Random();
        random.ints().limit(10).sorted().forEach(System.out::println);
    }

    private static void testParallel(){
        List<String> strings = Arrays.asList("abc","","bc","def","","123");
        long count = strings.parallelStream().filter(str -> str.isEmpty()).count();
        System.out.println(count);
    }

    private static void testCollectors(){
        List<String> strings = Arrays.asList("abc","","abc","abc","","123");
        String mergeStr = strings.stream().filter(str -> !str.isEmpty()).collect(Collectors.joining());
        String mergeStr1 = strings.stream().filter(str -> !str.isEmpty()).collect(Collectors.joining("@"));
        String mergeStr2 = strings.stream().filter(str -> !str.isEmpty()).collect(Collectors.joining("*","@","&"));
        System.out.println("Merge Strings: "+ mergeStr);
        System.out.println("Merge Strings: "+ mergeStr1);
        System.out.println("Merge Strings: "+ mergeStr2);
    }

    private static void testStatistic(){
        List<Integer> nums = Arrays.asList(12,23,45,453,2234,3,0);
        IntSummaryStatistics stats = nums.stream().mapToInt(x -> x).summaryStatistics();
        System.out.println("Max："+stats.getMax());
        System.out.println("Min: "+stats.getMin());

    }
}
