package com.example.reactivemaster.sec05;

import com.example.reactivemaster.common.Util;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Flux;
import reactor.core.scheduler.Schedulers;

@Slf4j
public class SubscribeOn {
    public static void main(String[] args) {
        subOn3();
    }

    private static void demo1(){
        var flux = Flux.create( fluxSink -> {
            for (int i = 1; i <= 3; i++) {
                log.info("GENERATING :{}",i);
                fluxSink.next(i);
            }
            fluxSink.complete();
        })
                .doOnNext(x->log.info("VALUE: {}",x));

        flux.subscribe(Util.subscriber("sub1"));
        flux.subscribe(Util.subscriber("sub2"));
        /*
            Everything is done by the current thread (main)
         */

        /*
            10:58:58.126 [main] INFO com.example.reactivemaster.sec05.ThreadingSchedulers -- GENERATING :1
10:58:58.135 [main] INFO com.example.reactivemaster.sec05.ThreadingSchedulers -- VALUE: 1
10:58:58.135 [main] INFO com.example.reactivemaster.common.DefaultSub -- sub1 RECEIVED ITEM: 1
10:58:58.135 [main] INFO com.example.reactivemaster.sec05.ThreadingSchedulers -- GENERATING :2
10:58:58.135 [main] INFO com.example.reactivemaster.sec05.ThreadingSchedulers -- VALUE: 2
10:58:58.135 [main] INFO com.example.reactivemaster.common.DefaultSub -- sub1 RECEIVED ITEM: 2
10:58:58.135 [main] INFO com.example.reactivemaster.sec05.ThreadingSchedulers -- GENERATING :3
10:58:58.135 [main] INFO com.example.reactivemaster.sec05.ThreadingSchedulers -- VALUE: 3
10:58:58.135 [main] INFO com.example.reactivemaster.common.DefaultSub -- sub1 RECEIVED ITEM: 3
10:58:58.138 [main] INFO com.example.reactivemaster.common.DefaultSub -- sub1 RECEIVED COMPLETED
10:58:58.138 [main] INFO com.example.reactivemaster.sec05.ThreadingSchedulers -- GENERATING :1
10:58:58.138 [main] INFO com.example.reactivemaster.sec05.ThreadingSchedulers -- VALUE: 1
10:58:58.138 [main] INFO com.example.reactivemaster.common.DefaultSub -- sub2 RECEIVED ITEM: 1
10:58:58.138 [main] INFO com.example.reactivemaster.sec05.ThreadingSchedulers -- GENERATING :2
10:58:58.138 [main] INFO com.example.reactivemaster.sec05.ThreadingSchedulers -- VALUE: 2
10:58:58.138 [main] INFO com.example.reactivemaster.common.DefaultSub -- sub2 RECEIVED ITEM: 2
10:58:58.138 [main] INFO com.example.reactivemaster.sec05.ThreadingSchedulers -- GENERATING :3
10:58:58.138 [main] INFO com.example.reactivemaster.sec05.ThreadingSchedulers -- VALUE: 3
10:58:58.138 [main] INFO com.example.reactivemaster.common.DefaultSub -- sub2 RECEIVED ITEM: 3
10:58:58.138 [main] INFO com.example.reactivemaster.common.DefaultSub -- sub2 RECEIVED COMPLETED
         */
    }

