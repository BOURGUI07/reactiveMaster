package com.example.reactivemaster.sec02;

import com.example.reactivemaster.common.Util;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Mono;

import java.util.List;

@Slf4j
public class fromCallable {
    public static void main(String[] args) {
        demo1();
    }

    private static int sum(List<Integer> list) throws Exception {
        log.info("STARTING THE PROCESS OF SUM CALCULATION");
        return list.stream().mapToInt(x -> x).sum();
    }

    private static void demo1() {
        Mono.fromCallable(() ->sum(List.of(1,2,3)))
                .subscribe(Util.subscriber());

        /*
            As you can see, eventhough the sum() method throws a checked exception
            the method of that calls fromCallable doesn't complain
            In case of calling fromSupplier, the method WILL complain!
         */

    }
}
