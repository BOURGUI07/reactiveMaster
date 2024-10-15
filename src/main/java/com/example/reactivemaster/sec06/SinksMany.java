package com.example.reactivemaster.sec06;

import com.example.reactivemaster.common.Util;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Sinks;

import java.time.Duration;
import java.util.ArrayList;
import java.util.concurrent.CompletableFuture;

@Slf4j
public class SinksMany {
    public static void main(String[] args) {
        sinkMany_replay2();
    }

    private static void demo1(){
        var sink = Sinks.many().unicast().onBackpressureBuffer();
        var flux = sink.asFlux();
        sink.tryEmitNext("hi");
        sink.tryEmitNext("bye");
        flux.subscribe(Util.subscriber());

        /*
            09:22:41.148 [main] INFO com.example.reactivemaster.common.DefaultSub -- ANONYMOUS RECEIVED ITEM: hi
            09:22:41.150 [main] INFO com.example.reactivemaster.common.DefaultSub -- ANONYMOUS RECEIVED ITEM: bye
         */
    }

    private static void demo2(){
        var sink = Sinks.many().unicast().onBackpressureBuffer();
        var flux = sink.asFlux();
        sink.tryEmitNext("hi");
        sink.tryEmitNext("bye");
        flux.subscribe(Util.subscriber("sam"));
        flux.subscribe(Util.subscriber("tom"));

        /*
            09:24:02.732 [main] INFO com.example.reactivemaster.common.DefaultSub -- sam RECEIVED ITEM: hi
            09:24:02.736 [main] INFO com.example.reactivemaster.common.DefaultSub -- sam RECEIVED ITEM: bye
            09:24:02.738 [main] INFO com.example.reactivemaster.common.DefaultSub -- tom CAUSED ERROR: Sinks.many().unicast() sinks only allow a single Subscriber
         */
    }

    public static void sink_ThreadSafety(){
        var sink = Sinks.many().unicast().onBackpressureBuffer();
        var flux = sink.asFlux();
        var list = new ArrayList<>();
        flux.subscribe(list::add);
        for(int i=0;i<1000;i++){
            var j = i;
            CompletableFuture.runAsync(()->{
                sink.tryEmitNext(j);
            });
        }
        Util.sleep(10);
        log.info("LIST SIZE: {}", list.size()); // it won't print 999
    }

    public static void sink_ThreadSafety1(){
        var sink = Sinks.many().unicast().onBackpressureBuffer();
        var flux = sink.asFlux();
        var list = new ArrayList<>();
        flux.subscribe(list::add);
        for(int i=0;i<1000;i++){
            var j = i;
            CompletableFuture.runAsync(()->{
                sink.emitNext(j,((signalType, emitResult) -> {
                    return Sinks.EmitResult.FAIL_NON_SERIALIZED.equals(emitResult);
                }));
            });
        }
        Util.sleep(10);
        log.info("LIST SIZE: {}", list.size()); // it WILL print 999
    }

    public static void sinkMany_multicast(){
        // handle through which we would push items
        // onBackPressureBuffer - bounded queue
        var sink = Sinks.many().multicast().onBackpressureBuffer();
        var flux = sink.asFlux();
        flux.subscribe(Util.subscriber("sub1"));
        flux.subscribe(Util.subscriber("sub2"));

        sink.tryEmitNext("Hi");
        sink.tryEmitNext("Hello");
        sink.tryEmitNext("Hola");

        Util.sleep(2);

        flux.subscribe(Util.subscriber("sub3"));

        sink.tryEmitNext("new Message");
		/*
			With sink multicast, we can emit many values,
			we can have more subs.
			if a sub joins late, won't be able to see the missed message.
			Here both sub1 and sub2 will receive all the messages
			Sub3 will only receive the 'new message', won't be able
			to see past messages
		 */

        /*
            09:39:45.120 [main] INFO com.example.reactivemaster.common.DefaultSub -- sub1 RECEIVED ITEM: Hi
09:39:45.124 [main] INFO com.example.reactivemaster.common.DefaultSub -- sub2 RECEIVED ITEM: Hi
09:39:45.124 [main] INFO com.example.reactivemaster.common.DefaultSub -- sub1 RECEIVED ITEM: Hello
09:39:45.124 [main] INFO com.example.reactivemaster.common.DefaultSub -- sub2 RECEIVED ITEM: Hello
09:39:45.124 [main] INFO com.example.reactivemaster.common.DefaultSub -- sub1 RECEIVED ITEM: Hola
09:39:45.124 [main] INFO com.example.reactivemaster.common.DefaultSub -- sub2 RECEIVED ITEM: Hola
09:39:47.134 [main] INFO com.example.reactivemaster.common.DefaultSub -- sub1 RECEIVED ITEM: new Message
09:39:47.134 [main] INFO com.example.reactivemaster.common.DefaultSub -- sub2 RECEIVED ITEM: new Message
09:39:47.134 [main] INFO com.example.reactivemaster.common.DefaultSub -- sub3 RECEIVED ITEM: new Message
         */

    }

    public static void sinkMany_multicast2(){
        // handle through which we would push items
        // onBackPressureBuffer - bounded queue
        var sink = Sinks.many().multicast().onBackpressureBuffer();
        var flux = sink.asFlux();

        sink.tryEmitNext("Hi");
        sink.tryEmitNext("Hola");
        sink.tryEmitNext("HELLO");
        Util.sleep(2);

        flux.subscribe(Util.subscriber("sub1"));
        flux.subscribe(Util.subscriber("sub2"));
        flux.subscribe(Util.subscriber("sub3"));
        sink.tryEmitNext("new Message");
		/*
			Here only sub1 who's gonna receive all the messages
			sub2 and sub3 will only receive 'new message'
		 */

        /*
            09:44:08.851 [main] INFO com.example.reactivemaster.common.DefaultSub -- sub1 RECEIVED ITEM: Hi
09:44:08.851 [main] INFO com.example.reactivemaster.common.DefaultSub -- sub1 RECEIVED ITEM: Hola
09:44:08.851 [main] INFO com.example.reactivemaster.common.DefaultSub -- sub1 RECEIVED ITEM: HELLO
09:44:08.851 [main] INFO com.example.reactivemaster.common.DefaultSub -- sub1 RECEIVED ITEM: new Message
09:44:08.851 [main] INFO com.example.reactivemaster.common.DefaultSub -- sub2 RECEIVED ITEM: new Message
09:44:08.851 [main] INFO com.example.reactivemaster.common.DefaultSub -- sub3 RECEIVED ITEM: new Message
         */
    }

