package com.example.reactivemaster.sec04;

import com.example.reactivemaster.common.Util;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Flux;

import java.time.Duration;

@Slf4j
public class HotPublisher {
    public static void main(String[] args) {
        demoHot10();
    }

    private static Flux<String> movieStream(){
        return Flux.generate(
                ()->{
                    log.info("RECEIVED THE REQUEST");
                    return 1;
                },
                (state, sink) ->{
                    var scene = "MOVIE SCENE " + state;
                    log.info("PLAYING SCENE: {}", scene);
                    sink.next(scene);
                    return ++state;
                }
        ).take(5).delayElements(Duration.ofSeconds(1)).cast(String.class);
    }

    private static void demoCold(){
        var movieFlux = movieStream();

        Util.sleep(2);
        movieFlux.subscribe(Util.subscriber("SAM"));

        Util.sleep(3);
        movieFlux.subscribe(Util.subscriber("MIKE"));

        Util.sleep(10);

        /*
            RECEIVED THE REQUEST
09:33:13.907 [main] INFO com.example.reactivemaster.sec04.HotPublisher -- PLAYING SCENE: MOVIE SCENE 1
09:33:14.933 [parallel-1] INFO com.example.reactivemaster.common.DefaultSub -- SAM RECEIVED ITEM: MOVIE SCENE 1
09:33:14.935 [parallel-1] INFO com.example.reactivemaster.sec04.HotPublisher -- PLAYING SCENE: MOVIE SCENE 2
09:33:15.940 [parallel-2] INFO com.example.reactivemaster.common.DefaultSub -- SAM RECEIVED ITEM: MOVIE SCENE 2
09:33:15.940 [parallel-2] INFO com.example.reactivemaster.sec04.HotPublisher -- PLAYING SCENE: MOVIE SCENE 3
09:33:16.919 [main] INFO com.example.reactivemaster.sec04.HotPublisher -- RECEIVED THE REQUEST
09:33:16.919 [main] INFO com.example.reactivemaster.sec04.HotPublisher -- PLAYING SCENE: MOVIE SCENE 1
09:33:16.951 [parallel-3] INFO com.example.reactivemaster.common.DefaultSub -- SAM RECEIVED ITEM: MOVIE SCENE 3
09:33:16.951 [parallel-3] INFO com.example.reactivemaster.sec04.HotPublisher -- PLAYING SCENE: MOVIE SCENE 4
09:33:17.925 [parallel-4] INFO com.example.reactivemaster.common.DefaultSub -- MIKE RECEIVED ITEM: MOVIE SCENE 1
09:33:17.925 [parallel-4] INFO com.example.reactivemaster.sec04.HotPublisher -- PLAYING SCENE: MOVIE SCENE 2
09:33:17.957 [parallel-5] INFO com.example.reactivemaster.common.DefaultSub -- SAM RECEIVED ITEM: MOVIE SCENE 4
09:33:17.957 [parallel-5] INFO com.example.reactivemaster.sec04.HotPublisher -- PLAYING SCENE: MOVIE SCENE 5
09:33:18.931 [parallel-6] INFO com.example.reactivemaster.common.DefaultSub -- MIKE RECEIVED ITEM: MOVIE SCENE 2
09:33:18.931 [parallel-6] INFO com.example.reactivemaster.sec04.HotPublisher -- PLAYING SCENE: MOVIE SCENE 3
09:33:18.963 [parallel-7] INFO com.example.reactivemaster.common.DefaultSub -- SAM RECEIVED ITEM: MOVIE SCENE 5
09:33:18.965 [parallel-7] INFO com.example.reactivemaster.common.DefaultSub -- SAM RECEIVED COMPLETED
09:33:19.939 [parallel-8] INFO com.example.reactivemaster.common.DefaultSub -- MIKE RECEIVED ITEM: MOVIE SCENE 3
09:33:19.939 [parallel-8] INFO com.example.reactivemaster.sec04.HotPublisher -- PLAYING SCENE: MOVIE SCENE 4
09:33:20.943 [parallel-1] INFO com.example.reactivemaster.common.DefaultSub -- MIKE RECEIVED ITEM: MOVIE SCENE 4
09:33:20.944 [parallel-1] INFO com.example.reactivemaster.sec04.HotPublisher -- PLAYING SCENE: MOVIE SCENE 5
09:33:21.955 [parallel-2] INFO com.example.reactivemaster.common.DefaultSub -- MIKE RECEIVED ITEM: MOVIE SCENE 5
09:33:21.955 [parallel-2] INFO com.example.reactivemaster.common.DefaultSub -- MIKE RECEIVED COMPLETED
         */
    }

