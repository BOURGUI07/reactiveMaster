package com.example.reactivemaster.sec05;

import com.example.reactivemaster.common.Util;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Flux;
import reactor.core.scheduler.Schedulers;

@Slf4j
public class publishOn {
    public static void main(String[] args) {
        demo1();
    }

    private static void demo1() {
        var flux = Flux.create(sink->{
                    for(int i=1;i<=3;i++){
                        log.info("GENERATING: {}", i);
                        sink.next(i);
                    }
                    sink.complete();
                })
                .doOnNext(value-> log.info("VALUE: {}", value))
                .doFirst(()-> log.info("FIRST1"))
                .publishOn(Schedulers.boundedElastic())
                .doFirst(()-> log.info("FIRST2"));

        Runnable runnable = () -> flux.subscribe(Util.subscriber());
        Thread.ofPlatform().start(runnable);
        Util.sleep(2);
    }
}