    private static void sinkMany_multicast3() {

        System.setProperty("reactor.bufferSize.small", "16");

        // handle through which we would push items
        // onBackPressureBuffer - bounded queue
        var sink = Sinks.many().multicast().onBackpressureBuffer();//sam will recieve SOME of the mssages, not ALL

        // handle through which subscribers will receive items
        var flux = sink.asFlux();

        flux.subscribe(Util.subscriber("sam"));
        flux.delayElements(Duration.ofMillis(200)).subscribe(Util.subscriber("mike"));

		/*
			Here sam won't receive ALL the messages, because the other sub is slow
			With multicast().onBackpressureBuffer(), the performance of slow sub affect the performance
			of the fast sub
		 */

        for (int i = 1; i <= 50; i++) {
            var result = sink.tryEmitNext(i);
            log.info("item: {}, result: {}", i, result);
        }

        Util.sleep(10);

        /*
            09:50:45.020 [main] INFO com.example.reactivemaster.common.DefaultSub -- sam RECEIVED ITEM: 1
09:50:45.025 [main] INFO com.example.reactivemaster.sec06.SinksMany -- item: 1, result: OK
09:50:45.025 [main] INFO com.example.reactivemaster.sec06.SinksMany -- item: 2, result: OK
09:50:45.025 [main] INFO com.example.reactivemaster.sec06.SinksMany -- item: 3, result: OK
09:50:45.025 [main] INFO com.example.reactivemaster.sec06.SinksMany -- item: 4, result: OK
09:50:45.025 [main] INFO com.example.reactivemaster.sec06.SinksMany -- item: 5, result: OK
09:50:45.025 [main] INFO com.example.reactivemaster.sec06.SinksMany -- item: 6, result: OK
09:50:45.025 [main] INFO com.example.reactivemaster.sec06.SinksMany -- item: 7, result: OK
09:50:45.025 [main] INFO com.example.reactivemaster.sec06.SinksMany -- item: 8, result: OK
09:50:45.025 [main] INFO com.example.reactivemaster.sec06.SinksMany -- item: 9, result: OK
09:50:45.025 [main] INFO com.example.reactivemaster.sec06.SinksMany -- item: 10, result: OK
09:50:45.025 [main] INFO com.example.reactivemaster.sec06.SinksMany -- item: 11, result: OK
09:50:45.025 [main] INFO com.example.reactivemaster.sec06.SinksMany -- item: 12, result: OK
09:50:45.025 [main] INFO com.example.reactivemaster.sec06.SinksMany -- item: 13, result: OK
09:50:45.025 [main] INFO com.example.reactivemaster.sec06.SinksMany -- item: 14, result: OK
09:50:45.025 [main] INFO com.example.reactivemaster.sec06.SinksMany -- item: 15, result: OK
09:50:45.025 [main] INFO com.example.reactivemaster.sec06.SinksMany -- item: 16, result: OK
09:50:45.025 [main] INFO com.example.reactivemaster.sec06.SinksMany -- item: 17, result: OK
09:50:45.025 [main] INFO com.example.reactivemaster.sec06.SinksMany -- item: 18, result: FAIL_OVERFLOW
09:50:45.025 [main] INFO com.example.reactivemaster.sec06.SinksMany -- item: 19, result: FAIL_OVERFLOW
09:50:45.025 [main] INFO com.example.reactivemaster.sec06.SinksMany -- item: 20, result: FAIL_OVERFLOW
09:50:45.025 [main] INFO com.example.reactivemaster.sec06.SinksMany -- item: 21, result: FAIL_OVERFLOW
09:50:45.025 [main] INFO com.example.reactivemaster.sec06.SinksMany -- item: 22, result: FAIL_OVERFLOW
09:50:45.025 [main] INFO com.example.reactivemaster.sec06.SinksMany -- item: 23, result: FAIL_OVERFLOW
09:50:45.025 [main] INFO com.example.reactivemaster.sec06.SinksMany -- item: 24, result: FAIL_OVERFLOW
09:50:45.025 [main] INFO com.example.reactivemaster.sec06.SinksMany -- item: 25, result: FAIL_OVERFLOW
09:50:45.034 [main] INFO com.example.reactivemaster.sec06.SinksMany -- item: 26, result: FAIL_OVERFLOW
09:50:45.034 [main] INFO com.example.reactivemaster.sec06.SinksMany -- item: 27, result: FAIL_OVERFLOW
09:50:45.034 [main] INFO com.example.reactivemaster.sec06.SinksMany -- item: 28, result: FAIL_OVERFLOW
09:50:45.034 [main] INFO com.example.reactivemaster.sec06.SinksMany -- item: 29, result: FAIL_OVERFLOW
09:50:45.034 [main] INFO com.example.reactivemaster.sec06.SinksMany -- item: 30, result: FAIL_OVERFLOW
09:50:45.034 [main] INFO com.example.reactivemaster.sec06.SinksMany -- item: 31, result: FAIL_OVERFLOW
09:50:45.034 [main] INFO com.example.reactivemaster.sec06.SinksMany -- item: 32, result: FAIL_OVERFLOW
09:50:45.034 [main] INFO com.example.reactivemaster.sec06.SinksMany -- item: 33, result: FAIL_OVERFLOW
09:50:45.034 [main] INFO com.example.reactivemaster.sec06.SinksMany -- item: 34, result: FAIL_OVERFLOW
09:50:45.034 [main] INFO com.example.reactivemaster.sec06.SinksMany -- item: 35, result: FAIL_OVERFLOW
09:50:45.034 [main] INFO com.example.reactivemaster.sec06.SinksMany -- item: 36, result: FAIL_OVERFLOW
09:50:45.034 [main] INFO com.example.reactivemaster.sec06.SinksMany -- item: 37, result: FAIL_OVERFLOW
09:50:45.034 [main] INFO com.example.reactivemaster.sec06.SinksMany -- item: 38, result: FAIL_OVERFLOW
09:50:45.034 [main] INFO com.example.reactivemaster.sec06.SinksMany -- item: 39, result: FAIL_OVERFLOW
09:50:45.034 [main] INFO com.example.reactivemaster.sec06.SinksMany -- item: 40, result: FAIL_OVERFLOW
09:50:45.034 [main] INFO com.example.reactivemaster.sec06.SinksMany -- item: 41, result: FAIL_OVERFLOW
09:50:45.034 [main] INFO com.example.reactivemaster.sec06.SinksMany -- item: 42, result: FAIL_OVERFLOW
09:50:45.034 [main] INFO com.example.reactivemaster.sec06.SinksMany -- item: 43, result: FAIL_OVERFLOW
09:50:45.034 [main] INFO com.example.reactivemaster.sec06.SinksMany -- item: 44, result: FAIL_OVERFLOW
09:50:45.034 [main] INFO com.example.reactivemaster.sec06.SinksMany -- item: 45, result: FAIL_OVERFLOW
09:50:45.034 [main] INFO com.example.reactivemaster.sec06.SinksMany -- item: 46, result: FAIL_OVERFLOW
09:50:45.034 [main] INFO com.example.reactivemaster.sec06.SinksMany -- item: 47, result: FAIL_OVERFLOW
09:50:45.034 [main] INFO com.example.reactivemaster.sec06.SinksMany -- item: 48, result: FAIL_OVERFLOW
09:50:45.034 [main] INFO com.example.reactivemaster.sec06.SinksMany -- item: 49, result: FAIL_OVERFLOW
09:50:45.034 [main] INFO com.example.reactivemaster.sec06.SinksMany -- item: 50, result: FAIL_OVERFLOW
09:50:45.241 [parallel-1] INFO com.example.reactivemaster.common.DefaultSub -- mike RECEIVED ITEM: 1
09:50:45.241 [parallel-1] INFO com.example.reactivemaster.common.DefaultSub -- sam RECEIVED ITEM: 2
09:50:45.446 [parallel-2] INFO com.example.reactivemaster.common.DefaultSub -- mike RECEIVED ITEM: 2
09:50:45.446 [parallel-2] INFO com.example.reactivemaster.common.DefaultSub -- sam RECEIVED ITEM: 3
09:50:45.652 [parallel-3] INFO com.example.reactivemaster.common.DefaultSub -- mike RECEIVED ITEM: 3
09:50:45.652 [parallel-3] INFO com.example.reactivemaster.common.DefaultSub -- sam RECEIVED ITEM: 4
09:50:45.856 [parallel-4] INFO com.example.reactivemaster.common.DefaultSub -- mike RECEIVED ITEM: 4
09:50:45.856 [parallel-4] INFO com.example.reactivemaster.common.DefaultSub -- sam RECEIVED ITEM: 5
09:50:46.064 [parallel-5] INFO com.example.reactivemaster.common.DefaultSub -- mike RECEIVED ITEM: 5
09:50:46.065 [parallel-5] INFO com.example.reactivemaster.common.DefaultSub -- sam RECEIVED ITEM: 6
09:50:46.268 [parallel-6] INFO com.example.reactivemaster.common.DefaultSub -- mike RECEIVED ITEM: 6
09:50:46.268 [parallel-6] INFO com.example.reactivemaster.common.DefaultSub -- sam RECEIVED ITEM: 7
09:50:46.475 [parallel-7] INFO com.example.reactivemaster.common.DefaultSub -- mike RECEIVED ITEM: 7
09:50:46.475 [parallel-7] INFO com.example.reactivemaster.common.DefaultSub -- sam RECEIVED ITEM: 8
09:50:46.681 [parallel-8] INFO com.example.reactivemaster.common.DefaultSub -- mike RECEIVED ITEM: 8
09:50:46.681 [parallel-8] INFO com.example.reactivemaster.common.DefaultSub -- sam RECEIVED ITEM: 9
09:50:46.887 [parallel-1] INFO com.example.reactivemaster.common.DefaultSub -- mike RECEIVED ITEM: 9
09:50:46.887 [parallel-1] INFO com.example.reactivemaster.common.DefaultSub -- sam RECEIVED ITEM: 10
09:50:47.095 [parallel-2] INFO com.example.reactivemaster.common.DefaultSub -- mike RECEIVED ITEM: 10
09:50:47.095 [parallel-2] INFO com.example.reactivemaster.common.DefaultSub -- sam RECEIVED ITEM: 11
09:50:47.303 [parallel-3] INFO com.example.reactivemaster.common.DefaultSub -- mike RECEIVED ITEM: 11
09:50:47.303 [parallel-3] INFO com.example.reactivemaster.common.DefaultSub -- sam RECEIVED ITEM: 12
09:50:47.509 [parallel-4] INFO com.example.reactivemaster.common.DefaultSub -- mike RECEIVED ITEM: 12
09:50:47.509 [parallel-4] INFO com.example.reactivemaster.common.DefaultSub -- sam RECEIVED ITEM: 13
09:50:47.715 [parallel-5] INFO com.example.reactivemaster.common.DefaultSub -- mike RECEIVED ITEM: 13
09:50:47.715 [parallel-5] INFO com.example.reactivemaster.common.DefaultSub -- sam RECEIVED ITEM: 14
09:50:47.922 [parallel-6] INFO com.example.reactivemaster.common.DefaultSub -- mike RECEIVED ITEM: 14
09:50:47.922 [parallel-6] INFO com.example.reactivemaster.common.DefaultSub -- sam RECEIVED ITEM: 15
09:50:48.130 [parallel-7] INFO com.example.reactivemaster.common.DefaultSub -- mike RECEIVED ITEM: 15
09:50:48.130 [parallel-7] INFO com.example.reactivemaster.common.DefaultSub -- sam RECEIVED ITEM: 16
09:50:48.340 [parallel-8] INFO com.example.reactivemaster.common.DefaultSub -- mike RECEIVED ITEM: 16
09:50:48.340 [parallel-8] INFO com.example.reactivemaster.common.DefaultSub -- sam RECEIVED ITEM: 17
09:50:48.545 [parallel-1] INFO com.example.reactivemaster.common.DefaultSub -- mike RECEIVED ITEM: 17
         */
    }