    private static void demoHot(){
        var movieFlux = movieStream().share();

        Util.sleep(2);
        movieFlux.subscribe(Util.subscriber("SAM"));

        Util.sleep(3);
        movieFlux.subscribe(Util.subscriber("MIKE"));

        Util.sleep(10);

        /*
            09:35:44.499 [main] INFO com.example.reactivemaster.sec04.HotPublisher -- RECEIVED THE REQUEST
09:35:44.513 [main] INFO com.example.reactivemaster.sec04.HotPublisher -- PLAYING SCENE: MOVIE SCENE 1
09:35:45.534 [parallel-1] INFO com.example.reactivemaster.common.DefaultSub -- SAM RECEIVED ITEM: MOVIE SCENE 1
09:35:45.534 [parallel-1] INFO com.example.reactivemaster.sec04.HotPublisher -- PLAYING SCENE: MOVIE SCENE 2
09:35:46.546 [parallel-2] INFO com.example.reactivemaster.common.DefaultSub -- SAM RECEIVED ITEM: MOVIE SCENE 2
09:35:46.547 [parallel-2] INFO com.example.reactivemaster.sec04.HotPublisher -- PLAYING SCENE: MOVIE SCENE 3
09:35:47.559 [parallel-3] INFO com.example.reactivemaster.common.DefaultSub -- SAM RECEIVED ITEM: MOVIE SCENE 3
09:35:47.559 [parallel-3] INFO com.example.reactivemaster.common.DefaultSub -- MIKE RECEIVED ITEM: MOVIE SCENE 3
09:35:47.561 [parallel-3] INFO com.example.reactivemaster.sec04.HotPublisher -- PLAYING SCENE: MOVIE SCENE 4
09:35:48.571 [parallel-4] INFO com.example.reactivemaster.common.DefaultSub -- SAM RECEIVED ITEM: MOVIE SCENE 4
09:35:48.572 [parallel-4] INFO com.example.reactivemaster.common.DefaultSub -- MIKE RECEIVED ITEM: MOVIE SCENE 4
09:35:48.572 [parallel-4] INFO com.example.reactivemaster.sec04.HotPublisher -- PLAYING SCENE: MOVIE SCENE 5
09:35:49.586 [parallel-5] INFO com.example.reactivemaster.common.DefaultSub -- SAM RECEIVED ITEM: MOVIE SCENE 5
09:35:49.587 [parallel-5] INFO com.example.reactivemaster.common.DefaultSub -- MIKE RECEIVED ITEM: MOVIE SCENE 5
09:35:49.593 [parallel-5] INFO com.example.reactivemaster.common.DefaultSub -- SAM RECEIVED COMPLETED
09:35:49.593 [parallel-5] INFO com.example.reactivemaster.common.DefaultSub -- MIKE RECEIVED COMPLETED
         */
    }

    private static void demoHot2(){
        var movieFlux = movieStream().share();

        Util.sleep(2);
        movieFlux.subscribe(Util.subscriber("SAM"));

        Util.sleep(3);
        movieFlux
                .take(2)
                .subscribe(Util.subscriber("MIKE"));

        Util.sleep(10);

        /*
            09:47:11.080 [main] INFO com.example.reactivemaster.sec04.HotPublisher -- RECEIVED THE REQUEST
09:47:11.088 [main] INFO com.example.reactivemaster.sec04.HotPublisher -- PLAYING SCENE: MOVIE SCENE 1
09:47:12.109 [parallel-1] INFO com.example.reactivemaster.common.DefaultSub -- SAM RECEIVED ITEM: MOVIE SCENE 1
09:47:12.109 [parallel-1] INFO com.example.reactivemaster.sec04.HotPublisher -- PLAYING SCENE: MOVIE SCENE 2
09:47:13.121 [parallel-2] INFO com.example.reactivemaster.common.DefaultSub -- SAM RECEIVED ITEM: MOVIE SCENE 2
09:47:13.121 [parallel-2] INFO com.example.reactivemaster.sec04.HotPublisher -- PLAYING SCENE: MOVIE SCENE 3
09:47:14.131 [parallel-3] INFO com.example.reactivemaster.common.DefaultSub -- SAM RECEIVED ITEM: MOVIE SCENE 3
09:47:14.131 [parallel-3] INFO com.example.reactivemaster.common.DefaultSub -- MIKE RECEIVED ITEM: MOVIE SCENE 3
09:47:14.131 [parallel-3] INFO com.example.reactivemaster.sec04.HotPublisher -- PLAYING SCENE: MOVIE SCENE 4
09:47:15.140 [parallel-4] INFO com.example.reactivemaster.common.DefaultSub -- SAM RECEIVED ITEM: MOVIE SCENE 4
09:47:15.140 [parallel-4] INFO com.example.reactivemaster.common.DefaultSub -- MIKE RECEIVED ITEM: MOVIE SCENE 4
09:47:15.140 [parallel-4] INFO com.example.reactivemaster.common.DefaultSub -- MIKE RECEIVED COMPLETED
09:47:15.140 [parallel-4] INFO com.example.reactivemaster.sec04.HotPublisher -- PLAYING SCENE: MOVIE SCENE 5
09:47:16.157 [parallel-5] INFO com.example.reactivemaster.common.DefaultSub -- SAM RECEIVED ITEM: MOVIE SCENE 5
09:47:16.157 [parallel-5] INFO com.example.reactivemaster.common.DefaultSub -- SAM RECEIVED COMPLETED
         */
    }

