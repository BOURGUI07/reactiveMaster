package com.example.reactivemaster.sec02;

import com.example.reactivemaster.common.Util;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Mono;

import java.util.List;

@Slf4j
public class fromSupplier {
    public static void main(String[] args) {
        demo3();
    }

    private static int sum(List<Integer> list) {
        log.info("STARTING THE PROCESS OF SUM CALCULATION");
        return list.stream().mapToInt(x -> x).sum();
    }

    private static void demo1(){
        Mono.just(sum(List.of(1,2,3)));
        /*
            It will start the process of sum calculation regardless of whether
            there's a subscriber or not.
         */
    }

    private static void demo2(){
        Mono.fromSupplier(() ->sum(List.of(1,2,3)));
        /*
            Unless there's a subscriber, it won't start the process of sum calculation
         */
    }

    private static void demo3(){
        Mono.fromSupplier(() ->sum(List.of(1,2,3)))
                .subscribe(Util.subscriber());
        /*
            Now it will start the process and publish the result.
         */
    }
}