    private static void sinkMany_multicast4() {
        System.setProperty("reactor.bufferSize.small", "16");

        // handle through which we would push items
        // onBackPressureBuffer - bounded queue
        var sink = Sinks.many().multicast().onBackpressureBuffer(50);//sam will recieve SOME of the mssages, not ALL

        // handle through which subscribers will receive items
        var flux = sink.asFlux();

        flux.subscribe(Util.subscriber("sam"));
        flux.delayElements(Duration.ofMillis(200)).subscribe(Util.subscriber("mike"));

		/*
			Here sam won't receive ALL the messages, because the other sub is slow
			With multicast().onBackpressureBuffer(), the performance of slow sub affect the performance
			of the fast sub
		 */

        for (int i = 1; i <= 50; i++) {
            var result = sink.tryEmitNext(i);
            log.info("item: {}, result: {}", i, result);
        }

        Util.sleep(10);

        /*
            "C:\Program Files\Java\jdk-22\bin\java.exe" "-javaagent:C:\Program Files\JetBrains\IntelliJ IDEA 2024.2.1\lib\idea_rt.jar=61713:C:\Program Files\JetBrains\IntelliJ IDEA 2024.2.1\bin" -Dfile.encoding=UTF-8 -Dsun.stdout.encoding=UTF-8 -Dsun.stderr.encoding=UTF-8 -classpath C:\Users\hp\Documents\projects\reactiveMaster\target\classes;C:\Users\hp\.m2\repository\org\springframework\boot\spring-boot-starter\3.3.3\spring-boot-starter-3.3.3.jar;C:\Users\hp\.m2\repository\org\springframework\boot\spring-boot\3.3.3\spring-boot-3.3.3.jar;C:\Users\hp\.m2\repository\org\springframework\spring-context\6.1.12\spring-context-6.1.12.jar;C:\Users\hp\.m2\repository\org\springframework\spring-aop\6.1.12\spring-aop-6.1.12.jar;C:\Users\hp\.m2\repository\org\springframework\spring-beans\6.1.12\spring-beans-6.1.12.jar;C:\Users\hp\.m2\repository\org\springframework\spring-expression\6.1.12\spring-expression-6.1.12.jar;C:\Users\hp\.m2\repository\io\micrometer\micrometer-observation\1.13.3\micrometer-observation-1.13.3.jar;C:\Users\hp\.m2\repository\io\micrometer\micrometer-commons\1.13.3\micrometer-commons-1.13.3.jar;C:\Users\hp\.m2\repository\org\springframework\boot\spring-boot-autoconfigure\3.3.3\spring-boot-autoconfigure-3.3.3.jar;C:\Users\hp\.m2\repository\org\springframework\boot\spring-boot-starter-logging\3.3.3\spring-boot-starter-logging-3.3.3.jar;C:\Users\hp\.m2\repository\ch\qos\logback\logback-classic\1.5.7\logback-classic-1.5.7.jar;C:\Users\hp\.m2\repository\ch\qos\logback\logback-core\1.5.7\logback-core-1.5.7.jar;C:\Users\hp\.m2\repository\org\apache\logging\log4j\log4j-to-slf4j\2.23.1\log4j-to-slf4j-2.23.1.jar;C:\Users\hp\.m2\repository\org\apache\logging\log4j\log4j-api\2.23.1\log4j-api-2.23.1.jar;C:\Users\hp\.m2\repository\org\slf4j\jul-to-slf4j\2.0.16\jul-to-slf4j-2.0.16.jar;C:\Users\hp\.m2\repository\jakarta\annotation\jakarta.annotation-api\2.1.1\jakarta.annotation-api-2.1.1.jar;C:\Users\hp\.m2\repository\org\springframework\spring-core\6.1.12\spring-core-6.1.12.jar;C:\Users\hp\.m2\repository\org\springframework\spring-jcl\6.1.12\spring-jcl-6.1.12.jar;C:\Users\hp\.m2\repository\org\yaml\snakeyaml\2.2\snakeyaml-2.2.jar;C:\Users\hp\.m2\repository\io\projectreactor\reactor-core\3.6.4\reactor-core-3.6.4.jar;C:\Users\hp\.m2\repository\org\reactivestreams\reactive-streams\1.0.4\reactive-streams-1.0.4.jar;C:\Users\hp\.m2\repository\org\projectlombok\lombok\1.18.34\lombok-1.18.34.jar;C:\Users\hp\.m2\repository\io\projectreactor\netty\reactor-netty-core\1.1.17\reactor-netty-core-1.1.17.jar;C:\Users\hp\.m2\repository\io\netty\netty-handler\4.1.112.Final\netty-handler-4.1.112.Final.jar;C:\Users\hp\.m2\repository\io\netty\netty-common\4.1.112.Final\netty-common-4.1.112.Final.jar;C:\Users\hp\.m2\repository\io\netty\netty-resolver\4.1.112.Final\netty-resolver-4.1.112.Final.jar;C:\Users\hp\.m2\repository\io\netty\netty-buffer\4.1.112.Final\netty-buffer-4.1.112.Final.jar;C:\Users\hp\.m2\repository\io\netty\netty-transport\4.1.112.Final\netty-transport-4.1.112.Final.jar;C:\Users\hp\.m2\repository\io\netty\netty-transport-native-unix-common\4.1.112.Final\netty-transport-native-unix-common-4.1.112.Final.jar;C:\Users\hp\.m2\repository\io\netty\netty-codec\4.1.112.Final\netty-codec-4.1.112.Final.jar;C:\Users\hp\.m2\repository\io\netty\netty-handler-proxy\4.1.112.Final\netty-handler-proxy-4.1.112.Final.jar;C:\Users\hp\.m2\repository\io\netty\netty-codec-socks\4.1.112.Final\netty-codec-socks-4.1.112.Final.jar;C:\Users\hp\.m2\repository\io\netty\netty-resolver-dns\4.1.112.Final\netty-resolver-dns-4.1.112.Final.jar;C:\Users\hp\.m2\repository\io\netty\netty-codec-dns\4.1.112.Final\netty-codec-dns-4.1.112.Final.jar;C:\Users\hp\.m2\repository\io\netty\netty-resolver-dns-native-macos\4.1.112.Final\netty-resolver-dns-native-macos-4.1.112.Final-osx-x86_64.jar;C:\Users\hp\.m2\repository\io\netty\netty-resolver-dns-classes-macos\4.1.112.Final\netty-resolver-dns-classes-macos-4.1.112.Final.jar;C:\Users\hp\.m2\repository\io\netty\netty-transport-native-epoll\4.1.112.Final\netty-transport-native-epoll-4.1.112.Final-linux-x86_64.jar;C:\Users\hp\.m2\repository\io\netty\netty-transport-classes-epoll\4.1.112.Final\netty-transport-classes-epoll-4.1.112.Final.jar;C:\Users\hp\.m2\repository\io\projectreactor\netty\reactor-netty-http\1.1.17\reactor-netty-http-1.1.17.jar;C:\Users\hp\.m2\repository\io\netty\netty-codec-http\4.1.112.Final\netty-codec-http-4.1.112.Final.jar;C:\Users\hp\.m2\repository\io\netty\netty-codec-http2\4.1.112.Final\netty-codec-http2-4.1.112.Final.jar;C:\Users\hp\.m2\repository\com\github\javafaker\javafaker\1.0.2\javafaker-1.0.2.jar;C:\Users\hp\.m2\repository\org\apache\commons\commons-lang3\3.14.0\commons-lang3-3.14.0.jar;C:\Users\hp\.m2\repository\org\yaml\snakeyaml\1.23\snakeyaml-1.23-android.jar;C:\Users\hp\.m2\repository\com\github\mifmif\generex\1.0.2\generex-1.0.2.jar;C:\Users\hp\.m2\repository\dk\brics\automaton\automaton\1.11-8\automaton-1.11-8.jar;C:\Users\hp\.m2\repository\org\slf4j\slf4j-api\2.0.16\slf4j-api-2.0.16.jar com.example.reactivemaster.sec06.SinksMany
09:53:29.839 [main] INFO com.example.reactivemaster.common.DefaultSub -- sam RECEIVED ITEM: 1
09:53:29.850 [main] INFO com.example.reactivemaster.sec06.SinksMany -- item: 1, result: OK
09:53:29.850 [main] INFO com.example.reactivemaster.sec06.SinksMany -- item: 2, result: OK
09:53:29.850 [main] INFO com.example.reactivemaster.sec06.SinksMany -- item: 3, result: OK
09:53:29.850 [main] INFO com.example.reactivemaster.sec06.SinksMany -- item: 4, result: OK
09:53:29.850 [main] INFO com.example.reactivemaster.sec06.SinksMany -- item: 5, result: OK
09:53:29.850 [main] INFO com.example.reactivemaster.sec06.SinksMany -- item: 6, result: OK
09:53:29.850 [main] INFO com.example.reactivemaster.sec06.SinksMany -- item: 7, result: OK
09:53:29.850 [main] INFO com.example.reactivemaster.sec06.SinksMany -- item: 8, result: OK
09:53:29.850 [main] INFO com.example.reactivemaster.sec06.SinksMany -- item: 9, result: OK
09:53:29.850 [main] INFO com.example.reactivemaster.sec06.SinksMany -- item: 10, result: OK
09:53:29.850 [main] INFO com.example.reactivemaster.sec06.SinksMany -- item: 11, result: OK
09:53:29.850 [main] INFO com.example.reactivemaster.sec06.SinksMany -- item: 12, result: OK
09:53:29.850 [main] INFO com.example.reactivemaster.sec06.SinksMany -- item: 13, result: OK
09:53:29.850 [main] INFO com.example.reactivemaster.sec06.SinksMany -- item: 14, result: OK
09:53:29.850 [main] INFO com.example.reactivemaster.sec06.SinksMany -- item: 15, result: OK
09:53:29.850 [main] INFO com.example.reactivemaster.sec06.SinksMany -- item: 16, result: OK
09:53:29.850 [main] INFO com.example.reactivemaster.sec06.SinksMany -- item: 17, result: OK
09:53:29.850 [main] INFO com.example.reactivemaster.sec06.SinksMany -- item: 18, result: OK
09:53:29.850 [main] INFO com.example.reactivemaster.sec06.SinksMany -- item: 19, result: OK
09:53:29.850 [main] INFO com.example.reactivemaster.sec06.SinksMany -- item: 20, result: OK
09:53:29.850 [main] INFO com.example.reactivemaster.sec06.SinksMany -- item: 21, result: OK
09:53:29.850 [main] INFO com.example.reactivemaster.sec06.SinksMany -- item: 22, result: OK
09:53:29.850 [main] INFO com.example.reactivemaster.sec06.SinksMany -- item: 23, result: OK
09:53:29.850 [main] INFO com.example.reactivemaster.sec06.SinksMany -- item: 24, result: OK
09:53:29.850 [main] INFO com.example.reactivemaster.sec06.SinksMany -- item: 25, result: OK
09:53:29.850 [main] INFO com.example.reactivemaster.sec06.SinksMany -- item: 26, result: OK
09:53:29.850 [main] INFO com.example.reactivemaster.sec06.SinksMany -- item: 27, result: OK
09:53:29.850 [main] INFO com.example.reactivemaster.sec06.SinksMany -- item: 28, result: OK
09:53:29.850 [main] INFO com.example.reactivemaster.sec06.SinksMany -- item: 29, result: OK
09:53:29.850 [main] INFO com.example.reactivemaster.sec06.SinksMany -- item: 30, result: OK
09:53:29.850 [main] INFO com.example.reactivemaster.sec06.SinksMany -- item: 31, result: OK
09:53:29.850 [main] INFO com.example.reactivemaster.sec06.SinksMany -- item: 32, result: OK
09:53:29.850 [main] INFO com.example.reactivemaster.sec06.SinksMany -- item: 33, result: OK
09:53:29.850 [main] INFO com.example.reactivemaster.sec06.SinksMany -- item: 34, result: OK
09:53:29.850 [main] INFO com.example.reactivemaster.sec06.SinksMany -- item: 35, result: OK
09:53:29.850 [main] INFO com.example.reactivemaster.sec06.SinksMany -- item: 36, result: OK
09:53:29.850 [main] INFO com.example.reactivemaster.sec06.SinksMany -- item: 37, result: OK
09:53:29.850 [main] INFO com.example.reactivemaster.sec06.SinksMany -- item: 38, result: OK
09:53:29.850 [main] INFO com.example.reactivemaster.sec06.SinksMany -- item: 39, result: OK
09:53:29.850 [main] INFO com.example.reactivemaster.sec06.SinksMany -- item: 40, result: OK
09:53:29.850 [main] INFO com.example.reactivemaster.sec06.SinksMany -- item: 41, result: OK
09:53:29.850 [main] INFO com.example.reactivemaster.sec06.SinksMany -- item: 42, result: OK
09:53:29.850 [main] INFO com.example.reactivemaster.sec06.SinksMany -- item: 43, result: OK
09:53:29.850 [main] INFO com.example.reactivemaster.sec06.SinksMany -- item: 44, result: OK
09:53:29.850 [main] INFO com.example.reactivemaster.sec06.SinksMany -- item: 45, result: OK
09:53:29.850 [main] INFO com.example.reactivemaster.sec06.SinksMany -- item: 46, result: OK
09:53:29.850 [main] INFO com.example.reactivemaster.sec06.SinksMany -- item: 47, result: OK
09:53:29.850 [main] INFO com.example.reactivemaster.sec06.SinksMany -- item: 48, result: OK
09:53:29.850 [main] INFO com.example.reactivemaster.sec06.SinksMany -- item: 49, result: OK
09:53:29.850 [main] INFO com.example.reactivemaster.sec06.SinksMany -- item: 50, result: OK
09:53:30.052 [parallel-1] INFO com.example.reactivemaster.common.DefaultSub -- mike RECEIVED ITEM: 1
09:53:30.052 [parallel-1] INFO com.example.reactivemaster.common.DefaultSub -- sam RECEIVED ITEM: 2
09:53:30.262 [parallel-2] INFO com.example.reactivemaster.common.DefaultSub -- mike RECEIVED ITEM: 2
09:53:30.262 [parallel-2] INFO com.example.reactivemaster.common.DefaultSub -- sam RECEIVED ITEM: 3
09:53:30.466 [parallel-3] INFO com.example.reactivemaster.common.DefaultSub -- mike RECEIVED ITEM: 3
09:53:30.466 [parallel-3] INFO com.example.reactivemaster.common.DefaultSub -- sam RECEIVED ITEM: 4
09:53:30.672 [parallel-4] INFO com.example.reactivemaster.common.DefaultSub -- mike RECEIVED ITEM: 4
09:53:30.672 [parallel-4] INFO com.example.reactivemaster.common.DefaultSub -- sam RECEIVED ITEM: 5
09:53:30.878 [parallel-5] INFO com.example.reactivemaster.common.DefaultSub -- mike RECEIVED ITEM: 5
09:53:30.878 [parallel-5] INFO com.example.reactivemaster.common.DefaultSub -- sam RECEIVED ITEM: 6
09:53:31.090 [parallel-6] INFO com.example.reactivemaster.common.DefaultSub -- mike RECEIVED ITEM: 6
09:53:31.090 [parallel-6] INFO com.example.reactivemaster.common.DefaultSub -- sam RECEIVED ITEM: 7
09:53:31.298 [parallel-7] INFO com.example.reactivemaster.common.DefaultSub -- mike RECEIVED ITEM: 7
09:53:31.298 [parallel-7] INFO com.example.reactivemaster.common.DefaultSub -- sam RECEIVED ITEM: 8
09:53:31.506 [parallel-8] INFO com.example.reactivemaster.common.DefaultSub -- mike RECEIVED ITEM: 8
09:53:31.506 [parallel-8] INFO com.example.reactivemaster.common.DefaultSub -- sam RECEIVED ITEM: 9
09:53:31.712 [parallel-1] INFO com.example.reactivemaster.common.DefaultSub -- mike RECEIVED ITEM: 9
09:53:31.712 [parallel-1] INFO com.example.reactivemaster.common.DefaultSub -- sam RECEIVED ITEM: 10
09:53:31.920 [parallel-2] INFO com.example.reactivemaster.common.DefaultSub -- mike RECEIVED ITEM: 10
09:53:31.920 [parallel-2] INFO com.example.reactivemaster.common.DefaultSub -- sam RECEIVED ITEM: 11
09:53:32.129 [parallel-3] INFO com.example.reactivemaster.common.DefaultSub -- mike RECEIVED ITEM: 11
09:53:32.129 [parallel-3] INFO com.example.reactivemaster.common.DefaultSub -- sam RECEIVED ITEM: 12
09:53:32.332 [parallel-4] INFO com.example.reactivemaster.common.DefaultSub -- mike RECEIVED ITEM: 12
09:53:32.332 [parallel-4] INFO com.example.reactivemaster.common.DefaultSub -- sam RECEIVED ITEM: 13
09:53:32.542 [parallel-5] INFO com.example.reactivemaster.common.DefaultSub -- mike RECEIVED ITEM: 13
09:53:32.542 [parallel-5] INFO com.example.reactivemaster.common.DefaultSub -- sam RECEIVED ITEM: 14
09:53:32.746 [parallel-6] INFO com.example.reactivemaster.common.DefaultSub -- mike RECEIVED ITEM: 14
09:53:32.746 [parallel-6] INFO com.example.reactivemaster.common.DefaultSub -- sam RECEIVED ITEM: 15
09:53:32.956 [parallel-7] INFO com.example.reactivemaster.common.DefaultSub -- mike RECEIVED ITEM: 15
09:53:32.956 [parallel-7] INFO com.example.reactivemaster.common.DefaultSub -- sam RECEIVED ITEM: 16
09:53:33.164 [parallel-8] INFO com.example.reactivemaster.common.DefaultSub -- mike RECEIVED ITEM: 16
09:53:33.164 [parallel-8] INFO com.example.reactivemaster.common.DefaultSub -- sam RECEIVED ITEM: 17
09:53:33.371 [parallel-1] INFO com.example.reactivemaster.common.DefaultSub -- mike RECEIVED ITEM: 17
09:53:33.371 [parallel-1] INFO com.example.reactivemaster.common.DefaultSub -- sam RECEIVED ITEM: 18
09:53:33.579 [parallel-2] INFO com.example.reactivemaster.common.DefaultSub -- mike RECEIVED ITEM: 18
09:53:33.579 [parallel-2] INFO com.example.reactivemaster.common.DefaultSub -- sam RECEIVED ITEM: 19
09:53:33.786 [parallel-3] INFO com.example.reactivemaster.common.DefaultSub -- mike RECEIVED ITEM: 19
09:53:33.786 [parallel-3] INFO com.example.reactivemaster.common.DefaultSub -- sam RECEIVED ITEM: 20
09:53:33.996 [parallel-4] INFO com.example.reactivemaster.common.DefaultSub -- mike RECEIVED ITEM: 20
09:53:33.996 [parallel-4] INFO com.example.reactivemaster.common.DefaultSub -- sam RECEIVED ITEM: 21
09:53:34.203 [parallel-5] INFO com.example.reactivemaster.common.DefaultSub -- mike RECEIVED ITEM: 21
09:53:34.203 [parallel-5] INFO com.example.reactivemaster.common.DefaultSub -- sam RECEIVED ITEM: 22
09:53:34.410 [parallel-6] INFO com.example.reactivemaster.common.DefaultSub -- mike RECEIVED ITEM: 22
09:53:34.410 [parallel-6] INFO com.example.reactivemaster.common.DefaultSub -- sam RECEIVED ITEM: 23
09:53:34.617 [parallel-7] INFO com.example.reactivemaster.common.DefaultSub -- mike RECEIVED ITEM: 23
09:53:34.617 [parallel-7] INFO com.example.reactivemaster.common.DefaultSub -- sam RECEIVED ITEM: 24
09:53:34.826 [parallel-8] INFO com.example.reactivemaster.common.DefaultSub -- mike RECEIVED ITEM: 24
09:53:34.826 [parallel-8] INFO com.example.reactivemaster.common.DefaultSub -- sam RECEIVED ITEM: 25
09:53:35.035 [parallel-1] INFO com.example.reactivemaster.common.DefaultSub -- mike RECEIVED ITEM: 25
09:53:35.035 [parallel-1] INFO com.example.reactivemaster.common.DefaultSub -- sam RECEIVED ITEM: 26
09:53:35.242 [parallel-2] INFO com.example.reactivemaster.common.DefaultSub -- mike RECEIVED ITEM: 26
09:53:35.242 [parallel-2] INFO com.example.reactivemaster.common.DefaultSub -- sam RECEIVED ITEM: 27
09:53:35.450 [parallel-3] INFO com.example.reactivemaster.common.DefaultSub -- mike RECEIVED ITEM: 27
09:53:35.450 [parallel-3] INFO com.example.reactivemaster.common.DefaultSub -- sam RECEIVED ITEM: 28
09:53:35.655 [parallel-4] INFO com.example.reactivemaster.common.DefaultSub -- mike RECEIVED ITEM: 28
09:53:35.655 [parallel-4] INFO com.example.reactivemaster.common.DefaultSub -- sam RECEIVED ITEM: 29
09:53:35.863 [parallel-5] INFO com.example.reactivemaster.common.DefaultSub -- mike RECEIVED ITEM: 29
09:53:35.863 [parallel-5] INFO com.example.reactivemaster.common.DefaultSub -- sam RECEIVED ITEM: 30
09:53:36.072 [parallel-6] INFO com.example.reactivemaster.common.DefaultSub -- mike RECEIVED ITEM: 30
09:53:36.072 [parallel-6] INFO com.example.reactivemaster.common.DefaultSub -- sam RECEIVED ITEM: 31
09:53:36.281 [parallel-7] INFO com.example.reactivemaster.common.DefaultSub -- mike RECEIVED ITEM: 31
09:53:36.281 [parallel-7] INFO com.example.reactivemaster.common.DefaultSub -- sam RECEIVED ITEM: 32
09:53:36.490 [parallel-8] INFO com.example.reactivemaster.common.DefaultSub -- mike RECEIVED ITEM: 32
09:53:36.490 [parallel-8] INFO com.example.reactivemaster.common.DefaultSub -- sam RECEIVED ITEM: 33
09:53:36.695 [parallel-1] INFO com.example.reactivemaster.common.DefaultSub -- mike RECEIVED ITEM: 33
09:53:36.695 [parallel-1] INFO com.example.reactivemaster.common.DefaultSub -- sam RECEIVED ITEM: 34
09:53:36.902 [parallel-2] INFO com.example.reactivemaster.common.DefaultSub -- mike RECEIVED ITEM: 34
09:53:36.902 [parallel-2] INFO com.example.reactivemaster.common.DefaultSub -- sam RECEIVED ITEM: 35
09:53:37.111 [parallel-3] INFO com.example.reactivemaster.common.DefaultSub -- mike RECEIVED ITEM: 35
09:53:37.111 [parallel-3] INFO com.example.reactivemaster.common.DefaultSub -- sam RECEIVED ITEM: 36
09:53:37.319 [parallel-4] INFO com.example.reactivemaster.common.DefaultSub -- mike RECEIVED ITEM: 36
09:53:37.319 [parallel-4] INFO com.example.reactivemaster.common.DefaultSub -- sam RECEIVED ITEM: 37
09:53:37.524 [parallel-5] INFO com.example.reactivemaster.common.DefaultSub -- mike RECEIVED ITEM: 37
09:53:37.524 [parallel-5] INFO com.example.reactivemaster.common.DefaultSub -- sam RECEIVED ITEM: 38
09:53:37.732 [parallel-6] INFO com.example.reactivemaster.common.DefaultSub -- mike RECEIVED ITEM: 38
09:53:37.732 [parallel-6] INFO com.example.reactivemaster.common.DefaultSub -- sam RECEIVED ITEM: 39
09:53:37.941 [parallel-7] INFO com.example.reactivemaster.common.DefaultSub -- mike RECEIVED ITEM: 39
09:53:37.941 [parallel-7] INFO com.example.reactivemaster.common.DefaultSub -- sam RECEIVED ITEM: 40
09:53:38.146 [parallel-8] INFO com.example.reactivemaster.common.DefaultSub -- mike RECEIVED ITEM: 40
09:53:38.146 [parallel-8] INFO com.example.reactivemaster.common.DefaultSub -- sam RECEIVED ITEM: 41
09:53:38.350 [parallel-1] INFO com.example.reactivemaster.common.DefaultSub -- mike RECEIVED ITEM: 41
09:53:38.350 [parallel-1] INFO com.example.reactivemaster.common.DefaultSub -- sam RECEIVED ITEM: 42
09:53:38.555 [parallel-2] INFO com.example.reactivemaster.common.DefaultSub -- mike RECEIVED ITEM: 42
09:53:38.555 [parallel-2] INFO com.example.reactivemaster.common.DefaultSub -- sam RECEIVED ITEM: 43
09:53:38.761 [parallel-3] INFO com.example.reactivemaster.common.DefaultSub -- mike RECEIVED ITEM: 43
09:53:38.761 [parallel-3] INFO com.example.reactivemaster.common.DefaultSub -- sam RECEIVED ITEM: 44
09:53:38.966 [parallel-4] INFO com.example.reactivemaster.common.DefaultSub -- mike RECEIVED ITEM: 44
09:53:38.966 [parallel-4] INFO com.example.reactivemaster.common.DefaultSub -- sam RECEIVED ITEM: 45
09:53:39.174 [parallel-5] INFO com.example.reactivemaster.common.DefaultSub -- mike RECEIVED ITEM: 45
09:53:39.174 [parallel-5] INFO com.example.reactivemaster.common.DefaultSub -- sam RECEIVED ITEM: 46
09:53:39.379 [parallel-6] INFO com.example.reactivemaster.common.DefaultSub -- mike RECEIVED ITEM: 46
09:53:39.379 [parallel-6] INFO com.example.reactivemaster.common.DefaultSub -- sam RECEIVED ITEM: 47
09:53:39.585 [parallel-7] INFO com.example.reactivemaster.common.DefaultSub -- mike RECEIVED ITEM: 47
09:53:39.585 [parallel-7] INFO com.example.reactivemaster.common.DefaultSub -- sam RECEIVED ITEM: 48
09:53:39.792 [parallel-8] INFO com.example.reactivemaster.common.DefaultSub -- mike RECEIVED ITEM: 48
09:53:39.792 [parallel-8] INFO com.example.reactivemaster.common.DefaultSub -- sam RECEIVED ITEM: 49
         */
    }