    private static void demoHot3(){
        var movieFlux = movieStream().share();

        Util.sleep(2);
        movieFlux
                .take(3)
                .subscribe(Util.subscriber("SAM"));

        Util.sleep(3);
        movieFlux
                .take(2)
                .subscribe(Util.subscriber("MIKE"));

        Util.sleep(10);

        /*
            09:50:22.264 [main] INFO com.example.reactivemaster.sec04.HotPublisher -- RECEIVED THE REQUEST
09:50:22.283 [main] INFO com.example.reactivemaster.sec04.HotPublisher -- PLAYING SCENE: MOVIE SCENE 1
09:50:23.321 [parallel-1] INFO com.example.reactivemaster.common.DefaultSub -- SAM RECEIVED ITEM: MOVIE SCENE 1
09:50:23.321 [parallel-1] INFO com.example.reactivemaster.sec04.HotPublisher -- PLAYING SCENE: MOVIE SCENE 2
09:50:24.329 [parallel-2] INFO com.example.reactivemaster.common.DefaultSub -- SAM RECEIVED ITEM: MOVIE SCENE 2
09:50:24.329 [parallel-2] INFO com.example.reactivemaster.sec04.HotPublisher -- PLAYING SCENE: MOVIE SCENE 3
09:50:25.340 [parallel-3] INFO com.example.reactivemaster.common.DefaultSub -- SAM RECEIVED ITEM: MOVIE SCENE 3
09:50:25.340 [parallel-3] INFO com.example.reactivemaster.common.DefaultSub -- SAM RECEIVED COMPLETED
09:50:25.340 [parallel-3] INFO com.example.reactivemaster.common.DefaultSub -- MIKE RECEIVED ITEM: MOVIE SCENE 3
09:50:25.340 [parallel-3] INFO com.example.reactivemaster.sec04.HotPublisher -- PLAYING SCENE: MOVIE SCENE 4
09:50:26.350 [parallel-4] INFO com.example.reactivemaster.common.DefaultSub -- MIKE RECEIVED ITEM: MOVIE SCENE 4
09:50:26.350 [parallel-4] INFO com.example.reactivemaster.common.DefaultSub -- MIKE RECEIVED COMPLETED
         */
    }