    private static void demo2(){
        var flux = Flux.create( fluxSink -> {
                    for (int i = 1; i <= 3; i++) {
                        log.info("GENERATING :{}",i);
                        fluxSink.next(i);
                    }
                    fluxSink.complete();
                })
                .doOnNext(x->log.info("VALUE: {}",x));

        Runnable runnable = () -> flux.subscribe(Util.subscriber("sub1"));
        Thread.ofPlatform().start(runnable);
        /*
            whoever is subscribing to the publisher, gonna end up doing all the work
         */

        /*
            11:02:11.916 [Thread-0] INFO com.example.reactivemaster.sec05.ThreadingSchedulers -- GENERATING :1
11:02:11.920 [Thread-0] INFO com.example.reactivemaster.sec05.ThreadingSchedulers -- VALUE: 1
11:02:11.920 [Thread-0] INFO com.example.reactivemaster.common.DefaultSub -- sub1 RECEIVED ITEM: 1
11:02:11.920 [Thread-0] INFO com.example.reactivemaster.sec05.ThreadingSchedulers -- GENERATING :2
11:02:11.920 [Thread-0] INFO com.example.reactivemaster.sec05.ThreadingSchedulers -- VALUE: 2
11:02:11.920 [Thread-0] INFO com.example.reactivemaster.common.DefaultSub -- sub1 RECEIVED ITEM: 2
11:02:11.920 [Thread-0] INFO com.example.reactivemaster.sec05.ThreadingSchedulers -- GENERATING :3
11:02:11.920 [Thread-0] INFO com.example.reactivemaster.sec05.ThreadingSchedulers -- VALUE: 3
11:02:11.920 [Thread-0] INFO com.example.reactivemaster.common.DefaultSub -- sub1 RECEIVED ITEM: 3
11:02:11.920 [Thread-0] INFO com.example.reactivemaster.common.DefaultSub -- sub1 RECEIVED COMPLETED
         */
    }

    private static void subOn(){
        var flux = Flux.create( fluxSink -> {
                    for (int i = 1; i <= 3; i++) {
                        log.info("GENERATING :{}",i);
                        fluxSink.next(i);
                    }
                    fluxSink.complete();
                })
                .doOnNext(x->log.info("VALUE: {}",x));

        flux
                .doFirst(() ->log.info("FIRST1"))
                .doFirst(() ->log.info("FIRST2"))
                .subscribe(Util.subscriber());

        /*
            11:11:05.141 [main] INFO com.example.reactivemaster.sec05.ThreadingSchedulers -- FIRST2
11:11:05.145 [main] INFO com.example.reactivemaster.sec05.ThreadingSchedulers -- FIRST1
11:11:05.165 [main] INFO com.example.reactivemaster.sec05.ThreadingSchedulers -- GENERATING :1
11:11:05.167 [main] INFO com.example.reactivemaster.sec05.ThreadingSchedulers -- VALUE: 1
11:11:05.167 [main] INFO com.example.reactivemaster.common.DefaultSub -- ANONYMOUS RECEIVED ITEM: 1
11:11:05.167 [main] INFO com.example.reactivemaster.sec05.ThreadingSchedulers -- GENERATING :2
11:11:05.167 [main] INFO com.example.reactivemaster.sec05.ThreadingSchedulers -- VALUE: 2
11:11:05.167 [main] INFO com.example.reactivemaster.common.DefaultSub -- ANONYMOUS RECEIVED ITEM: 2
11:11:05.167 [main] INFO com.example.reactivemaster.sec05.ThreadingSchedulers -- GENERATING :3
11:11:05.167 [main] INFO com.example.reactivemaster.sec05.ThreadingSchedulers -- VALUE: 3
11:11:05.168 [main] INFO com.example.reactivemaster.common.DefaultSub -- ANONYMOUS RECEIVED ITEM: 3
11:11:05.170 [main] INFO com.example.reactivemaster.common.DefaultSub -- ANONYMOUS RECEIVED COMPLETED
         */
    }