    private static void sinkMany_multicast5(){
        System.setProperty("reactor.bufferSize.small", "16");

        // handle through which we would push items
        // onBackPressureBuffer - bounded queue
        var sink = Sinks.many().multicast().directBestEffort(); // sam will receive all the messages

        // handle through which subscribers will receive items
        var flux = sink.asFlux();

        flux.subscribe(Util.subscriber("sam"));
        flux.delayElements(Duration.ofMillis(200)).subscribe(Util.subscriber("mike"));

		/*
			Now Sam will receive ALL the messages, while mike is gonna be ignored
			the sink here will focus on the fast sub only
			If you want mike to the receive the messages as well, see nextMethod
		 */

        for (int i = 1; i <= 50; i++) {
            var result = sink.tryEmitNext(i);
            log.info("item: {}, result: {}", i, result);
        }

        /*
            09:57:32.255 [main] INFO com.example.reactivemaster.common.DefaultSub -- sam RECEIVED ITEM: 1
09:57:32.264 [main] INFO com.example.reactivemaster.sec06.SinksMany -- item: 1, result: OK
09:57:32.264 [main] INFO com.example.reactivemaster.common.DefaultSub -- sam RECEIVED ITEM: 2
09:57:32.264 [main] INFO com.example.reactivemaster.sec06.SinksMany -- item: 2, result: OK
09:57:32.264 [main] INFO com.example.reactivemaster.common.DefaultSub -- sam RECEIVED ITEM: 3
09:57:32.264 [main] INFO com.example.reactivemaster.sec06.SinksMany -- item: 3, result: OK
09:57:32.264 [main] INFO com.example.reactivemaster.common.DefaultSub -- sam RECEIVED ITEM: 4
09:57:32.266 [main] INFO com.example.reactivemaster.sec06.SinksMany -- item: 4, result: OK
09:57:32.266 [main] INFO com.example.reactivemaster.common.DefaultSub -- sam RECEIVED ITEM: 5
09:57:32.266 [main] INFO com.example.reactivemaster.sec06.SinksMany -- item: 5, result: OK
09:57:32.266 [main] INFO com.example.reactivemaster.common.DefaultSub -- sam RECEIVED ITEM: 6
09:57:32.266 [main] INFO com.example.reactivemaster.sec06.SinksMany -- item: 6, result: OK
09:57:32.266 [main] INFO com.example.reactivemaster.common.DefaultSub -- sam RECEIVED ITEM: 7
09:57:32.266 [main] INFO com.example.reactivemaster.sec06.SinksMany -- item: 7, result: OK
09:57:32.266 [main] INFO com.example.reactivemaster.common.DefaultSub -- sam RECEIVED ITEM: 8
09:57:32.266 [main] INFO com.example.reactivemaster.sec06.SinksMany -- item: 8, result: OK
09:57:32.266 [main] INFO com.example.reactivemaster.common.DefaultSub -- sam RECEIVED ITEM: 9
09:57:32.266 [main] INFO com.example.reactivemaster.sec06.SinksMany -- item: 9, result: OK
09:57:32.266 [main] INFO com.example.reactivemaster.common.DefaultSub -- sam RECEIVED ITEM: 10
09:57:32.266 [main] INFO com.example.reactivemaster.sec06.SinksMany -- item: 10, result: OK
09:57:32.266 [main] INFO com.example.reactivemaster.common.DefaultSub -- sam RECEIVED ITEM: 11
09:57:32.266 [main] INFO com.example.reactivemaster.sec06.SinksMany -- item: 11, result: OK
09:57:32.266 [main] INFO com.example.reactivemaster.common.DefaultSub -- sam RECEIVED ITEM: 12
09:57:32.266 [main] INFO com.example.reactivemaster.sec06.SinksMany -- item: 12, result: OK
09:57:32.266 [main] INFO com.example.reactivemaster.common.DefaultSub -- sam RECEIVED ITEM: 13
09:57:32.266 [main] INFO com.example.reactivemaster.sec06.SinksMany -- item: 13, result: OK
09:57:32.266 [main] INFO com.example.reactivemaster.common.DefaultSub -- sam RECEIVED ITEM: 14
09:57:32.266 [main] INFO com.example.reactivemaster.sec06.SinksMany -- item: 14, result: OK
09:57:32.266 [main] INFO com.example.reactivemaster.common.DefaultSub -- sam RECEIVED ITEM: 15
09:57:32.266 [main] INFO com.example.reactivemaster.sec06.SinksMany -- item: 15, result: OK
09:57:32.266 [main] INFO com.example.reactivemaster.common.DefaultSub -- sam RECEIVED ITEM: 16
09:57:32.266 [main] INFO com.example.reactivemaster.sec06.SinksMany -- item: 16, result: OK
09:57:32.266 [main] INFO com.example.reactivemaster.common.DefaultSub -- sam RECEIVED ITEM: 17
09:57:32.266 [main] INFO com.example.reactivemaster.sec06.SinksMany -- item: 17, result: OK
09:57:32.266 [main] INFO com.example.reactivemaster.common.DefaultSub -- sam RECEIVED ITEM: 18
09:57:32.266 [main] INFO com.example.reactivemaster.sec06.SinksMany -- item: 18, result: OK
09:57:32.266 [main] INFO com.example.reactivemaster.common.DefaultSub -- sam RECEIVED ITEM: 19
09:57:32.266 [main] INFO com.example.reactivemaster.sec06.SinksMany -- item: 19, result: OK
09:57:32.266 [main] INFO com.example.reactivemaster.common.DefaultSub -- sam RECEIVED ITEM: 20
09:57:32.266 [main] INFO com.example.reactivemaster.sec06.SinksMany -- item: 20, result: OK
09:57:32.266 [main] INFO com.example.reactivemaster.common.DefaultSub -- sam RECEIVED ITEM: 21
09:57:32.266 [main] INFO com.example.reactivemaster.sec06.SinksMany -- item: 21, result: OK
09:57:32.266 [main] INFO com.example.reactivemaster.common.DefaultSub -- sam RECEIVED ITEM: 22
09:57:32.266 [main] INFO com.example.reactivemaster.sec06.SinksMany -- item: 22, result: OK
09:57:32.266 [main] INFO com.example.reactivemaster.common.DefaultSub -- sam RECEIVED ITEM: 23
09:57:32.266 [main] INFO com.example.reactivemaster.sec06.SinksMany -- item: 23, result: OK
09:57:32.266 [main] INFO com.example.reactivemaster.common.DefaultSub -- sam RECEIVED ITEM: 24
09:57:32.266 [main] INFO com.example.reactivemaster.sec06.SinksMany -- item: 24, result: OK
09:57:32.266 [main] INFO com.example.reactivemaster.common.DefaultSub -- sam RECEIVED ITEM: 25
09:57:32.266 [main] INFO com.example.reactivemaster.sec06.SinksMany -- item: 25, result: OK
09:57:32.266 [main] INFO com.example.reactivemaster.common.DefaultSub -- sam RECEIVED ITEM: 26
09:57:32.266 [main] INFO com.example.reactivemaster.sec06.SinksMany -- item: 26, result: OK
09:57:32.266 [main] INFO com.example.reactivemaster.common.DefaultSub -- sam RECEIVED ITEM: 27
09:57:32.266 [main] INFO com.example.reactivemaster.sec06.SinksMany -- item: 27, result: OK
09:57:32.266 [main] INFO com.example.reactivemaster.common.DefaultSub -- sam RECEIVED ITEM: 28
09:57:32.266 [main] INFO com.example.reactivemaster.sec06.SinksMany -- item: 28, result: OK
09:57:32.266 [main] INFO com.example.reactivemaster.common.DefaultSub -- sam RECEIVED ITEM: 29
09:57:32.266 [main] INFO com.example.reactivemaster.sec06.SinksMany -- item: 29, result: OK
09:57:32.266 [main] INFO com.example.reactivemaster.common.DefaultSub -- sam RECEIVED ITEM: 30
09:57:32.266 [main] INFO com.example.reactivemaster.sec06.SinksMany -- item: 30, result: OK
09:57:32.266 [main] INFO com.example.reactivemaster.common.DefaultSub -- sam RECEIVED ITEM: 31
09:57:32.266 [main] INFO com.example.reactivemaster.sec06.SinksMany -- item: 31, result: OK
09:57:32.266 [main] INFO com.example.reactivemaster.common.DefaultSub -- sam RECEIVED ITEM: 32
09:57:32.266 [main] INFO com.example.reactivemaster.sec06.SinksMany -- item: 32, result: OK
09:57:32.266 [main] INFO com.example.reactivemaster.common.DefaultSub -- sam RECEIVED ITEM: 33
09:57:32.266 [main] INFO com.example.reactivemaster.sec06.SinksMany -- item: 33, result: OK
09:57:32.266 [main] INFO com.example.reactivemaster.common.DefaultSub -- sam RECEIVED ITEM: 34
09:57:32.266 [main] INFO com.example.reactivemaster.sec06.SinksMany -- item: 34, result: OK
09:57:32.266 [main] INFO com.example.reactivemaster.common.DefaultSub -- sam RECEIVED ITEM: 35
09:57:32.266 [main] INFO com.example.reactivemaster.sec06.SinksMany -- item: 35, result: OK
09:57:32.266 [main] INFO com.example.reactivemaster.common.DefaultSub -- sam RECEIVED ITEM: 36
09:57:32.266 [main] INFO com.example.reactivemaster.sec06.SinksMany -- item: 36, result: OK
09:57:32.266 [main] INFO com.example.reactivemaster.common.DefaultSub -- sam RECEIVED ITEM: 37
09:57:32.266 [main] INFO com.example.reactivemaster.sec06.SinksMany -- item: 37, result: OK
09:57:32.266 [main] INFO com.example.reactivemaster.common.DefaultSub -- sam RECEIVED ITEM: 38
09:57:32.266 [main] INFO com.example.reactivemaster.sec06.SinksMany -- item: 38, result: OK
09:57:32.266 [main] INFO com.example.reactivemaster.common.DefaultSub -- sam RECEIVED ITEM: 39
09:57:32.266 [main] INFO com.example.reactivemaster.sec06.SinksMany -- item: 39, result: OK
09:57:32.266 [main] INFO com.example.reactivemaster.common.DefaultSub -- sam RECEIVED ITEM: 40
09:57:32.266 [main] INFO com.example.reactivemaster.sec06.SinksMany -- item: 40, result: OK
09:57:32.266 [main] INFO com.example.reactivemaster.common.DefaultSub -- sam RECEIVED ITEM: 41
09:57:32.266 [main] INFO com.example.reactivemaster.sec06.SinksMany -- item: 41, result: OK
09:57:32.266 [main] INFO com.example.reactivemaster.common.DefaultSub -- sam RECEIVED ITEM: 42
09:57:32.266 [main] INFO com.example.reactivemaster.sec06.SinksMany -- item: 42, result: OK
09:57:32.266 [main] INFO com.example.reactivemaster.common.DefaultSub -- sam RECEIVED ITEM: 43
09:57:32.266 [main] INFO com.example.reactivemaster.sec06.SinksMany -- item: 43, result: OK
09:57:32.266 [main] INFO com.example.reactivemaster.common.DefaultSub -- sam RECEIVED ITEM: 44
09:57:32.266 [main] INFO com.example.reactivemaster.sec06.SinksMany -- item: 44, result: OK
09:57:32.266 [main] INFO com.example.reactivemaster.common.DefaultSub -- sam RECEIVED ITEM: 45
09:57:32.266 [main] INFO com.example.reactivemaster.sec06.SinksMany -- item: 45, result: OK
09:57:32.266 [main] INFO com.example.reactivemaster.common.DefaultSub -- sam RECEIVED ITEM: 46
09:57:32.266 [main] INFO com.example.reactivemaster.sec06.SinksMany -- item: 46, result: OK
09:57:32.266 [main] INFO com.example.reactivemaster.common.DefaultSub -- sam RECEIVED ITEM: 47
09:57:32.266 [main] INFO com.example.reactivemaster.sec06.SinksMany -- item: 47, result: OK
09:57:32.266 [main] INFO com.example.reactivemaster.common.DefaultSub -- sam RECEIVED ITEM: 48
09:57:32.266 [main] INFO com.example.reactivemaster.sec06.SinksMany -- item: 48, result: OK
09:57:32.266 [main] INFO com.example.reactivemaster.common.DefaultSub -- sam RECEIVED ITEM: 49
09:57:32.266 [main] INFO com.example.reactivemaster.sec06.SinksMany -- item: 49, result: OK
09:57:32.266 [main] INFO com.example.reactivemaster.common.DefaultSub -- sam RECEIVED ITEM: 50
09:57:32.266 [main] INFO com.example.reactivemaster.sec06.SinksMany -- item: 50, result: OK
         */
    }

