package test.ericma;

import com.sun.xml.internal.ws.util.CompletedFuture;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.*;
import java.util.stream.Collectors;

public class CompletableFutureTest {
    public static void main(String[] args) {
        CompletableFutureTest test = new CompletableFutureTest();
        //test.completableFutureExample();
        //test.runAsyncExample();
        //test.thenApplyExample();
        //test.thenApplyAsyncExample();
        //test.thenApplyAsynWithExecutorExample();
        //test.thenAcceptExample();
        //test.thenAcceptAsyncExample();
        //test.completeExceptionallyExample();
        //test.cancelExample();
        //test.applyToEitherExample();
        //test.acceptEitherExample();
        //test.runAfterBothExample();
        //test.thenAcceptBothExample();
        //test.thenCombineExample();
        //test.thenCombineAysncExample();
        //test.thenComposeExample();
        //test.anyOfExample();
        //test.allOfExample();
        test.allOfAysncExample();
    }

    //1、创建一个完成的CompletableFuture
    static void completableFutureExample() {
        CompletableFuture cf = CompletableFuture.completedFuture("message");
        System.out.println(cf.isDone());
        System.out.println(cf.getNow(null).equals("message"));
    }

    //2、运行一个简单的异步阶段
    static void runAsyncExample() {
        CompletableFuture cf = CompletableFuture.runAsync(() -> {
            //异步执行通过ForkJoinPool实现，使用守护线程执行任务：true
            System.out.println("isDaemon(): " + Thread.currentThread().isDaemon());
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        });
        System.out.println("1-cf.isDone(): " + cf.isDone());
        try {
            Thread.sleep(3000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        System.out.println("2-cf.isDone(): " + cf.isDone());
    }

    //3、测试thenApply：then-发生在正常阶段完成之后；apply-返回阶段会对前一阶段的结果进行处理
    static void thenApplyExample() {
        CompletableFuture cf = CompletableFuture.completedFuture("message").thenApply(s -> {
            System.out.println("is Daemon: " + Thread.currentThread().isDaemon());
            return s.toUpperCase();
        });
        //thenApply为阻塞操作，getNow必定在大写操作后
        System.out.println(cf.getNow(null));
    }

    //4、上个例子thenApply为阻塞操作。可以异步的执行：
    static void thenApplyAsyncExample() {
        CompletableFuture cf = CompletableFuture.completedFuture("message").thenApplyAsync(s -> {
            System.out.println("is Daemon: " + Thread.currentThread().isDaemon());
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            return s.toUpperCase();
        });
        //getNow:立即获取结果，无则返回默认值。get：等待处理直到正常返回or异常抛出
        System.out.println("getNow:" + cf.getNow(null));
        try {
            System.out.println("get:" + cf.get());
        } catch (Exception e) {
            e.printStackTrace();
        }
        //join基本和get含义相同，对于exception的处理略有区别
        System.out.println("join:" + cf.join());
    }

    //5、定制一个executor执行thenApplyAsync
    static ExecutorService executor = Executors.newFixedThreadPool(3, new ThreadFactory() {
        int count = 1;

        @Override
        public Thread newThread(Runnable r) {
            return new Thread(r, "custom-executor-" + count++);
        }
    });

    static void thenApplyAsynWithExecutorExample() {
        CompletableFuture cf = CompletableFuture.completedFuture("message").thenApplyAsync(s -> {
            //自定义的executor，非ForJoinPool，所以不是守护线程来执行
            System.out.println("is Daemon: " + Thread.currentThread().isDaemon());
            System.out.println("Thread name: " + Thread.currentThread().getName());
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            return s.toUpperCase();
        }, executor);
        System.out.println("getNow: " + cf.getNow(null));
        System.out.println("join: " + cf.join());
    }

    //6、消费前一阶段的结果，并不返回，那thenApply -->thenAccept，变成一个消费者
    static void thenAcceptExample() {
        StringBuffer stringBuffer = new StringBuffer();
        CompletableFuture cf = CompletableFuture.completedFuture("thenAccpet message").thenAccept(s -> {
            stringBuffer.append(s);
        });
        //同步执行：所以可以立即取到结果
        System.out.println("strBuf: " + stringBuffer);
        //并没有返回结果，所以get、join并不能取到值
        System.out.println(cf.getNow(null));
        System.out.println("join:" + cf.join());
    }

    //7、异步消费前一阶段结果
    static void thenAcceptAsyncExample() {
        StringBuffer strBuf = new StringBuffer();
        CompletableFuture cf = CompletableFuture.completedFuture("thenAcceptAsync message").thenAcceptAsync(s -> {
            strBuf.append(s);
        });
        System.out.println("1-result: " + strBuf);
        //异步执行，所以1-result为空，需要join等待执行完毕
        cf.join(); //cf.join本身是null没有返回结果的
        System.out.println("2-result: " + strBuf);
    }

    //8、完成计算异常:Completable.delayedExecutor-在Java9中引入
    static void completeExceptionallyExample() {
        CompletableFuture cf = CompletableFuture.completedFuture("message").thenApplyAsync(s -> {
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            return s.toUpperCase();
        });
        //handle：在原来的CompletableFuture也即cf“执行完毕”或“抛出异常”后触发，返回新的CompletableFuture
        CompletableFuture exceptionHandler = cf.handle((s, th) -> {
            return th != null ? "message upon cancel" : s;
        });
        /*
         * cf中thenApplyAsync异步执行，阻塞未完成大写转换cf已经强制抛出异常-->被exceptionHandler捕获
         * 如果注释掉抛出异常：cf.join正常执行，exceptionHandler获取到s也即转换后的字符串“MESSAGE”
         * */
        cf.completeExceptionally(new RuntimeException("completed exceptionally"));
        System.out.println(cf.isCompletedExceptionally());
        try {
            //这里join不会得到结果，上方已经抛出异常
            cf.join();
        } catch (Exception e) {
            //System.out.println("exception result: " + e.getCause().getMessage());
            System.out.println("exception result: " + e.getMessage());
        }
        //exceptionHandler捕获异常，th非null，故得到结果为“message upon cancel”
        System.out.println("exception join: " + exceptionHandler.join());
    }

    //9、取消计算
    static void cancelExample() {
        CompletableFuture cf = CompletableFuture.completedFuture("message").thenApplyAsync(s -> {
            try {
                Thread.sleep(1500);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            return s.toUpperCase();
        });
        CompletableFuture cf2 = cf.exceptionally(th -> "cancel message");

        //cancel等价于completeExceptionally(new CancellationException())
        System.out.println("cf cancel: " + cf.cancel(true));
        System.out.println("cf isCompletedExceptionally: " + cf.isCompletedExceptionally());

        //cf异常后cf2捕获：
        System.out.println("cf2 message: " + cf2.join());
        try {
            cf.get();
        } catch (Exception e) {
            System.out.println("exception type: " + e.getClass());
        }
    }

    //10、两个阶段完成之一则执行function
    static void applyToEitherExample() {
        String str = "Message";
        CompletableFuture cf1 = CompletableFuture.completedFuture(str).thenApplyAsync(s -> {
            //可以通过控制cf1是否立即执行得到不同的cf2.join结果
            /*try {
                Thread.sleep(1200);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }*/
            return s.toUpperCase();
        });
        //applyToEither当cf1或参数中other-CompletionStage中有一个执行完毕，则执行function:s->s+" from applyToEither"
        CompletableFuture cf2 = cf1.applyToEither(CompletableFuture.completedFuture(str).thenApplyAsync(String::toLowerCase),
                s -> s + " from applyToEither");
        System.out.println("cf2 result: " + cf2.join());
    }

    //11、两个CompletionStage完成后则执行cconsumer
    static void acceptEitherExample() {
        String str = "Message";
        CompletableFuture cf = CompletableFuture.completedFuture(str)
                .thenApplyAsync(s -> {
                    /*try {
                        Thread.sleep(1000);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }*/
                    return s.toUpperCase();
                })
                .acceptEither(CompletableFuture.completedFuture(str).thenApplyAsync(String::toLowerCase),
                        //这里不可以用s-> s + " from applyToEither"，有返回值不符合consumer规定
                        System.out::println);
    }

    //12、两个阶段都执行完毕后运行Runnable
    static void runAfterBothExample() {
        String str = "Message";
        StringBuffer stringBuffer = new StringBuffer();
        CompletableFuture.completedFuture(str).thenApply(String::toUpperCase).runAfterBoth(
                CompletableFuture.completedFuture(str).thenApply(String::toLowerCase),
                //Runnable函数式接口：不接收参数 && 不返回参数
                () -> stringBuffer.append("done"));
        System.out.println(stringBuffer);
    }

    //13、两个阶段执行完毕后运行BiConsumer：接收两个参数，无返回结果
    static void thenAcceptBothExample() {
        String str = "Message";
        StringBuffer strBuf = new StringBuffer();
        CompletableFuture.completedFuture(str).thenApply(String::toUpperCase).thenAcceptBoth(
                CompletableFuture.completedFuture(str).thenApply(String::toLowerCase),
                (s1, s2) -> strBuf.append(s1).append(s2));
        System.out.println("strBuf: " + strBuf);
    }

    //14、两个阶段执行完毕后运行BiFunction：接收两个参数，返回一个结果
    static void thenCombineExample() {
        String str = "Message";
        CompletableFuture cf = CompletableFuture.completedFuture(str).thenApply(String::toUpperCase).thenCombine(
                CompletableFuture.completedFuture(str).thenApply(String::toLowerCase),
                (s1, s2) -> s1 + s2);
        //上面使用thenApply非异步执行，直接getNow可以得到正确的结果
        System.out.println(cf.getNow(null));
    }

    //15、异步执行并通过BiFunction整合两个结果
    static void thenCombineAysncExample() {
        String str = "Message";
        CompletableFuture cf = CompletableFuture.completedFuture(str).thenApplyAsync(s -> {
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            return s.toUpperCase();
        }).thenCombine(
                CompletableFuture.completedFuture(str).thenApplyAsync(String::toLowerCase),
                (s1, s2) -> s1 + s2);
        //由于thenApplyAsyn异步且存在等待，即使不使用thenCombineAysnc结果依然是异步的
        System.out.println(cf.getNow(null));
        System.out.println(cf.join());
    }

    //16、组合CompletableFuture
    static void thenComposeExample() {
        String str = "Message";
        CompletableFuture cf = CompletableFuture.completedFuture(str).thenApply(String::toUpperCase)
                //upstr为转换后的MESSAGE
                .thenCompose(upstr -> CompletableFuture.completedFuture(str)
                        .thenApply(String::toLowerCase)
                        //s为thenApply转换后的message，拼接的到结果messageMESSAGE
                        .thenApply(s -> s + upstr));
        System.out.printf("result: " + cf.join());
    }

    //17、当几个阶段中的一个完成，创建一个完成CompletableFuture
    static void anyOfExample() {
        StringBuffer stringBuffer = new StringBuffer();
        List<String> message = Arrays.asList("a", "b", "c");
        List<CompletableFuture> futures = message.stream()
                .map(str -> CompletableFuture.completedFuture(str)
                        .thenApply(String::toUpperCase))
                .collect(Collectors.toList());
        CompletableFuture.anyOf(futures.toArray(new CompletableFuture[futures.size()]))
                .whenComplete((res,th)->{
                    if(th ==null){
                        System.out.println("result:" + res);
                        stringBuffer.append(res);
                    }
                });
        System.out.printf("stringBuffer: " + stringBuffer);

    }

    //18、当所有阶段完成后
    static void allOfExample(){
        StringBuffer stringBuffer = new StringBuffer();
        List<String> messages = Arrays.asList("a","b","c");
        List<CompletableFuture> futures = messages.stream()
                .map(str -> CompletableFuture.completedFuture(str)
                        .thenApply(String::toUpperCase))
                .collect(Collectors.toList());
        CompletableFuture.allOf(futures.toArray(new CompletableFuture[futures.size()]))
                .whenComplete((param,th)->{
                    futures.forEach(cf ->{
                            System.out.println(cf.getNow(null));
                            stringBuffer.append(cf.getNow(null));
                            }
                    );
                    stringBuffer.append(" end");
                });
        System.out.println("stringBuffer"+stringBuffer);
    }

    //19、当所有的阶段都完成后，异步创建一个阶段
    static void allOfAysncExample(){
        StringBuffer strBuf = new StringBuffer();
        List<String> messages = Arrays.asList("a","b","c");
        /*List<CompletableFuture> futures = messages.stream()
                .map(msg -> CompletableFuture.completedFuture(msg)
                        .thenApplyAsync(String::toUpperCase))
                .collect(Collectors.toList());*/
        List<CompletableFuture> futures =  messages.stream()
                .map((msg) -> CompletableFuture.supplyAsync(() -> {return msg.toUpperCase();}))
                .collect(Collectors.toList());
        CompletableFuture allOf = CompletableFuture.allOf(futures.toArray(new CompletableFuture[futures.size()]))
                .whenComplete((v,th)->{
                    futures.forEach(cf -> System.out.println(cf.getNow(null)));
                    strBuf.append(" done");
                });
        allOf.join();
        System.out.println(strBuf);
    }
}