    private static void demoHot4(){
        var movieFlux = movieStream().publish().refCount(1);
        //publish().refCount(1);  same as share()
        // it needs at least ONE sub to publish data

        Util.sleep(2);
        movieFlux
                .take(3)
                .subscribe(Util.subscriber("SAM"));

        Util.sleep(3);
        movieFlux
                .take(2)
                .subscribe(Util.subscriber("MIKE"));

        Util.sleep(10);

        /*
            09:54:06.691 [main] INFO com.example.reactivemaster.sec04.HotPublisher -- RECEIVED THE REQUEST
09:54:06.706 [main] INFO com.example.reactivemaster.sec04.HotPublisher -- PLAYING SCENE: MOVIE SCENE 1
09:54:07.724 [parallel-1] INFO com.example.reactivemaster.common.DefaultSub -- SAM RECEIVED ITEM: MOVIE SCENE 1
09:54:07.724 [parallel-1] INFO com.example.reactivemaster.sec04.HotPublisher -- PLAYING SCENE: MOVIE SCENE 2
09:54:08.736 [parallel-2] INFO com.example.reactivemaster.common.DefaultSub -- SAM RECEIVED ITEM: MOVIE SCENE 2
09:54:08.736 [parallel-2] INFO com.example.reactivemaster.sec04.HotPublisher -- PLAYING SCENE: MOVIE SCENE 3
09:54:09.749 [parallel-3] INFO com.example.reactivemaster.common.DefaultSub -- SAM RECEIVED ITEM: MOVIE SCENE 3
09:54:09.749 [parallel-3] INFO com.example.reactivemaster.common.DefaultSub -- SAM RECEIVED COMPLETED
09:54:09.749 [parallel-3] INFO com.example.reactivemaster.common.DefaultSub -- MIKE RECEIVED ITEM: MOVIE SCENE 3
09:54:09.749 [parallel-3] INFO com.example.reactivemaster.sec04.HotPublisher -- PLAYING SCENE: MOVIE SCENE 4
09:54:10.759 [parallel-4] INFO com.example.reactivemaster.common.DefaultSub -- MIKE RECEIVED ITEM: MOVIE SCENE 4
09:54:10.759 [parallel-4] INFO com.example.reactivemaster.common.DefaultSub -- MIKE RECEIVED COMPLETED
         */
    }

    private static void demoHot5(){
        var movieFlux = movieStream().publish().refCount(2);
        // it needs at least 2 subs to publish data
        // it will wait for both, sam and mike, to emit data

        Util.sleep(2);
        movieFlux
                .take(3)
                .subscribe(Util.subscriber("SAM"));

        Util.sleep(3);
        movieFlux
                .take(2)
                .subscribe(Util.subscriber("MIKE"));

        Util.sleep(10);

        /*
            09:57:02.757 [main] INFO com.example.reactivemaster.sec04.HotPublisher -- RECEIVED THE REQUEST
09:57:02.770 [main] INFO com.example.reactivemaster.sec04.HotPublisher -- PLAYING SCENE: MOVIE SCENE 1
09:57:03.787 [parallel-1] INFO com.example.reactivemaster.common.DefaultSub -- SAM RECEIVED ITEM: MOVIE SCENE 1
09:57:03.787 [parallel-1] INFO com.example.reactivemaster.common.DefaultSub -- MIKE RECEIVED ITEM: MOVIE SCENE 1
09:57:03.787 [parallel-1] INFO com.example.reactivemaster.sec04.HotPublisher -- PLAYING SCENE: MOVIE SCENE 2
09:57:04.794 [parallel-2] INFO com.example.reactivemaster.common.DefaultSub -- SAM RECEIVED ITEM: MOVIE SCENE 2
09:57:04.794 [parallel-2] INFO com.example.reactivemaster.common.DefaultSub -- MIKE RECEIVED ITEM: MOVIE SCENE 2
09:57:04.794 [parallel-2] INFO com.example.reactivemaster.common.DefaultSub -- MIKE RECEIVED COMPLETED
09:57:04.794 [parallel-2] INFO com.example.reactivemaster.sec04.HotPublisher -- PLAYING SCENE: MOVIE SCENE 3
09:57:05.813 [parallel-3] INFO com.example.reactivemaster.common.DefaultSub -- SAM RECEIVED ITEM: MOVIE SCENE 3
09:57:05.813 [parallel-3] INFO com.example.reactivemaster.common.DefaultSub -- SAM RECEIVED COMPLETED
         */
    }