    private static void sinkMany_multicast6(){
        System.setProperty("reactor.bufferSize.small", "16");

        // handle through which we would push items
        // onBackPressureBuffer - bounded queue
        var sink = Sinks.many().multicast().directBestEffort();

        // handle through which subscribers will receive items
        var flux = sink.asFlux();

        flux.subscribe(Util.subscriber("sam"));
        flux
                .onBackpressureBuffer() // telling the sink, "I am a slow sub"
                .delayElements(Duration.ofMillis(200)).subscribe(Util.subscriber("mike"));

		/*
			Now both sam and mike will receive the messages
		 */

        for (int i = 1; i <= 50; i++) {
            var result = sink.tryEmitNext(i);
            log.info("item: {}, result: {}", i, result);
        }

        Util.sleep(10);
        /*
            10:01:37.048 [main] INFO com.example.reactivemaster.common.DefaultSub -- sam RECEIVED ITEM: 1
10:01:37.059 [main] INFO com.example.reactivemaster.sec06.SinksMany -- item: 1, result: OK
10:01:37.059 [main] INFO com.example.reactivemaster.common.DefaultSub -- sam RECEIVED ITEM: 2
10:01:37.059 [main] INFO com.example.reactivemaster.sec06.SinksMany -- item: 2, result: OK
10:01:37.059 [main] INFO com.example.reactivemaster.common.DefaultSub -- sam RECEIVED ITEM: 3
10:01:37.059 [main] INFO com.example.reactivemaster.sec06.SinksMany -- item: 3, result: OK
10:01:37.059 [main] INFO com.example.reactivemaster.common.DefaultSub -- sam RECEIVED ITEM: 4
10:01:37.059 [main] INFO com.example.reactivemaster.sec06.SinksMany -- item: 4, result: OK
10:01:37.059 [main] INFO com.example.reactivemaster.common.DefaultSub -- sam RECEIVED ITEM: 5
10:01:37.059 [main] INFO com.example.reactivemaster.sec06.SinksMany -- item: 5, result: OK
10:01:37.059 [main] INFO com.example.reactivemaster.common.DefaultSub -- sam RECEIVED ITEM: 6
10:01:37.059 [main] INFO com.example.reactivemaster.sec06.SinksMany -- item: 6, result: OK
10:01:37.059 [main] INFO com.example.reactivemaster.common.DefaultSub -- sam RECEIVED ITEM: 7
10:01:37.059 [main] INFO com.example.reactivemaster.sec06.SinksMany -- item: 7, result: OK
10:01:37.059 [main] INFO com.example.reactivemaster.common.DefaultSub -- sam RECEIVED ITEM: 8
10:01:37.059 [main] INFO com.example.reactivemaster.sec06.SinksMany -- item: 8, result: OK
10:01:37.059 [main] INFO com.example.reactivemaster.common.DefaultSub -- sam RECEIVED ITEM: 9
10:01:37.059 [main] INFO com.example.reactivemaster.sec06.SinksMany -- item: 9, result: OK
10:01:37.059 [main] INFO com.example.reactivemaster.common.DefaultSub -- sam RECEIVED ITEM: 10
10:01:37.059 [main] INFO com.example.reactivemaster.sec06.SinksMany -- item: 10, result: OK
10:01:37.059 [main] INFO com.example.reactivemaster.common.DefaultSub -- sam RECEIVED ITEM: 11
10:01:37.059 [main] INFO com.example.reactivemaster.sec06.SinksMany -- item: 11, result: OK
10:01:37.059 [main] INFO com.example.reactivemaster.common.DefaultSub -- sam RECEIVED ITEM: 12
10:01:37.059 [main] INFO com.example.reactivemaster.sec06.SinksMany -- item: 12, result: OK
10:01:37.059 [main] INFO com.example.reactivemaster.common.DefaultSub -- sam RECEIVED ITEM: 13
10:01:37.059 [main] INFO com.example.reactivemaster.sec06.SinksMany -- item: 13, result: OK
10:01:37.060 [main] INFO com.example.reactivemaster.common.DefaultSub -- sam RECEIVED ITEM: 14
10:01:37.060 [main] INFO com.example.reactivemaster.sec06.SinksMany -- item: 14, result: OK
10:01:37.060 [main] INFO com.example.reactivemaster.common.DefaultSub -- sam RECEIVED ITEM: 15
10:01:37.060 [main] INFO com.example.reactivemaster.sec06.SinksMany -- item: 15, result: OK
10:01:37.060 [main] INFO com.example.reactivemaster.common.DefaultSub -- sam RECEIVED ITEM: 16
10:01:37.060 [main] INFO com.example.reactivemaster.sec06.SinksMany -- item: 16, result: OK
10:01:37.060 [main] INFO com.example.reactivemaster.common.DefaultSub -- sam RECEIVED ITEM: 17
10:01:37.060 [main] INFO com.example.reactivemaster.sec06.SinksMany -- item: 17, result: OK
10:01:37.060 [main] INFO com.example.reactivemaster.common.DefaultSub -- sam RECEIVED ITEM: 18
10:01:37.060 [main] INFO com.example.reactivemaster.sec06.SinksMany -- item: 18, result: OK
10:01:37.060 [main] INFO com.example.reactivemaster.common.DefaultSub -- sam RECEIVED ITEM: 19
10:01:37.060 [main] INFO com.example.reactivemaster.sec06.SinksMany -- item: 19, result: OK
10:01:37.060 [main] INFO com.example.reactivemaster.common.DefaultSub -- sam RECEIVED ITEM: 20
10:01:37.060 [main] INFO com.example.reactivemaster.sec06.SinksMany -- item: 20, result: OK
10:01:37.060 [main] INFO com.example.reactivemaster.common.DefaultSub -- sam RECEIVED ITEM: 21
10:01:37.060 [main] INFO com.example.reactivemaster.sec06.SinksMany -- item: 21, result: OK
10:01:37.060 [main] INFO com.example.reactivemaster.common.DefaultSub -- sam RECEIVED ITEM: 22
10:01:37.060 [main] INFO com.example.reactivemaster.sec06.SinksMany -- item: 22, result: OK
10:01:37.060 [main] INFO com.example.reactivemaster.common.DefaultSub -- sam RECEIVED ITEM: 23
10:01:37.060 [main] INFO com.example.reactivemaster.sec06.SinksMany -- item: 23, result: OK
10:01:37.060 [main] INFO com.example.reactivemaster.common.DefaultSub -- sam RECEIVED ITEM: 24
10:01:37.060 [main] INFO com.example.reactivemaster.sec06.SinksMany -- item: 24, result: OK
10:01:37.060 [main] INFO com.example.reactivemaster.common.DefaultSub -- sam RECEIVED ITEM: 25
10:01:37.060 [main] INFO com.example.reactivemaster.sec06.SinksMany -- item: 25, result: OK
10:01:37.060 [main] INFO com.example.reactivemaster.common.DefaultSub -- sam RECEIVED ITEM: 26
10:01:37.060 [main] INFO com.example.reactivemaster.sec06.SinksMany -- item: 26, result: OK
10:01:37.060 [main] INFO com.example.reactivemaster.common.DefaultSub -- sam RECEIVED ITEM: 27
10:01:37.060 [main] INFO com.example.reactivemaster.sec06.SinksMany -- item: 27, result: OK
10:01:37.060 [main] INFO com.example.reactivemaster.common.DefaultSub -- sam RECEIVED ITEM: 28
10:01:37.060 [main] INFO com.example.reactivemaster.sec06.SinksMany -- item: 28, result: OK
10:01:37.060 [main] INFO com.example.reactivemaster.common.DefaultSub -- sam RECEIVED ITEM: 29
10:01:37.060 [main] INFO com.example.reactivemaster.sec06.SinksMany -- item: 29, result: OK
10:01:37.060 [main] INFO com.example.reactivemaster.common.DefaultSub -- sam RECEIVED ITEM: 30
10:01:37.060 [main] INFO com.example.reactivemaster.sec06.SinksMany -- item: 30, result: OK
10:01:37.060 [main] INFO com.example.reactivemaster.common.DefaultSub -- sam RECEIVED ITEM: 31
10:01:37.060 [main] INFO com.example.reactivemaster.sec06.SinksMany -- item: 31, result: OK
10:01:37.060 [main] INFO com.example.reactivemaster.common.DefaultSub -- sam RECEIVED ITEM: 32
10:01:37.060 [main] INFO com.example.reactivemaster.sec06.SinksMany -- item: 32, result: OK
10:01:37.060 [main] INFO com.example.reactivemaster.common.DefaultSub -- sam RECEIVED ITEM: 33
10:01:37.060 [main] INFO com.example.reactivemaster.sec06.SinksMany -- item: 33, result: OK
10:01:37.060 [main] INFO com.example.reactivemaster.common.DefaultSub -- sam RECEIVED ITEM: 34
10:01:37.060 [main] INFO com.example.reactivemaster.sec06.SinksMany -- item: 34, result: OK
10:01:37.060 [main] INFO com.example.reactivemaster.common.DefaultSub -- sam RECEIVED ITEM: 35
10:01:37.060 [main] INFO com.example.reactivemaster.sec06.SinksMany -- item: 35, result: OK
10:01:37.060 [main] INFO com.example.reactivemaster.common.DefaultSub -- sam RECEIVED ITEM: 36
10:01:37.060 [main] INFO com.example.reactivemaster.sec06.SinksMany -- item: 36, result: OK
10:01:37.060 [main] INFO com.example.reactivemaster.common.DefaultSub -- sam RECEIVED ITEM: 37
10:01:37.060 [main] INFO com.example.reactivemaster.sec06.SinksMany -- item: 37, result: OK
10:01:37.060 [main] INFO com.example.reactivemaster.common.DefaultSub -- sam RECEIVED ITEM: 38
10:01:37.060 [main] INFO com.example.reactivemaster.sec06.SinksMany -- item: 38, result: OK
10:01:37.060 [main] INFO com.example.reactivemaster.common.DefaultSub -- sam RECEIVED ITEM: 39
10:01:37.060 [main] INFO com.example.reactivemaster.sec06.SinksMany -- item: 39, result: OK
10:01:37.060 [main] INFO com.example.reactivemaster.common.DefaultSub -- sam RECEIVED ITEM: 40
10:01:37.060 [main] INFO com.example.reactivemaster.sec06.SinksMany -- item: 40, result: OK
10:01:37.060 [main] INFO com.example.reactivemaster.common.DefaultSub -- sam RECEIVED ITEM: 41
10:01:37.060 [main] INFO com.example.reactivemaster.sec06.SinksMany -- item: 41, result: OK
10:01:37.060 [main] INFO com.example.reactivemaster.common.DefaultSub -- sam RECEIVED ITEM: 42
10:01:37.060 [main] INFO com.example.reactivemaster.sec06.SinksMany -- item: 42, result: OK
10:01:37.060 [main] INFO com.example.reactivemaster.common.DefaultSub -- sam RECEIVED ITEM: 43
10:01:37.060 [main] INFO com.example.reactivemaster.sec06.SinksMany -- item: 43, result: OK
10:01:37.060 [main] INFO com.example.reactivemaster.common.DefaultSub -- sam RECEIVED ITEM: 44
10:01:37.060 [main] INFO com.example.reactivemaster.sec06.SinksMany -- item: 44, result: OK
10:01:37.060 [main] INFO com.example.reactivemaster.common.DefaultSub -- sam RECEIVED ITEM: 45
10:01:37.060 [main] INFO com.example.reactivemaster.sec06.SinksMany -- item: 45, result: OK
10:01:37.060 [main] INFO com.example.reactivemaster.common.DefaultSub -- sam RECEIVED ITEM: 46
10:01:37.060 [main] INFO com.example.reactivemaster.sec06.SinksMany -- item: 46, result: OK
10:01:37.060 [main] INFO com.example.reactivemaster.common.DefaultSub -- sam RECEIVED ITEM: 47
10:01:37.060 [main] INFO com.example.reactivemaster.sec06.SinksMany -- item: 47, result: OK
10:01:37.060 [main] INFO com.example.reactivemaster.common.DefaultSub -- sam RECEIVED ITEM: 48
10:01:37.060 [main] INFO com.example.reactivemaster.sec06.SinksMany -- item: 48, result: OK
10:01:37.060 [main] INFO com.example.reactivemaster.common.DefaultSub -- sam RECEIVED ITEM: 49
10:01:37.060 [main] INFO com.example.reactivemaster.sec06.SinksMany -- item: 49, result: OK
10:01:37.060 [main] INFO com.example.reactivemaster.common.DefaultSub -- sam RECEIVED ITEM: 50
10:01:37.060 [main] INFO com.example.reactivemaster.sec06.SinksMany -- item: 50, result: OK
10:01:37.262 [parallel-1] INFO com.example.reactivemaster.common.DefaultSub -- mike RECEIVED ITEM: 1
10:01:37.467 [parallel-2] INFO com.example.reactivemaster.common.DefaultSub -- mike RECEIVED ITEM: 2
10:01:37.674 [parallel-3] INFO com.example.reactivemaster.common.DefaultSub -- mike RECEIVED ITEM: 3
10:01:37.880 [parallel-4] INFO com.example.reactivemaster.common.DefaultSub -- mike RECEIVED ITEM: 4
10:01:38.088 [parallel-5] INFO com.example.reactivemaster.common.DefaultSub -- mike RECEIVED ITEM: 5
10:01:38.294 [parallel-6] INFO com.example.reactivemaster.common.DefaultSub -- mike RECEIVED ITEM: 6
10:01:38.499 [parallel-7] INFO com.example.reactivemaster.common.DefaultSub -- mike RECEIVED ITEM: 7
10:01:38.703 [parallel-8] INFO com.example.reactivemaster.common.DefaultSub -- mike RECEIVED ITEM: 8
10:01:38.907 [parallel-1] INFO com.example.reactivemaster.common.DefaultSub -- mike RECEIVED ITEM: 9
10:01:39.113 [parallel-2] INFO com.example.reactivemaster.common.DefaultSub -- mike RECEIVED ITEM: 10
10:01:39.317 [parallel-3] INFO com.example.reactivemaster.common.DefaultSub -- mike RECEIVED ITEM: 11
10:01:39.521 [parallel-4] INFO com.example.reactivemaster.common.DefaultSub -- mike RECEIVED ITEM: 12
10:01:39.725 [parallel-5] INFO com.example.reactivemaster.common.DefaultSub -- mike RECEIVED ITEM: 13
10:01:39.929 [parallel-6] INFO com.example.reactivemaster.common.DefaultSub -- mike RECEIVED ITEM: 14
10:01:40.133 [parallel-7] INFO com.example.reactivemaster.common.DefaultSub -- mike RECEIVED ITEM: 15
10:01:40.339 [parallel-8] INFO com.example.reactivemaster.common.DefaultSub -- mike RECEIVED ITEM: 16
10:01:40.545 [parallel-1] INFO com.example.reactivemaster.common.DefaultSub -- mike RECEIVED ITEM: 17
10:01:40.749 [parallel-2] INFO com.example.reactivemaster.common.DefaultSub -- mike RECEIVED ITEM: 18
10:01:40.953 [parallel-3] INFO com.example.reactivemaster.common.DefaultSub -- mike RECEIVED ITEM: 19
10:01:41.159 [parallel-4] INFO com.example.reactivemaster.common.DefaultSub -- mike RECEIVED ITEM: 20
10:01:41.362 [parallel-5] INFO com.example.reactivemaster.common.DefaultSub -- mike RECEIVED ITEM: 21
10:01:41.567 [parallel-6] INFO com.example.reactivemaster.common.DefaultSub -- mike RECEIVED ITEM: 22
10:01:41.774 [parallel-7] INFO com.example.reactivemaster.common.DefaultSub -- mike RECEIVED ITEM: 23
10:01:41.980 [parallel-8] INFO com.example.reactivemaster.common.DefaultSub -- mike RECEIVED ITEM: 24
10:01:42.187 [parallel-1] INFO com.example.reactivemaster.common.DefaultSub -- mike RECEIVED ITEM: 25
10:01:42.393 [parallel-2] INFO com.example.reactivemaster.common.DefaultSub -- mike RECEIVED ITEM: 26
10:01:42.600 [parallel-3] INFO com.example.reactivemaster.common.DefaultSub -- mike RECEIVED ITEM: 27
10:01:42.808 [parallel-4] INFO com.example.reactivemaster.common.DefaultSub -- mike RECEIVED ITEM: 28
10:01:43.013 [parallel-5] INFO com.example.reactivemaster.common.DefaultSub -- mike RECEIVED ITEM: 29
10:01:43.222 [parallel-6] INFO com.example.reactivemaster.common.DefaultSub -- mike RECEIVED ITEM: 30
10:01:43.427 [parallel-7] INFO com.example.reactivemaster.common.DefaultSub -- mike RECEIVED ITEM: 31
10:01:43.634 [parallel-8] INFO com.example.reactivemaster.common.DefaultSub -- mike RECEIVED ITEM: 32
10:01:43.838 [parallel-1] INFO com.example.reactivemaster.common.DefaultSub -- mike RECEIVED ITEM: 33
10:01:44.043 [parallel-2] INFO com.example.reactivemaster.common.DefaultSub -- mike RECEIVED ITEM: 34
10:01:44.249 [parallel-3] INFO com.example.reactivemaster.common.DefaultSub -- mike RECEIVED ITEM: 35
10:01:44.454 [parallel-4] INFO com.example.reactivemaster.common.DefaultSub -- mike RECEIVED ITEM: 36
10:01:44.664 [parallel-5] INFO com.example.reactivemaster.common.DefaultSub -- mike RECEIVED ITEM: 37
10:01:44.871 [parallel-6] INFO com.example.reactivemaster.common.DefaultSub -- mike RECEIVED ITEM: 38
10:01:45.080 [parallel-7] INFO com.example.reactivemaster.common.DefaultSub -- mike RECEIVED ITEM: 39
10:01:45.289 [parallel-8] INFO com.example.reactivemaster.common.DefaultSub -- mike RECEIVED ITEM: 40
10:01:45.496 [parallel-1] INFO com.example.reactivemaster.common.DefaultSub -- mike RECEIVED ITEM: 41
10:01:45.709 [parallel-2] INFO com.example.reactivemaster.common.DefaultSub -- mike RECEIVED ITEM: 42
10:01:45.917 [parallel-3] INFO com.example.reactivemaster.common.DefaultSub -- mike RECEIVED ITEM: 43
10:01:46.123 [parallel-4] INFO com.example.reactivemaster.common.DefaultSub -- mike RECEIVED ITEM: 44
10:01:46.332 [parallel-5] INFO com.example.reactivemaster.common.DefaultSub -- mike RECEIVED ITEM: 45
10:01:46.538 [parallel-6] INFO com.example.reactivemaster.common.DefaultSub -- mike RECEIVED ITEM: 46
10:01:46.745 [parallel-7] INFO com.example.reactivemaster.common.DefaultSub -- mike RECEIVED ITEM: 47
10:01:46.953 [parallel-8] INFO com.example.reactivemaster.common.DefaultSub -- mike RECEIVED ITEM: 48
         */

    }