    private static void subOn1(){
        var flux = Flux.create( fluxSink -> {
                    for (int i = 1; i <= 3; i++) {
                        log.info("GENERATING :{}",i);
                        fluxSink.next(i);
                    }
                    fluxSink.complete();
                })
                .doOnNext(x->log.info("VALUE: {}",x));

        flux
                .doFirst(() ->log.info("FIRST1"))
                .subscribeOn(Schedulers.boundedElastic())
                .doFirst(() ->log.info("FIRST2"))
                .subscribe(Util.subscriber());

        Util.sleep(2);

        /*
            11:13:29.866 [main] INFO com.example.reactivemaster.sec05.ThreadingSchedulers -- FIRST2
11:13:29.882 [boundedElastic-1] INFO com.example.reactivemaster.sec05.ThreadingSchedulers -- FIRST1
11:13:29.895 [boundedElastic-1] INFO com.example.reactivemaster.sec05.ThreadingSchedulers -- GENERATING :1
11:13:29.895 [boundedElastic-1] INFO com.example.reactivemaster.sec05.ThreadingSchedulers -- VALUE: 1
11:13:29.895 [boundedElastic-1] INFO com.example.reactivemaster.common.DefaultSub -- ANONYMOUS RECEIVED ITEM: 1
11:13:29.895 [boundedElastic-1] INFO com.example.reactivemaster.sec05.ThreadingSchedulers -- GENERATING :2
11:13:29.895 [boundedElastic-1] INFO com.example.reactivemaster.sec05.ThreadingSchedulers -- VALUE: 2
11:13:29.895 [boundedElastic-1] INFO com.example.reactivemaster.common.DefaultSub -- ANONYMOUS RECEIVED ITEM: 2
11:13:29.895 [boundedElastic-1] INFO com.example.reactivemaster.sec05.ThreadingSchedulers -- GENERATING :3
11:13:29.907 [boundedElastic-1] INFO com.example.reactivemaster.sec05.ThreadingSchedulers -- VALUE: 3
11:13:29.907 [boundedElastic-1] INFO com.example.reactivemaster.common.DefaultSub -- ANONYMOUS RECEIVED ITEM: 3
11:13:29.911 [boundedElastic-1] INFO com.example.reactivemaster.common.DefaultSub -- ANONYMOUS RECEIVED COMPLETED
         */
    }

    private static void subOn2(){
        var flux = Flux.create( fluxSink -> {
                    for (int i = 1; i <= 3; i++) {
                        log.info("GENERATING :{}",i);
                        fluxSink.next(i);
                    }
                    fluxSink.complete();
                })
                .doOnNext(x->log.info("VALUE: {}",x));

        Runnable runnable = () -> flux
                .doFirst(() ->log.info("FIRST1"))
                .subscribeOn(Schedulers.boundedElastic())
                .doFirst(() ->log.info("FIRST2"))
                .subscribe(Util.subscriber());

        Thread.ofPlatform().start(runnable);

        Util.sleep(2);

        /*
            11:15:16.269 [Thread-0] INFO com.example.reactivemaster.sec05.ThreadingSchedulers -- FIRST2
11:15:16.300 [boundedElastic-1] INFO com.example.reactivemaster.sec05.ThreadingSchedulers -- FIRST1
11:15:16.320 [boundedElastic-1] INFO com.example.reactivemaster.sec05.ThreadingSchedulers -- GENERATING :1
11:15:16.323 [boundedElastic-1] INFO com.example.reactivemaster.sec05.ThreadingSchedulers -- VALUE: 1
11:15:16.323 [boundedElastic-1] INFO com.example.reactivemaster.common.DefaultSub -- ANONYMOUS RECEIVED ITEM: 1
11:15:16.323 [boundedElastic-1] INFO com.example.reactivemaster.sec05.ThreadingSchedulers -- GENERATING :2
11:15:16.323 [boundedElastic-1] INFO com.example.reactivemaster.sec05.ThreadingSchedulers -- VALUE: 2
11:15:16.324 [boundedElastic-1] INFO com.example.reactivemaster.common.DefaultSub -- ANONYMOUS RECEIVED ITEM: 2
11:15:16.324 [boundedElastic-1] INFO com.example.reactivemaster.sec05.ThreadingSchedulers -- GENERATING :3
11:15:16.324 [boundedElastic-1] INFO com.example.reactivemaster.sec05.ThreadingSchedulers -- VALUE: 3
11:15:16.324 [boundedElastic-1] INFO com.example.reactivemaster.common.DefaultSub -- ANONYMOUS RECEIVED ITEM: 3
11:15:16.330 [boundedElastic-1] INFO com.example.reactivemaster.common.DefaultSub -- ANONYMOUS RECEIVED COMPLETED
         */
    }