    private static void demoHot6(){
        var movieFlux = movieStream().share();

        Util.sleep(2);
        movieFlux
                .take(1)
                .subscribe(Util.subscriber("SAM"));

        Util.sleep(3);
        movieFlux
                .take(2)
                .subscribe(Util.subscriber("MIKE"));

        Util.sleep(10);
        /*
            10:00:30.640 [main] INFO com.example.reactivemaster.sec04.HotPublisher -- RECEIVED THE REQUEST
10:00:30.640 [main] INFO com.example.reactivemaster.sec04.HotPublisher -- PLAYING SCENE: MOVIE SCENE 1
10:00:31.672 [parallel-1] INFO com.example.reactivemaster.common.DefaultSub -- SAM RECEIVED ITEM: MOVIE SCENE 1
10:00:31.676 [parallel-1] INFO com.example.reactivemaster.common.DefaultSub -- SAM RECEIVED COMPLETED
10:00:33.663 [main] INFO com.example.reactivemaster.sec04.HotPublisher -- RECEIVED THE REQUEST
10:00:33.663 [main] INFO com.example.reactivemaster.sec04.HotPublisher -- PLAYING SCENE: MOVIE SCENE 1
10:00:34.675 [parallel-2] INFO com.example.reactivemaster.common.DefaultSub -- MIKE RECEIVED ITEM: MOVIE SCENE 1
10:00:34.675 [parallel-2] INFO com.example.reactivemaster.sec04.HotPublisher -- PLAYING SCENE: MOVIE SCENE 2
10:00:35.676 [parallel-3] INFO com.example.reactivemaster.common.DefaultSub -- MIKE RECEIVED ITEM: MOVIE SCENE 2
10:00:35.676 [parallel-3] INFO com.example.reactivemaster.common.DefaultSub -- MIKE RECEIVED COMPLETED
         */
    }

    private static void demoHot7(){
        var movieFlux = movieStream().publish().autoConnect();

        /*
            It will wait until sam joins to start emitting data
            but even if both sam and mike leave, it will continue emitting data
         */

        Util.sleep(2);
        movieFlux
                .take(3)
                .subscribe(Util.subscriber("SAM"));

        Util.sleep(3);
        movieFlux
                .take(2)
                .subscribe(Util.subscriber("MIKE"));

        Util.sleep(10);

        /*
            10:05:05.584 [main] INFO com.example.reactivemaster.sec04.HotPublisher -- RECEIVED THE REQUEST
10:05:05.600 [main] INFO com.example.reactivemaster.sec04.HotPublisher -- PLAYING SCENE: MOVIE SCENE 1
10:05:06.633 [parallel-1] INFO com.example.reactivemaster.common.DefaultSub -- SAM RECEIVED ITEM: MOVIE SCENE 1
10:05:06.633 [parallel-1] INFO com.example.reactivemaster.sec04.HotPublisher -- PLAYING SCENE: MOVIE SCENE 2
10:05:07.642 [parallel-2] INFO com.example.reactivemaster.common.DefaultSub -- SAM RECEIVED ITEM: MOVIE SCENE 2
10:05:07.642 [parallel-2] INFO com.example.reactivemaster.sec04.HotPublisher -- PLAYING SCENE: MOVIE SCENE 3
10:05:08.647 [parallel-3] INFO com.example.reactivemaster.common.DefaultSub -- SAM RECEIVED ITEM: MOVIE SCENE 3
10:05:08.651 [parallel-3] INFO com.example.reactivemaster.common.DefaultSub -- SAM RECEIVED COMPLETED
10:05:08.651 [parallel-3] INFO com.example.reactivemaster.common.DefaultSub -- MIKE RECEIVED ITEM: MOVIE SCENE 3
10:05:08.651 [parallel-3] INFO com.example.reactivemaster.sec04.HotPublisher -- PLAYING SCENE: MOVIE SCENE 4
10:05:09.662 [parallel-4] INFO com.example.reactivemaster.common.DefaultSub -- MIKE RECEIVED ITEM: MOVIE SCENE 4
10:05:09.662 [parallel-4] INFO com.example.reactivemaster.common.DefaultSub -- MIKE RECEIVED COMPLETED
10:05:09.662 [parallel-4] INFO com.example.reactivemaster.sec04.HotPublisher -- PLAYING SCENE: MOVIE SCENE 5
         */
    }