    private static void sinkMany_multicast7(){
        System.setProperty("reactor.bufferSize.small", "16");

        // handle through which we would push items
        // onBackPressureBuffer - bounded queue
        var sink = Sinks.many().multicast().directAllOrNothing(); // if a sub is slow, don't deliver to anyone!

        // handle through which subscribers will receive items
        var flux = sink.asFlux();

        flux.subscribe(Util.subscriber("sam"));
        flux.delayElements(Duration.ofMillis(200)).subscribe(Util.subscriber("mike"));

		/*
			Here both sam and mike won't receive any messages, since one of them is slow
		 */

        for (int i = 1; i <= 50; i++) {
            var result = sink.tryEmitNext(i);
            log.info("item: {}, result: {}", i, result);
        }

        Util.sleep(10);

        /*
            10:03:37.799 [main] INFO com.example.reactivemaster.common.DefaultSub -- sam RECEIVED ITEM: 1
10:03:37.809 [main] INFO com.example.reactivemaster.sec06.SinksMany -- item: 1, result: OK
10:03:37.809 [main] INFO com.example.reactivemaster.sec06.SinksMany -- item: 2, result: FAIL_OVERFLOW
10:03:37.809 [main] INFO com.example.reactivemaster.sec06.SinksMany -- item: 3, result: FAIL_OVERFLOW
10:03:37.809 [main] INFO com.example.reactivemaster.sec06.SinksMany -- item: 4, result: FAIL_OVERFLOW
10:03:37.809 [main] INFO com.example.reactivemaster.sec06.SinksMany -- item: 5, result: FAIL_OVERFLOW
10:03:37.809 [main] INFO com.example.reactivemaster.sec06.SinksMany -- item: 6, result: FAIL_OVERFLOW
10:03:37.809 [main] INFO com.example.reactivemaster.sec06.SinksMany -- item: 7, result: FAIL_OVERFLOW
10:03:37.809 [main] INFO com.example.reactivemaster.sec06.SinksMany -- item: 8, result: FAIL_OVERFLOW
10:03:37.809 [main] INFO com.example.reactivemaster.sec06.SinksMany -- item: 9, result: FAIL_OVERFLOW
10:03:37.809 [main] INFO com.example.reactivemaster.sec06.SinksMany -- item: 10, result: FAIL_OVERFLOW
10:03:37.809 [main] INFO com.example.reactivemaster.sec06.SinksMany -- item: 11, result: FAIL_OVERFLOW
10:03:37.809 [main] INFO com.example.reactivemaster.sec06.SinksMany -- item: 12, result: FAIL_OVERFLOW
10:03:37.809 [main] INFO com.example.reactivemaster.sec06.SinksMany -- item: 13, result: FAIL_OVERFLOW
10:03:37.809 [main] INFO com.example.reactivemaster.sec06.SinksMany -- item: 14, result: FAIL_OVERFLOW
10:03:37.812 [main] INFO com.example.reactivemaster.sec06.SinksMany -- item: 15, result: FAIL_OVERFLOW
10:03:37.812 [main] INFO com.example.reactivemaster.sec06.SinksMany -- item: 16, result: FAIL_OVERFLOW
10:03:37.812 [main] INFO com.example.reactivemaster.sec06.SinksMany -- item: 17, result: FAIL_OVERFLOW
10:03:37.812 [main] INFO com.example.reactivemaster.sec06.SinksMany -- item: 18, result: FAIL_OVERFLOW
10:03:37.812 [main] INFO com.example.reactivemaster.sec06.SinksMany -- item: 19, result: FAIL_OVERFLOW
10:03:37.812 [main] INFO com.example.reactivemaster.sec06.SinksMany -- item: 20, result: FAIL_OVERFLOW
10:03:37.812 [main] INFO com.example.reactivemaster.sec06.SinksMany -- item: 21, result: FAIL_OVERFLOW
10:03:37.812 [main] INFO com.example.reactivemaster.sec06.SinksMany -- item: 22, result: FAIL_OVERFLOW
10:03:37.812 [main] INFO com.example.reactivemaster.sec06.SinksMany -- item: 23, result: FAIL_OVERFLOW
10:03:37.812 [main] INFO com.example.reactivemaster.sec06.SinksMany -- item: 24, result: FAIL_OVERFLOW
10:03:37.812 [main] INFO com.example.reactivemaster.sec06.SinksMany -- item: 25, result: FAIL_OVERFLOW
10:03:37.812 [main] INFO com.example.reactivemaster.sec06.SinksMany -- item: 26, result: FAIL_OVERFLOW
10:03:37.812 [main] INFO com.example.reactivemaster.sec06.SinksMany -- item: 27, result: FAIL_OVERFLOW
10:03:37.812 [main] INFO com.example.reactivemaster.sec06.SinksMany -- item: 28, result: FAIL_OVERFLOW
10:03:37.812 [main] INFO com.example.reactivemaster.sec06.SinksMany -- item: 29, result: FAIL_OVERFLOW
10:03:37.812 [main] INFO com.example.reactivemaster.sec06.SinksMany -- item: 30, result: FAIL_OVERFLOW
10:03:37.812 [main] INFO com.example.reactivemaster.sec06.SinksMany -- item: 31, result: FAIL_OVERFLOW
10:03:37.812 [main] INFO com.example.reactivemaster.sec06.SinksMany -- item: 32, result: FAIL_OVERFLOW
10:03:37.812 [main] INFO com.example.reactivemaster.sec06.SinksMany -- item: 33, result: FAIL_OVERFLOW
10:03:37.812 [main] INFO com.example.reactivemaster.sec06.SinksMany -- item: 34, result: FAIL_OVERFLOW
10:03:37.812 [main] INFO com.example.reactivemaster.sec06.SinksMany -- item: 35, result: FAIL_OVERFLOW
10:03:37.812 [main] INFO com.example.reactivemaster.sec06.SinksMany -- item: 36, result: FAIL_OVERFLOW
10:03:37.812 [main] INFO com.example.reactivemaster.sec06.SinksMany -- item: 37, result: FAIL_OVERFLOW
10:03:37.812 [main] INFO com.example.reactivemaster.sec06.SinksMany -- item: 38, result: FAIL_OVERFLOW
10:03:37.812 [main] INFO com.example.reactivemaster.sec06.SinksMany -- item: 39, result: FAIL_OVERFLOW
10:03:37.812 [main] INFO com.example.reactivemaster.sec06.SinksMany -- item: 40, result: FAIL_OVERFLOW
10:03:37.812 [main] INFO com.example.reactivemaster.sec06.SinksMany -- item: 41, result: FAIL_OVERFLOW
10:03:37.812 [main] INFO com.example.reactivemaster.sec06.SinksMany -- item: 42, result: FAIL_OVERFLOW
10:03:37.812 [main] INFO com.example.reactivemaster.sec06.SinksMany -- item: 43, result: FAIL_OVERFLOW
10:03:37.812 [main] INFO com.example.reactivemaster.sec06.SinksMany -- item: 44, result: FAIL_OVERFLOW
10:03:37.813 [main] INFO com.example.reactivemaster.sec06.SinksMany -- item: 45, result: FAIL_OVERFLOW
10:03:37.813 [main] INFO com.example.reactivemaster.sec06.SinksMany -- item: 46, result: FAIL_OVERFLOW
10:03:37.813 [main] INFO com.example.reactivemaster.sec06.SinksMany -- item: 47, result: FAIL_OVERFLOW
10:03:37.813 [main] INFO com.example.reactivemaster.sec06.SinksMany -- item: 48, result: FAIL_OVERFLOW
10:03:37.813 [main] INFO com.example.reactivemaster.sec06.SinksMany -- item: 49, result: FAIL_OVERFLOW
10:03:37.813 [main] INFO com.example.reactivemaster.sec06.SinksMany -- item: 50, result: FAIL_OVERFLOW
10:03:38.013 [parallel-1] INFO com.example.reactivemaster.common.DefaultSub -- mike RECEIVED ITEM: 1
         */
    }