    private static void subOn3(){
        var flux = Flux.create( fluxSink -> {
                    for (int i = 1; i <= 3; i++) {
                        log.info("GENERATING :{}",i);
                        fluxSink.next(i);
                    }
                    fluxSink.complete();
                })
                .doOnNext(x->log.info("VALUE: {}",x))
                .doFirst(() ->log.info("FIRST1"))
                .subscribeOn(Schedulers.boundedElastic())
                .doFirst(() ->log.info("FIRST2"));

        Runnable runnable1 = () -> flux
                .subscribe(Util.subscriber("sub1"));

        Runnable runnable2 = () -> flux
                .subscribe(Util.subscriber("sub2"));

        Thread.ofPlatform().start(runnable1);
        Thread.ofPlatform().start(runnable2);

        Util.sleep(2);

        /*
            11:18:28.412 [Thread-1] INFO com.example.reactivemaster.sec05.ThreadingSchedulers -- FIRST2
11:18:28.412 [Thread-0] INFO com.example.reactivemaster.sec05.ThreadingSchedulers -- FIRST2
11:18:28.431 [boundedElastic-1] INFO com.example.reactivemaster.sec05.ThreadingSchedulers -- FIRST1
11:18:28.431 [boundedElastic-2] INFO com.example.reactivemaster.sec05.ThreadingSchedulers -- FIRST1
11:18:28.451 [boundedElastic-2] INFO com.example.reactivemaster.sec05.ThreadingSchedulers -- GENERATING :1
11:18:28.451 [boundedElastic-1] INFO com.example.reactivemaster.sec05.ThreadingSchedulers -- GENERATING :1
11:18:28.454 [boundedElastic-2] INFO com.example.reactivemaster.sec05.ThreadingSchedulers -- VALUE: 1
11:18:28.454 [boundedElastic-1] INFO com.example.reactivemaster.sec05.ThreadingSchedulers -- VALUE: 1
11:18:28.454 [boundedElastic-2] INFO com.example.reactivemaster.common.DefaultSub -- sub1 RECEIVED ITEM: 1
11:18:28.454 [boundedElastic-2] INFO com.example.reactivemaster.sec05.ThreadingSchedulers -- GENERATING :2
11:18:28.454 [boundedElastic-1] INFO com.example.reactivemaster.common.DefaultSub -- sub2 RECEIVED ITEM: 1
11:18:28.454 [boundedElastic-2] INFO com.example.reactivemaster.sec05.ThreadingSchedulers -- VALUE: 2
11:18:28.454 [boundedElastic-1] INFO com.example.reactivemaster.sec05.ThreadingSchedulers -- GENERATING :2
11:18:28.454 [boundedElastic-2] INFO com.example.reactivemaster.common.DefaultSub -- sub1 RECEIVED ITEM: 2
11:18:28.454 [boundedElastic-1] INFO com.example.reactivemaster.sec05.ThreadingSchedulers -- VALUE: 2
11:18:28.454 [boundedElastic-2] INFO com.example.reactivemaster.sec05.ThreadingSchedulers -- GENERATING :3
11:18:28.454 [boundedElastic-1] INFO com.example.reactivemaster.common.DefaultSub -- sub2 RECEIVED ITEM: 2
11:18:28.454 [boundedElastic-2] INFO com.example.reactivemaster.sec05.ThreadingSchedulers -- VALUE: 3
11:18:28.454 [boundedElastic-1] INFO com.example.reactivemaster.sec05.ThreadingSchedulers -- GENERATING :3
11:18:28.454 [boundedElastic-2] INFO com.example.reactivemaster.common.DefaultSub -- sub1 RECEIVED ITEM: 3
11:18:28.454 [boundedElastic-1] INFO com.example.reactivemaster.sec05.ThreadingSchedulers -- VALUE: 3
11:18:28.454 [boundedElastic-1] INFO com.example.reactivemaster.common.DefaultSub -- sub2 RECEIVED ITEM: 3
11:18:28.460 [boundedElastic-1] INFO com.example.reactivemaster.common.DefaultSub -- sub2 RECEIVED COMPLETED
11:18:28.460 [boundedElastic-2] INFO com.example.reactivemaster.common.DefaultSub -- sub1 RECEIVED COMPLETED
         */
    }


}