    private static void demoHot8(){
        var movieFlux = movieStream().publish().autoConnect(0);

        /*
            It WON'T wait until sam joins to start emitting data
            It will emit data immediately
            but even if both sam and mike leave, it will continue emitting data
         */

        Util.sleep(2);
        movieFlux
                .take(3)
                .subscribe(Util.subscriber("SAM"));

        Util.sleep(3);
        movieFlux
                .take(2)
                .subscribe(Util.subscriber("MIKE"));

        Util.sleep(10);

        /*
            10:07:33.048 [main] INFO com.example.reactivemaster.sec04.HotPublisher -- RECEIVED THE REQUEST
10:07:33.059 [main] INFO com.example.reactivemaster.sec04.HotPublisher -- PLAYING SCENE: MOVIE SCENE 1
10:07:34.072 [parallel-1] INFO com.example.reactivemaster.sec04.HotPublisher -- PLAYING SCENE: MOVIE SCENE 2
10:07:35.090 [parallel-2] INFO com.example.reactivemaster.sec04.HotPublisher -- PLAYING SCENE: MOVIE SCENE 3
10:07:36.100 [parallel-3] INFO com.example.reactivemaster.common.DefaultSub -- SAM RECEIVED ITEM: MOVIE SCENE 3
10:07:36.100 [parallel-3] INFO com.example.reactivemaster.sec04.HotPublisher -- PLAYING SCENE: MOVIE SCENE 4
10:07:37.107 [parallel-4] INFO com.example.reactivemaster.common.DefaultSub -- SAM RECEIVED ITEM: MOVIE SCENE 4
10:07:37.107 [parallel-4] INFO com.example.reactivemaster.sec04.HotPublisher -- PLAYING SCENE: MOVIE SCENE 5
10:07:38.123 [parallel-5] INFO com.example.reactivemaster.common.DefaultSub -- SAM RECEIVED ITEM: MOVIE SCENE 5
10:07:38.129 [parallel-5] INFO com.example.reactivemaster.common.DefaultSub -- SAM RECEIVED COMPLETED
10:07:38.129 [parallel-5] INFO com.example.reactivemaster.common.DefaultSub -- MIKE RECEIVED COMPLETED
         */
    }

    private static Flux<Integer> stockStream(){
        return Flux.<Integer>generate(sink ->sink.next(Util.faker().random().nextInt(10,100)))
                .doOnNext(x-> log.info("EMITTING PRICE: {}",x))
                .delayElements(Duration.ofSeconds(3));
    }
    private static void demoHot9(){

        var flux = stockStream().publish().autoConnect(0);

        /*
            Both mike and sam can't see the past data, they have to wait for the new emitted data
            to see them.
            They both can't see the past price
            They both can't see the current price
            they have to wait for the new price to see it.
            if you wanna make them see whatever price was emitted just right before they joined
            see next method!
         */

        Util.sleep(4);
        log.info("SAM JOINING");
        flux.subscribe(Util.subscriber("SAM"));

        Util.sleep(4);
        log.info("MIKE JOINING");
        flux.subscribe(Util.subscriber("MIKE"));

        Util.sleep(15);

        /*
            10:14:27.666 [main] INFO com.example.reactivemaster.sec04.HotPublisher -- EMITTING PRICE: 80
10:14:30.693 [parallel-1] INFO com.example.reactivemaster.sec04.HotPublisher -- EMITTING PRICE: 62
10:14:31.689 [main] INFO com.example.reactivemaster.sec04.HotPublisher -- SAM JOINING
10:14:33.703 [parallel-2] INFO com.example.reactivemaster.common.DefaultSub -- SAM RECEIVED ITEM: 62
10:14:33.703 [parallel-2] INFO com.example.reactivemaster.sec04.HotPublisher -- EMITTING PRICE: 48
10:14:35.708 [main] INFO com.example.reactivemaster.sec04.HotPublisher -- MIKE JOINING
10:14:36.718 [parallel-3] INFO com.example.reactivemaster.common.DefaultSub -- SAM RECEIVED ITEM: 48
10:14:36.718 [parallel-3] INFO com.example.reactivemaster.common.DefaultSub -- MIKE RECEIVED ITEM: 48
10:14:36.718 [parallel-3] INFO com.example.reactivemaster.sec04.HotPublisher -- EMITTING PRICE: 63
10:14:39.732 [parallel-4] INFO com.example.reactivemaster.common.DefaultSub -- SAM RECEIVED ITEM: 63
10:14:39.732 [parallel-4] INFO com.example.reactivemaster.common.DefaultSub -- MIKE RECEIVED ITEM: 63
10:14:39.732 [parallel-4] INFO com.example.reactivemaster.sec04.HotPublisher -- EMITTING PRICE: 16
10:14:42.741 [parallel-5] INFO com.example.reactivemaster.common.DefaultSub -- SAM RECEIVED ITEM: 16
10:14:42.741 [parallel-5] INFO com.example.reactivemaster.common.DefaultSub -- MIKE RECEIVED ITEM: 16
10:14:42.741 [parallel-5] INFO com.example.reactivemaster.sec04.HotPublisher -- EMITTING PRICE: 41
10:14:45.743 [parallel-6] INFO com.example.reactivemaster.common.DefaultSub -- SAM RECEIVED ITEM: 41
10:14:45.743 [parallel-6] INFO com.example.reactivemaster.common.DefaultSub -- MIKE RECEIVED ITEM: 41
10:14:45.743 [parallel-6] INFO com.example.reactivemaster.sec04.HotPublisher -- EMITTING PRICE: 10
10:14:48.755 [parallel-7] INFO com.example.reactivemaster.common.DefaultSub -- SAM RECEIVED ITEM: 10
10:14:48.756 [parallel-7] INFO com.example.reactivemaster.common.DefaultSub -- MIKE RECEIVED ITEM: 10
10:14:48.756 [parallel-7] INFO com.example.reactivemaster.sec04.HotPublisher -- EMITTING PRICE: 57
         */
    }