    public static void sinkMany_replay(){
        var sink = Sinks.many().replay().all();
        var flux = sink.asFlux();
        flux.subscribe(Util.subscriber("sub1"));
        flux.subscribe(Util.subscriber("sub2"));

        sink.tryEmitNext("Hi");
        sink.tryEmitNext("Hola");
        sink.tryEmitNext("HELLO");

        Util.sleep(10);

        flux.subscribe(Util.subscriber("sub3"));

        sink.tryEmitNext("new Message");
		/*
			With sink replay, we can emit many values,
			we can have more subs.
			ALL subs will receive ALL the messages, be it early subs or past ones
			Sometimes you requirement requires the late sub should be able to see
			only the last 2 messages or messages of the last 2 minutes
			if so, see next method
		 */

        /*
            10:06:57.255 [main] INFO com.example.reactivemaster.common.DefaultSub -- sub1 RECEIVED ITEM: Hi
10:06:57.259 [main] INFO com.example.reactivemaster.common.DefaultSub -- sub2 RECEIVED ITEM: Hi
10:06:57.259 [main] INFO com.example.reactivemaster.common.DefaultSub -- sub1 RECEIVED ITEM: Hola
10:06:57.259 [main] INFO com.example.reactivemaster.common.DefaultSub -- sub2 RECEIVED ITEM: Hola
10:06:57.259 [main] INFO com.example.reactivemaster.common.DefaultSub -- sub1 RECEIVED ITEM: HELLO
10:06:57.259 [main] INFO com.example.reactivemaster.common.DefaultSub -- sub2 RECEIVED ITEM: HELLO
10:07:07.263 [main] INFO com.example.reactivemaster.common.DefaultSub -- sub3 RECEIVED ITEM: Hi
10:07:07.263 [main] INFO com.example.reactivemaster.common.DefaultSub -- sub3 RECEIVED ITEM: Hola
10:07:07.263 [main] INFO com.example.reactivemaster.common.DefaultSub -- sub3 RECEIVED ITEM: HELLO
10:07:07.263 [main] INFO com.example.reactivemaster.common.DefaultSub -- sub1 RECEIVED ITEM: new Message
10:07:07.263 [main] INFO com.example.reactivemaster.common.DefaultSub -- sub2 RECEIVED ITEM: new Message
10:07:07.263 [main] INFO com.example.reactivemaster.common.DefaultSub -- sub3 RECEIVED ITEM: new Message
         */
    }

