package com.example.reactivemaster.sec02;

import com.example.reactivemaster.common.Util;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Mono;

import java.util.concurrent.CompletableFuture;

@Slf4j
public class fromFuture {
    public static void main(String[] args) throws InterruptedException {
        demo2();
    }

    private static CompletableFuture<String> getName() {
        return CompletableFuture.supplyAsync(()->{
           log.info("GENERATING NAME");
           return Util.faker().name().fullName();
        });
    }

    private static void demo1() throws InterruptedException {
        Mono.fromFuture(getName())
                .subscribe(Util.subscriber());
        Util.sleep(2);

        /*
            Even without a subscriber, it will start the process of
            generating name
         */
    }

    private static void demo2() throws InterruptedException {
        Mono.fromFuture(()-> getName());
     //           .subscribe(Util.subscriber());
      //  Util.sleep(2);

        /*
            now it won't start the process
         */
    }
}