    private static void demoHot10(){
        var flux = stockStream().replay(1).autoConnect(0);

        /*
            Now whenever sam and mike join, they can see the latest current price
            the moment they join.
            replay(1) -> means replay the latest ONE current price;
         */

        Util.sleep(4);
        log.info("SAM JOINING");
        flux.subscribe(Util.subscriber("SAM"));

        Util.sleep(4);
        log.info("MIKE JOINING");
        flux.subscribe(Util.subscriber("MIKE"));

        Util.sleep(15);

        /*
            10:22:54.024 [main] INFO com.example.reactivemaster.sec04.HotPublisher -- EMITTING PRICE: 98
10:22:57.038 [parallel-1] INFO com.example.reactivemaster.sec04.HotPublisher -- EMITTING PRICE: 29
10:22:58.040 [main] INFO com.example.reactivemaster.sec04.HotPublisher -- SAM JOINING
10:22:58.040 [main] INFO com.example.reactivemaster.common.DefaultSub -- SAM RECEIVED ITEM: 98
10:23:00.057 [parallel-2] INFO com.example.reactivemaster.common.DefaultSub -- SAM RECEIVED ITEM: 29
10:23:00.057 [parallel-2] INFO com.example.reactivemaster.sec04.HotPublisher -- EMITTING PRICE: 64
10:23:02.049 [main] INFO com.example.reactivemaster.sec04.HotPublisher -- MIKE JOINING
10:23:02.049 [main] INFO com.example.reactivemaster.common.DefaultSub -- MIKE RECEIVED ITEM: 29
10:23:03.070 [parallel-3] INFO com.example.reactivemaster.common.DefaultSub -- SAM RECEIVED ITEM: 64
10:23:03.070 [parallel-3] INFO com.example.reactivemaster.common.DefaultSub -- MIKE RECEIVED ITEM: 64
10:23:03.070 [parallel-3] INFO com.example.reactivemaster.sec04.HotPublisher -- EMITTING PRICE: 80
10:23:06.079 [parallel-4] INFO com.example.reactivemaster.common.DefaultSub -- SAM RECEIVED ITEM: 80
10:23:06.079 [parallel-4] INFO com.example.reactivemaster.common.DefaultSub -- MIKE RECEIVED ITEM: 80
10:23:06.079 [parallel-4] INFO com.example.reactivemaster.sec04.HotPublisher -- EMITTING PRICE: 16
10:23:09.086 [parallel-5] INFO com.example.reactivemaster.common.DefaultSub -- SAM RECEIVED ITEM: 16
10:23:09.086 [parallel-5] INFO com.example.reactivemaster.common.DefaultSub -- MIKE RECEIVED ITEM: 16
10:23:09.086 [parallel-5] INFO com.example.reactivemaster.sec04.HotPublisher -- EMITTING PRICE: 13
10:23:12.101 [parallel-6] INFO com.example.reactivemaster.common.DefaultSub -- SAM RECEIVED ITEM: 13
10:23:12.101 [parallel-6] INFO com.example.reactivemaster.common.DefaultSub -- MIKE RECEIVED ITEM: 13
10:23:12.101 [parallel-6] INFO com.example.reactivemaster.sec04.HotPublisher -- EMITTING PRICE: 35
10:23:15.102 [parallel-7] INFO com.example.reactivemaster.common.DefaultSub -- SAM RECEIVED ITEM: 35
10:23:15.102 [parallel-7] INFO com.example.reactivemaster.common.DefaultSub -- MIKE RECEIVED ITEM: 35
10:23:15.102 [parallel-7] INFO com.example.reactivemaster.sec04.HotPublisher -- EMITTING PRICE: 62
         */
    }
}