    public static void sinkMany_replay2(){
        var sink = Sinks.many().replay().limit(1);
        var flux = sink.asFlux();
        flux.subscribe(Util.subscriber("sub1"));
        flux.subscribe(Util.subscriber("sub2"));

        sink.tryEmitNext("msg1");
        sink.tryEmitNext("msg2");
        sink.tryEmitNext("msg3");

        Util.sleep(2);

        flux.subscribe(Util.subscriber("sub3"));

        sink.tryEmitNext("msg4");
		/*
			Now sub3 only gonna see 'msg3' and 'msg4'
		 */

        /*
            "C:\Program Files\Java\jdk-22\bin\java.exe" "-javaagent:C:\Program Files\JetBrains\IntelliJ IDEA 2024.2.1\lib\idea_rt.jar=61785:C:\Program Files\JetBrains\IntelliJ IDEA 2024.2.1\bin" -Dfile.encoding=UTF-8 -Dsun.stdout.encoding=UTF-8 -Dsun.stderr.encoding=UTF-8 -classpath C:\Users\hp\Documents\projects\reactiveMaster\target\classes;C:\Users\hp\.m2\repository\org\springframework\boot\spring-boot-starter\3.3.3\spring-boot-starter-3.3.3.jar;C:\Users\hp\.m2\repository\org\springframework\boot\spring-boot\3.3.3\spring-boot-3.3.3.jar;C:\Users\hp\.m2\repository\org\springframework\spring-context\6.1.12\spring-context-6.1.12.jar;C:\Users\hp\.m2\repository\org\springframework\spring-aop\6.1.12\spring-aop-6.1.12.jar;C:\Users\hp\.m2\repository\org\springframework\spring-beans\6.1.12\spring-beans-6.1.12.jar;C:\Users\hp\.m2\repository\org\springframework\spring-expression\6.1.12\spring-expression-6.1.12.jar;C:\Users\hp\.m2\repository\io\micrometer\micrometer-observation\1.13.3\micrometer-observation-1.13.3.jar;C:\Users\hp\.m2\repository\io\micrometer\micrometer-commons\1.13.3\micrometer-commons-1.13.3.jar;C:\Users\hp\.m2\repository\org\springframework\boot\spring-boot-autoconfigure\3.3.3\spring-boot-autoconfigure-3.3.3.jar;C:\Users\hp\.m2\repository\org\springframework\boot\spring-boot-starter-logging\3.3.3\spring-boot-starter-logging-3.3.3.jar;C:\Users\hp\.m2\repository\ch\qos\logback\logback-classic\1.5.7\logback-classic-1.5.7.jar;C:\Users\hp\.m2\repository\ch\qos\logback\logback-core\1.5.7\logback-core-1.5.7.jar;C:\Users\hp\.m2\repository\org\apache\logging\log4j\log4j-to-slf4j\2.23.1\log4j-to-slf4j-2.23.1.jar;C:\Users\hp\.m2\repository\org\apache\logging\log4j\log4j-api\2.23.1\log4j-api-2.23.1.jar;C:\Users\hp\.m2\repository\org\slf4j\jul-to-slf4j\2.0.16\jul-to-slf4j-2.0.16.jar;C:\Users\hp\.m2\repository\jakarta\annotation\jakarta.annotation-api\2.1.1\jakarta.annotation-api-2.1.1.jar;C:\Users\hp\.m2\repository\org\springframework\spring-core\6.1.12\spring-core-6.1.12.jar;C:\Users\hp\.m2\repository\org\springframework\spring-jcl\6.1.12\spring-jcl-6.1.12.jar;C:\Users\hp\.m2\repository\org\yaml\snakeyaml\2.2\snakeyaml-2.2.jar;C:\Users\hp\.m2\repository\io\projectreactor\reactor-core\3.6.4\reactor-core-3.6.4.jar;C:\Users\hp\.m2\repository\org\reactivestreams\reactive-streams\1.0.4\reactive-streams-1.0.4.jar;C:\Users\hp\.m2\repository\org\projectlombok\lombok\1.18.34\lombok-1.18.34.jar;C:\Users\hp\.m2\repository\io\projectreactor\netty\reactor-netty-core\1.1.17\reactor-netty-core-1.1.17.jar;C:\Users\hp\.m2\repository\io\netty\netty-handler\4.1.112.Final\netty-handler-4.1.112.Final.jar;C:\Users\hp\.m2\repository\io\netty\netty-common\4.1.112.Final\netty-common-4.1.112.Final.jar;C:\Users\hp\.m2\repository\io\netty\netty-resolver\4.1.112.Final\netty-resolver-4.1.112.Final.jar;C:\Users\hp\.m2\repository\io\netty\netty-buffer\4.1.112.Final\netty-buffer-4.1.112.Final.jar;C:\Users\hp\.m2\repository\io\netty\netty-transport\4.1.112.Final\netty-transport-4.1.112.Final.jar;C:\Users\hp\.m2\repository\io\netty\netty-transport-native-unix-common\4.1.112.Final\netty-transport-native-unix-common-4.1.112.Final.jar;C:\Users\hp\.m2\repository\io\netty\netty-codec\4.1.112.Final\netty-codec-4.1.112.Final.jar;C:\Users\hp\.m2\repository\io\netty\netty-handler-proxy\4.1.112.Final\netty-handler-proxy-4.1.112.Final.jar;C:\Users\hp\.m2\repository\io\netty\netty-codec-socks\4.1.112.Final\netty-codec-socks-4.1.112.Final.jar;C:\Users\hp\.m2\repository\io\netty\netty-resolver-dns\4.1.112.Final\netty-resolver-dns-4.1.112.Final.jar;C:\Users\hp\.m2\repository\io\netty\netty-codec-dns\4.1.112.Final\netty-codec-dns-4.1.112.Final.jar;C:\Users\hp\.m2\repository\io\netty\netty-resolver-dns-native-macos\4.1.112.Final\netty-resolver-dns-native-macos-4.1.112.Final-osx-x86_64.jar;C:\Users\hp\.m2\repository\io\netty\netty-resolver-dns-classes-macos\4.1.112.Final\netty-resolver-dns-classes-macos-4.1.112.Final.jar;C:\Users\hp\.m2\repository\io\netty\netty-transport-native-epoll\4.1.112.Final\netty-transport-native-epoll-4.1.112.Final-linux-x86_64.jar;C:\Users\hp\.m2\repository\io\netty\netty-transport-classes-epoll\4.1.112.Final\netty-transport-classes-epoll-4.1.112.Final.jar;C:\Users\hp\.m2\repository\io\projectreactor\netty\reactor-netty-http\1.1.17\reactor-netty-http-1.1.17.jar;C:\Users\hp\.m2\repository\io\netty\netty-codec-http\4.1.112.Final\netty-codec-http-4.1.112.Final.jar;C:\Users\hp\.m2\repository\io\netty\netty-codec-http2\4.1.112.Final\netty-codec-http2-4.1.112.Final.jar;C:\Users\hp\.m2\repository\com\github\javafaker\javafaker\1.0.2\javafaker-1.0.2.jar;C:\Users\hp\.m2\repository\org\apache\commons\commons-lang3\3.14.0\commons-lang3-3.14.0.jar;C:\Users\hp\.m2\repository\org\yaml\snakeyaml\1.23\snakeyaml-1.23-android.jar;C:\Users\hp\.m2\repository\com\github\mifmif\generex\1.0.2\generex-1.0.2.jar;C:\Users\hp\.m2\repository\dk\brics\automaton\automaton\1.11-8\automaton-1.11-8.jar;C:\Users\hp\.m2\repository\org\slf4j\slf4j-api\2.0.16\slf4j-api-2.0.16.jar com.example.reactivemaster.sec06.SinksMany
10:08:40.882 [main] INFO com.example.reactivemaster.common.DefaultSub -- sub1 RECEIVED ITEM: msg1
10:08:40.887 [main] INFO com.example.reactivemaster.common.DefaultSub -- sub2 RECEIVED ITEM: msg1
10:08:40.888 [main] INFO com.example.reactivemaster.common.DefaultSub -- sub1 RECEIVED ITEM: msg2
10:08:40.888 [main] INFO com.example.reactivemaster.common.DefaultSub -- sub2 RECEIVED ITEM: msg2
10:08:40.888 [main] INFO com.example.reactivemaster.common.DefaultSub -- sub1 RECEIVED ITEM: msg3
10:08:40.888 [main] INFO com.example.reactivemaster.common.DefaultSub -- sub2 RECEIVED ITEM: msg3
10:08:42.901 [main] INFO com.example.reactivemaster.common.DefaultSub -- sub3 RECEIVED ITEM: msg3
10:08:42.902 [main] INFO com.example.reactivemaster.common.DefaultSub -- sub1 RECEIVED ITEM: msg4
10:08:42.902 [main] INFO com.example.reactivemaster.common.DefaultSub -- sub2 RECEIVED ITEM: msg4
10:08:42.902 [main] INFO com.example.reactivemaster.common.DefaultSub -- sub3 RECEIVED ITEM: msg4
         */
    }

    


}
