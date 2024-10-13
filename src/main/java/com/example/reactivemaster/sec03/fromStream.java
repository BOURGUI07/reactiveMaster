package com.example.reactivemaster.sec03;

import com.example.reactivemaster.common.Util;
import reactor.core.publisher.Flux;

import java.util.List;

public class fromStream {
    public static void main(String[] args) {
        demo2();
    }

    private static void demo1(){
        var list = List.of(1,2,3,4,5);
        var stream = list.stream();

        var flux = Flux.fromStream(stream);

        flux.subscribe(Util.subscriber("SAM"));
        flux.subscribe(Util.subscriber("TOM"));

        /*
            The stream can only be consumed once
            only SAM will be able to consume the stream
         */
    }

    private static void demo2(){
        var list = List.of(1,2,3,4,5);

        var flux = Flux.fromStream(list::stream);

        flux.subscribe(Util.subscriber("SAM"));
        flux.subscribe(Util.subscriber("TOM"));

        /*
            Now both SAM AND TOM will consume the stream
         */
    }
}
