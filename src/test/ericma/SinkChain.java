package test.ericma;

import java.util.Arrays;
import java.util.Spliterator;
import java.util.function.Consumer;

public class SinkChain {
    public static void main(String[] args) {
        int[] source = {1,2,3,4,5};
        Spliterator spliterator = Arrays.stream(source).spliterator();

        Sink<Integer> sink0 = new Sink<>("source sink", null);
        Sink<Integer> sink4 = sink0.op("sink1").op("sink2").op("sink3").op("terminal sink");

        Sink wrappedSink = wrapSink(sink4);
        spliterator.forEachRemaining(wrappedSink);
    }

    public static Sink wrapSink(Sink sink){
        while(sink.upstream != null){
            sink.upstream.downstream = sink;
            sink = sink.upstream;
        }
        return sink;
    }

    static class Sink<T> implements Consumer<T>{
        private Sink upstream;
        private Sink downstream;
        private String name;

        public Sink(String name, Sink upstream){
            this.name = name;
            this.upstream = upstream;
        }

        public Sink op(String name){
            return new Sink(name,this);
        }

        @Override
        public void accept(T t) {
            System.out.println(name +" handles " + t);
            if(downstream != null){
                downstream.accept(t);
            }
        }
    }
}
