package com.example.reactivemaster.sec02;

import com.example.reactivemaster.common.Util;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Mono;

import java.util.List;

@Slf4j
public class monoDefer {
    public static void main(String[] args)  {
        demo2();
    }

    private static Mono<String> getName(){
        log.info("ENTERING THE GET NAME METHOD");
        return Mono.fromSupplier(()->{
            log.info("GENERATING NAME");
            return Util.faker().name().fullName();
        });
    }

    private static void demo1(){
        getName().subscribe(Util.subscriber());

        /*
            Here it will create the publisher regardless of whether there's a sub or not
            but won't execute the publisher business logic unless there's a sub
            It will print ENTERING THE GET NAME METHOD even in the absence of a sub
         */
    }

    private static int sum(List<Integer> list)  {
        log.info("STARTING THE PROCESS OF SUM CALCULATION");
        Util.sleep(3);
        return list.stream().mapToInt(x -> x).sum();
    }

    private static Mono<Integer> createPublisher(){
        log.info("CREATING THE PUBLISHER");
        Util.sleep(1);
        var list = List.of(1,2,3,4);
        return Mono.fromSupplier(() ->sum(list));
    }

    private static void demo2(){
        Mono.defer(monoDefer::createPublisher);

        /*
            Unless there's a sub,
            This will neither create the publisher nor will execute it
         */
    }
}
