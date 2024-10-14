package com.example.reactivemaster.sec05;

import com.example.reactivemaster.common.Util;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Flux;
import reactor.core.scheduler.Schedulers;

@Slf4j
public class virtualThreads {
    public static void main(String[] args) {
        demo2();
    }

    private static void demo1(){
        var flux = Flux.create(fluxSink -> {
                    for (int i = 1; i <= 3; i++) {
                        log.info("GENERATING :{}",i);
                        fluxSink.next(i);
                    }
                    fluxSink.complete();
                })
                .doOnNext(x->log.info("VALUE: {}",x))
                .doFirst(() ->log.info("FIRST1, {}", Thread.currentThread().isVirtual() ))
                .subscribeOn(Schedulers.boundedElastic())
                .doFirst(() ->log.info("FIRST2"));

        Runnable runnable1 = () -> flux
                .subscribe(Util.subscriber("sub1"));

        Thread.ofPlatform().start(runnable1);

        Util.sleep(2);

        /*
            11:32:13.835 [Thread-0] INFO com.example.reactivemaster.sec05.virtualThreads -- FIRST2
11:32:13.853 [boundedElastic-1] INFO com.example.reactivemaster.sec05.virtualThreads -- FIRST1, false
11:32:13.871 [boundedElastic-1] INFO com.example.reactivemaster.sec05.virtualThreads -- GENERATING :1
11:32:13.871 [boundedElastic-1] INFO com.example.reactivemaster.sec05.virtualThreads -- VALUE: 1
11:32:13.871 [boundedElastic-1] INFO com.example.reactivemaster.common.DefaultSub -- sub1 RECEIVED ITEM: 1
11:32:13.871 [boundedElastic-1] INFO com.example.reactivemaster.sec05.virtualThreads -- GENERATING :2
11:32:13.871 [boundedElastic-1] INFO com.example.reactivemaster.sec05.virtualThreads -- VALUE: 2
11:32:13.871 [boundedElastic-1] INFO com.example.reactivemaster.common.DefaultSub -- sub1 RECEIVED ITEM: 2
11:32:13.871 [boundedElastic-1] INFO com.example.reactivemaster.sec05.virtualThreads -- GENERATING :3
11:32:13.871 [boundedElastic-1] INFO com.example.reactivemaster.sec05.virtualThreads -- VALUE: 3
11:32:13.871 [boundedElastic-1] INFO com.example.reactivemaster.common.DefaultSub -- sub1 RECEIVED ITEM: 3
11:32:13.882 [boundedElastic-1] INFO com.example.reactivemaster.common.DefaultSub -- sub1 RECEIVED COMPLETED
         */
    }

    private static void demo2(){
        System.getProperty("reactor.Schedulers.defaultBoundedElasticOnVirtualThreads", "true");
        var flux = Flux.create(fluxSink -> {
                    for (int i = 1; i <= 3; i++) {
                        log.info("GENERATING :{}",i);
                        fluxSink.next(i);
                    }
                    fluxSink.complete();
                })
                .doOnNext(x->log.info("VALUE: {}",x))
                .doFirst(() ->log.info("FIRST1, {}", Thread.currentThread().isVirtual() ))
                .subscribeOn(Schedulers.boundedElastic())
                .doFirst(() ->log.info("FIRST2"));

        Runnable runnable1 = () -> flux
                .subscribe(Util.subscriber("sub1"));

        Thread.ofPlatform().start(runnable1);

        Util.sleep(2);
    }
}
