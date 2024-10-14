package com.example.reactivemaster.sec05;

import com.example.reactivemaster.common.Util;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Flux;
import reactor.core.scheduler.Schedulers;

@Slf4j
public class MultipleSubscribeOn {
    public static void main(String[] args) {
        demo1();
    }
    private static void demo1(){
        var flux = Flux.create(fluxSink -> {
                    for (int i = 1; i <= 3; i++) {
                        log.info("GENERATING :{}",i);
                        fluxSink.next(i);
                    }
                    fluxSink.complete();
                })
                .subscribeOn(Schedulers.newParallel("vins-1"))
                .doOnNext(x->log.info("VALUE: {}",x))
                .doFirst(() ->log.info("FIRST1"))
                .subscribeOn(Schedulers.boundedElastic())
                .doFirst(() ->log.info("FIRST2"));

        Runnable runnable1 = () -> flux
                .subscribe(Util.subscriber("sub1"));

        Thread.ofPlatform().start(runnable1);

        Util.sleep(2);

        /*
            11:24:06.740 [Thread-0] INFO com.example.reactivemaster.sec05.MultipleSubscribeOn -- FIRST2
11:24:06.757 [boundedElastic-1] INFO com.example.reactivemaster.sec05.MultipleSubscribeOn -- FIRST1
11:24:06.772 [vins-1-1] INFO com.example.reactivemaster.sec05.MultipleSubscribeOn -- GENERATING :1
11:24:06.772 [vins-1-1] INFO com.example.reactivemaster.sec05.MultipleSubscribeOn -- VALUE: 1
11:24:06.772 [vins-1-1] INFO com.example.reactivemaster.common.DefaultSub -- sub1 RECEIVED ITEM: 1
11:24:06.772 [vins-1-1] INFO com.example.reactivemaster.sec05.MultipleSubscribeOn -- GENERATING :2
11:24:06.772 [vins-1-1] INFO com.example.reactivemaster.sec05.MultipleSubscribeOn -- VALUE: 2
11:24:06.772 [vins-1-1] INFO com.example.reactivemaster.common.DefaultSub -- sub1 RECEIVED ITEM: 2
11:24:06.772 [vins-1-1] INFO com.example.reactivemaster.sec05.MultipleSubscribeOn -- GENERATING :3
11:24:06.772 [vins-1-1] INFO com.example.reactivemaster.sec05.MultipleSubscribeOn -- VALUE: 3
11:24:06.772 [vins-1-1] INFO com.example.reactivemaster.common.DefaultSub -- sub1 RECEIVED ITEM: 3
11:24:06.787 [vins-1-1] INFO com.example.reactivemaster.common.DefaultSub -- sub1 RECEIVED COMPLETED
         */
    }


}
