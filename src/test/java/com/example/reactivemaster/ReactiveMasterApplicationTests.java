package com.example.reactivemaster;

import com.example.reactivemaster.common.Util;
import com.example.reactivemaster.sec01.subscriber.SubscriberImpl;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import reactor.core.Disposable;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.publisher.Sinks;
import reactor.test.StepVerifier;
import reactor.util.retry.Retry;

import java.text.SimpleDateFormat;
import java.time.Duration;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Predicate;
import java.util.function.UnaryOperator;
import java.util.stream.Collectors;

@Slf4j
class ReactiveMasterApplicationTests {
    private AtomicBoolean publisherExecuted = new AtomicBoolean(false);
    private AtomicBoolean publisherCreated = new AtomicBoolean(false);

    private int sum(List<Integer> list){
        publisherExecuted.set(true);
        return list.stream().mapToInt(x->x).sum();
    }

    private Mono<Integer> createPub(){
        publisherCreated.set(true);
        return Mono.fromSupplier(() -> sum(List.of(1,2,3)));
    }

    private Mono<Integer> createMonoJustPublisher(){
        publisherCreated.set(true);
        return Mono.just(sum(List.of(1,2)));
    }

    @Test
    void just(){

        var mono = createMonoJustPublisher();

        assert publisherExecuted.get() && publisherCreated.get();

        mono.as(StepVerifier::create)
                .expectNext(3)
                .verifyComplete();


    }

    @Test
    void from_Supplier(){

        var mono = createPub();

        assert !publisherExecuted.get() && publisherCreated.get();

        mono.as(StepVerifier::create)
                .expectNext(6)
                .verifyComplete();

        assert publisherExecuted.get();


    }

    @Test
    void defer(){
        var mono = Mono.defer(this::createPub);
        assert !publisherExecuted.get() && !publisherCreated.get();

        mono.as(StepVerifier::create)
                .expectNext(6)
                .verifyComplete();

        assert publisherExecuted.get() && publisherCreated.get();
    }

    private List<Integer> createList(){
        publisherExecuted.set(true);
        return List.of(1,2,3);
    }

    private Flux<Integer> createFlux(){
        publisherCreated.set(true);
        return Flux.fromIterable(createList());
    }

    @Test
    void fluxDefer(){
        var flux = Flux.defer(this::createFlux);
        assert !publisherExecuted.get() && !publisherCreated.get();
        flux.as(StepVerifier::create)
                .expectNext(1,2,3)
                .verifyComplete();
        assert publisherExecuted.get() && publisherCreated.get();
    }

    @Test
    void name() {
        Mono.just("Hello World")
                .as(StepVerifier::create)
                //      .consumeNextWith(x-> Assertions.assertTrue(x.contains("World")))
                //      .expectNextMatches(s -> s.startsWith("H"))
                //    .expectNextCount(1)
                    .expectNext("Hello World")
                .verifyComplete();
    }

    @Test
    void monoFromFlux(){
        Flux.range(1,5)
                .as(Mono::from)
                .as(StepVerifier::create)
                .expectNext(1)
                .verifyComplete();
    }

    @Test
    void fluxFromMono(){
        Mono.just("ab")
                .as(Flux::from)
                .as(StepVerifier::create)
                .expectNext("ab")
                .verifyComplete();
    }

    @Test
    void fluxCreate(){
        var flux = Flux.<String>create(fluxSink -> {
            String country;
            do{
                country = Util.faker().country().name();
                fluxSink.next(country);
            }while(!country.equalsIgnoreCase("canada"));
            fluxSink.complete();
        });

        flux
                .collectList()
                .as(StepVerifier::create)
                .assertNext(list ->list.stream().noneMatch(x->x.equalsIgnoreCase("canada")))
                .verifyComplete();
    }

    @Test
    void listThreadUnsafe(){
        var list = new ArrayList<Integer>();

        Runnable runnable = () ->{
          for(int i=1;i<=1000;i++){
              list.add(i);
          }
        };

        for(int i=1;i<=10;i++){
            Thread.ofPlatform().start(runnable);
        }

        assert list.size() != 10000;
    }

    @Test
    void fluxCreateDefaultBehavior(){
        var sub = new SubscriberImpl();

        Flux.<String>create(fluxSink -> {
            for(int i=1;i<10;i++){
                var name = Util.faker().name().fullName();
                publisherExecuted.set(true);
                fluxSink.next(name);
            }
            fluxSink.complete();
        }).subscribe(sub);

        sub.getSubscription().cancel();
        assert publisherExecuted.get();

        /*
            Flux Create Default behavior is to create the data
            and stores it in a queue, then when a sub requests data
            it gives it to them
            In this case, the Flux.create() generates 10 names before any
            request demand
         */
    }

    @Test
    void fluxRequestOnDemand(){
        var sub = new SubscriberImpl();

        Flux.<String>create(fluxSink -> {
           fluxSink.onRequest(request ->{
               for(int i=1;i<=request && !fluxSink.isCancelled();i++){
                   var name = Util.faker().name().fullName();
                   publisherExecuted.set(true);
                   fluxSink.next(name);
               }
               fluxSink.complete();
           });
        }).subscribe(sub);
        sub.getSubscription().cancel();
        assert !publisherExecuted.get();

        /*
            Now Flux Create doesn't create the data UNTIL the subscriber requests
            when he does, it creates the data with the amount the sub wants.
         */
    }

    @Test
    void synchronousSink(){
        Flux.generate(synchronousSink -> {
            synchronousSink.next(1);
            synchronousSink.complete();
        })
                .as(StepVerifier::create)
                .expectNext(1)
                .verifyComplete();
    }

    @Test
    void synchronousSink1(){
        Flux.generate(synchronousSink -> {
                    synchronousSink.next(1);
                })
                .take(3)
                .as(StepVerifier::create)
                .expectNext(1,1,1)
                .verifyComplete();
    }

    @Test
    void synchronousSink2(){
        Flux.generate(synchronousSink -> {
                    synchronousSink.next(1);
                    synchronousSink.error(new RuntimeException());
                })
                .take(3)
                .as(StepVerifier::create)
                .expectNext(1)
                .verifyError(RuntimeException.class);
    }

    @Test
    void synchronousSink3(){
        Flux.<String>generate(synchronousSink -> {
            var name = Util.faker().country().name();
            synchronousSink.next(name);
        })
                .takeUntil(x->x.equalsIgnoreCase("canada"))
                .collectList()
                .as(StepVerifier::create)
                .assertNext(list ->list.stream().noneMatch(x->x.equalsIgnoreCase("canada")))
                .verifyComplete();
    }

    @Test
    void synchronousSink4(){
        Predicate<List<String>> predicate1 = list -> list.stream().noneMatch(x->x.equalsIgnoreCase("canada"));
        Predicate<List<String>> predicate = predicate1.or(list ->list.size()==10);
        Flux.generate(
                ()->0,
                (counter,sink) ->{
                    var country = Util.faker().country().name();
                    sink.next(country);
                    counter++;
                    if(counter==10 || country.equalsIgnoreCase("canada")){
                        sink.complete();
                    }
                    return counter;
                }
        )
                .cast(String.class)
                .collectList()
                .as(StepVerifier::create)
                .assertNext(predicate::test)
                .verifyComplete();

    }

    @Test
    void handle(){
        Flux.range(1,5)
                .handle((item,sink) ->{
                    switch (item){
                        case 1 -> sink.next(99);
                        case 2 -> {}
                        case 5 -> sink.error(new RuntimeException());
                        default -> sink.next(item);
                    }
                })
                .as(StepVerifier::create)
                .expectNext(99,3,4)
                .verifyError(RuntimeException.class);
    }

    @Test
    void handle1(){
        Flux.<String>generate(synchronousSink -> {
            var country = Util.faker().country().name();
            synchronousSink.next(country);
        })
                .<String>handle((country,sink) ->{
                    if(country.equalsIgnoreCase("canada")){
                        sink.complete();
                    }
                })
                .collectList()
                .as(StepVerifier::create)
                .assertNext(list ->list.stream().noneMatch(x->x.equalsIgnoreCase("canada")))
                .verifyComplete();
    }

    @Test
    void error(){
        Mono.error(new RuntimeException("Error"))
                .as(StepVerifier::create)
                .verifyErrorMatches(throwable -> throwable instanceof RuntimeException && throwable.getMessage().equals("Error"));
        //   .verifyErrorMessage("Error");
        //     .verifyError(RuntimeException.class);
    }

    @Test
    void take(){
        Flux.interval(Duration.ofSeconds(1))
                .take(2)
                .as(StepVerifier::create)
                .expectNext(0L,1L)
                .verifyComplete();
    }

    @Test
    void interval(){
        Flux.interval(Duration.ofSeconds(1))
                .map(x->"Order-"+x)
                .take(2)
                .as(StepVerifier::create)
                .thenAwait(Duration.ofSeconds(3))
                .expectNext("Order-0","Order-1")
                .verifyComplete();
    }

    @Test
    void intervalWithDelay(){
        System.out.println("BEFORE EMITTING VALUES AT: "+  LocalTime.now());
        Flux.interval(Duration.ofSeconds(2),Duration.ofSeconds(2))
                .take(5)
                .doOnNext(x-> System.out.println("EMITTED VALUE: "  + x + " AT :" + LocalTime.now()))
                .then()
                .as(StepVerifier::create)
                .verifyComplete();

        //BEFORE EMITTING VALUES AT: 08:45:05.183200800
        //EMITTED VALUE: 0 AT :08:45:07.436889800
        //EMITTED VALUE: 1 AT :08:45:09.449696400
        //EMITTED VALUE: 2 AT :08:45:11.443515200
        //EMITTED VALUE: 3 AT :08:45:13.445795900
        //EMITTED VALUE: 4 AT :08:45:15.437316900

    }

    @Test
    void onErrorReturn(){
        Flux.range(1,5)
                .map(x->x==4?(x/0):x)
                .onErrorReturn(ArithmeticException.class,-1)
                .as(StepVerifier::create)
                .expectNext(1,2,3,-1)
                .verifyComplete();
    }

    @Test
    void onErrorResume2(){
        Flux.range(1,5)
                .map(x->x==4?(x/0):x)
                .onErrorResume(ex ->Mono.just(-1))
           //     .onErrorResume(ArithmeticException.class,ex-> Mono.just(-1))
                .as(StepVerifier::create)
                .expectNext(1,2,3,-1)
                .verifyComplete();
    }

    @Test
    void flatMap(){
        Flux.range(1,3)
                .flatMap(i -> Mono.just(i*i))
                .as(StepVerifier::create)
                .expectNext(1,4,9)
                .verifyComplete();
    }

    @Test
    void concatMap(){
        Flux.range(1,3)
                .concatMap(i -> Mono.just(i*i))
                .as(StepVerifier::create)
                .expectNext(1,4,9)
                .verifyComplete();
    }

    @Test
    void map(){
        Flux.just("Orange","Banana","Lemon")
                .map(String::toUpperCase)
                .as(StepVerifier::create)
                .expectNext("ORANGE","BANANA","LEMON")
                .verifyComplete();
    }

    @Test
    void onErrorResume(){
        Mono.error(new RuntimeException("Error"))
                .onErrorResume(throwable -> Mono.just(1))
                .as(StepVerifier::create)
                .expectNext(1)
                .verifyComplete();
    }

    @Test
    void onErrorComplete(){
        Flux.error(new IllegalArgumentException("Error"))
                .onErrorComplete()
                .as(StepVerifier::create)
                .verifyComplete();
    }

    @Test
    void switchIfEmpty(){
        Flux.empty()
                .switchIfEmpty(Mono.just(1))
                .as(StepVerifier::create)
                .expectNext(1)
                .verifyComplete();
    }


    @Test
    void defaultIfEmpty(){
        Flux.<String>empty()
                .defaultIfEmpty("String")
                .as(StepVerifier::create)
                .expectNext("String")
                .verifyComplete();
    }

    @Test
    void onErrorMap(){
        Mono.error(new RuntimeException("Error"))
                .onErrorMap(ex->new IllegalArgumentException("Error"))
                .as(StepVerifier::create)
                .verifyError(IllegalArgumentException.class);
    }

    @Test
    void onErrorContinue(){
        Flux.range(1,4)
                .map(i->{
                    if(i==3) throw new RuntimeException("Error");
                    return i;
                })
                .onErrorContinue((error,obj)->log.info("Error on value: {}",obj))
                .as(StepVerifier::create)
                .expectNext(1,2,4)
                .verifyComplete();
    }

    @Test
    void fromIterable(){
        Flux.fromIterable(List.of(1,2,3))
                .as(StepVerifier::create)
                .expectNext(1,2,3)
                .verifyComplete();
    }

    @Test
    void concat(){
        Flux.concat(Flux.just(1,2,3),Flux.just(4,5,6))
                .as(StepVerifier::create)
                .expectNext(1,2,3,4,5,6)
                .verifyComplete();
    }

    @Test
    void concatWithValues(){
        Flux.range(1,5)
                .concatWithValues(-2,-6)
                .concatWith(Flux.just(44,22))
                .as(StepVerifier::create)
                .expectNext(1,2,3,4,5,-2,-6,44,22)
                .verifyComplete();
    }

    @Test
    void merge(){
        Flux.merge(Flux.just(1,2,3),Flux.just(4,5,6))
                .as(StepVerifier::create)
                .expectNext(1,2,3,4,5,6)
                .verifyComplete();
    }

    @Test
    void filter(){
        Flux.range(1,30)
                .filter(i->i%2==0)
                .collectList()
                .as(StepVerifier::create)
                .expectNextMatches(x->x.stream().allMatch(y->y%2==0))
                .verifyComplete();
    }

    @Test
    void skip(){
        Flux.range(1,3)
                .skip(2)
                .as(StepVerifier::create)
                .expectNext(3)
                .verifyComplete();
    }

    @Test
    void buffer(){
        Flux.range(1,6)
                .buffer()
                .as(StepVerifier::create)
                .expectNext(List.of(1,2,3,4,5,6))
                .verifyComplete();
    }

    @Test
    void buffer1(){
        Flux.range(1,6)
                .buffer(2)
                .as(StepVerifier::create)
                .expectNext(List.of(1,2),List.of(3,4),List.of(5,6))
                .verifyComplete();
    }

    @Test
    void buffer2(){
        Flux.range(1,6)
                .buffer(3)
                .as(StepVerifier::create)
                .expectNext(List.of(1,2,3),List.of(4,5,6))
                .verifyComplete();
    }

    @Test
    void buffer3(){
        Flux.range(1,6)
                .delayElements(Duration.ofSeconds(1))
                .buffer(Duration.ofSeconds(4))
                .as(StepVerifier::create)
                .expectNext(List.of(1,2,3))
                .expectNext(List.of(4,5,6))
                .expectComplete()
                .verify(Duration.ofSeconds(7));
    }

    @Test
    void buffer4(){
        Flux.range(1,6)
                .concatWith(Flux.never())
                .bufferTimeout(2,Duration.ofSeconds(1))
                .as(StepVerifier::create)
                .expectNext(List.of(1, 2), List.of(3, 4), List.of(5, 6))
                .thenCancel()
                .verify();
    }

    @Test
    void bufferUntil(){
        Flux.just("ab","cd","xy","ab","dd","mm","ab")
                .bufferUntil(x->x.equals("ab"))
                .as(StepVerifier::create)
                .expectNext(List.of("ab"),List.of("cd","xy","ab"), List.of("dd","mm","ab"))
                .verifyComplete();
    }


    @Test
    void fromSupplier(){
        Mono.fromSupplier(() -> "Hello World")
                .as(StepVerifier::create)
                .expectNext("Hello World")
                .verifyComplete();
    }

    @Test
    void repeat(){
        Flux.just(2,4)
                .repeat(2)
                .as(StepVerifier::create)
                //     .expectNextCount(6)
                .expectNext(2,4,2,4,2,4)
                .verifyComplete();
    }

    @Test
    void repeat1(){
        Mono.just(2)
                .repeat(3)
                .as(StepVerifier::create)
                .expectNextCount(4)
                .verifyComplete();
    }

    @Test
    void repeat2(){
        Mono.fromSupplier(() ->Util.faker().country().name())
                .repeat()
                .takeUntil(country -> country.equalsIgnoreCase("canada"))
                .collectList()
                .as(StepVerifier::create)
                .assertNext(list -> list.stream().noneMatch(x->x.equalsIgnoreCase("canada")))
                .verifyComplete();
    }

    @Test
    void repeat3(){
        var atomicInteger = new AtomicInteger(0);
        Mono.fromSupplier(() ->Util.faker().country().name())
                .repeat(() -> atomicInteger.incrementAndGet()<3)
                .as(StepVerifier::create)
                .expectNextCount(3)
                .verifyComplete();
    }

    @Test
    void repeat4(){
        Mono.fromSupplier(() ->1)
                .delayElement(Duration.ofSeconds(1))
                .repeatWhen(flux -> flux.delayElements(Duration.ofSeconds(2)))
                .take(Duration.ofSeconds(14))
                .as(StepVerifier::create)
                .expectNextCount(5)
                .verifyComplete();

        /*
            the 1st one will take 1 sec
            the last 4 will take 3 sec each = 12
            so it will take 13 secs
            add one more second
         */
    }

    @Test
    void retry(){
        var atomicInteger = new AtomicInteger(0);
        Mono.fromSupplier(() ->{
            if(atomicInteger.incrementAndGet()<3){
                throw new RuntimeException("Error");
            }
            return "ok";
        })
                .retry(1)
                .as(StepVerifier::create)
                .verifyError();

    }

    @Test
    void retry1(){
        var atomicInteger = new AtomicInteger(0);
        Mono.fromSupplier(() ->{
                    if(atomicInteger.incrementAndGet()<3){
                        throw new RuntimeException("Error");
                    }
                    return "ok";
                })
                .retry(2)
                .as(StepVerifier::create)
                .expectNext("ok")
                .verifyComplete();

    }

    @Test
    void retry2(){
        var atomicInteger = new AtomicInteger(0);
        Mono.fromSupplier(() ->{
                    if(atomicInteger.incrementAndGet()<3){
                        throw new RuntimeException("Error");
                    }
                    return "ok";
                })
                .retryWhen(Retry.fixedDelay(
                        2,Duration.ofSeconds(1)) // retry 2 times, 2 secs between each
                .filter(ex-> RuntimeException.class.equals(ex.getClass())) // ONLY Retry when the error is runtime exception
                .onRetryExhaustedThrow((spec,signal) -> signal.failure()) // Show the original exception instead of retryExhausted exception
                .doBeforeRetry(rs-> log.info("RETRYING")))
                .as(StepVerifier::create)
                .expectNext("ok")
                .verifyComplete();

    }

    @Test
    void sinkOneEmpty(){
        var sink = Sinks.one();
        var mono = sink.asMono();
        sink.tryEmitEmpty();
        mono
                .as(StepVerifier::create)
                .verifyComplete();

    }

    @Test
    void sinkOneValue(){
        var sink = Sinks.one();
        var mono = sink.asMono();
        sink.tryEmitValue("hi");
        mono
                .as(StepVerifier::create)
                .expectNext("hi")
                .verifyComplete();

    }

    @Test
    void sinkOneError(){
        var sink = Sinks.one();
        var mono = sink.asMono();
        sink.tryEmitError(new RuntimeException());
        mono
                .as(StepVerifier::create)
                .verifyError(RuntimeException.class);
    }

    @Test
    void sinkOneMultipleSubs(){
        var sink = Sinks.one();
        var mono = sink.asMono();
        sink.tryEmitValue("hi");
        mono
                .as(StepVerifier::create)
                .expectNext("hi")
                .verifyComplete();

        mono
                .as(StepVerifier::create)
                .expectNext("hi")
                .verifyComplete();

    }


    @Test
    void takeUntil(){
        Flux.range(1,10)
                .takeUntil(i->i%7==0)
                .as(StepVerifier::create)
                .expectNext(1,2,3,4,5,6,7)
                .verifyComplete();
    }

    @Test
    void takeWhile(){
        Flux.range(1,10)
                .takeWhile(i->i!=10)
                .as(StepVerifier::create)
                .expectNextCount(9)
                .verifyComplete();
    }

    @Test
    void firstWithValue(){
        Flux.firstWithValue(Flux.empty(),Flux.just(1,2,3))
                .as(StepVerifier::create)
                .expectNext(1,2,3)
                .verifyComplete();
    }

    @Test
    void delayElements(){
        Flux.range(1,4)
                .delayElements(Duration.ofSeconds(1))
                .as(StepVerifier::create)
                .thenAwait(Duration.ofSeconds(1))
                .expectNext(1,2,3,4)
                .verifyComplete();
    }

    @Test
    void reduce(){
        Flux.just(1,2,3)
                .reduce(Integer::sum)
                .as(StepVerifier::create)
                .expectNext(6)
                .verifyComplete();
    }

    @Test
    void collectMap(){
        Flux.interval(Duration.ofMillis(100))
                .map(i-> Util.faker().name().firstName())
                .take(10)
                .collectMap(key->key,String::length)
                .as(StepVerifier::create)
                .expectNextMatches(x->{
                    var firstKey = x.keySet().stream().findFirst();
                    var firstValue = x.values().stream().findFirst();
                    return firstKey.get().length() ==(firstValue.get());
                }).verifyComplete();
    }

    @Test
    void collect(){
        Flux.just("a","b","a","d","b","b","e","b")
                .collect(Collectors.groupingBy(
                        word -> word,
                        Collectors.counting()
                ))
                .doOnNext(System.out::println)
                .then()
                .as(StepVerifier::create)
                .verifyComplete();

        // {a=2, b=4, d=1, e=1}
    }

    @Test
    void collect2(){
        Flux.just("ab","bbb","a","aaad","dddb","aaaaaab","mmmme","ssssb")
                .collect(Collectors.groupingBy(
                        String::length,
                        Collectors.toList()
                ))
                .doOnNext(System.out::println)
                .then()
                .as(StepVerifier::create)
                .verifyComplete();

        // {1=[a], 2=[ab], 3=[bbb], 4=[aaad, dddb], 5=[mmmme, ssssb], 7=[aaaaaab]}
    }

    @Test
    void collectSortedList(){
        Flux.just(2,8,5,1,3)
                .collectSortedList()
                .doOnNext(System.out::println)
                .then()
                .as(StepVerifier::create)
                .verifyComplete();

        //[1, 2, 3, 5, 8]
    }

    private Flux<String> process(String data){
        return Flux.range(1,3)
                .map(i->data+i);
    }

    @Test
    void flatMapMany(){
        Mono.just("word")
                .flatMapMany(this::process)
                .as(StepVerifier::create)
                .expectNext("word1","word2","word3")
                .verifyComplete();

    }

    @Test
    void then(){
        Mono.just("78")
                .then(Mono.just("AB"))
                .as(StepVerifier::create)
                .expectNext("AB")
                .verifyComplete();
    }

    @Test
    void thenMany(){
        Mono.just("78")
                .thenMany(Flux.just("AB","cc"))
                .as(StepVerifier::create)
                .expectNext("AB","cc")
                .verifyComplete();
    }

    @Test
    void window(){
        Flux.range(1,6)
                .window(2)
                .flatMap(Flux::collectList)
                .as(StepVerifier::create)
                .expectNext(List.of(1,2),List.of(3,4),List.of(5,6))
                .verifyComplete();
    }

    @Test
    void switchMap(){
        Flux.range(1,2)
                .switchMap(i->Flux.just(i,i*i))
                .as(StepVerifier::create)
                .expectNext(1,1,2,4)
                .verifyComplete();
    }

    @Test
    void count(){
        Flux.range(1,10)
                .count()
                .as(StepVerifier::create)
                .expectNext(10L)
                .verifyComplete();
    }

    @Test
    void takeLast(){
        Flux.range(1,10)
                .takeLast(2)
                .as(StepVerifier::create)
                .expectNext(9,10)
                .verifyComplete();
    }

    @Test
    void timeout(){
        Flux.range(1,10)
                .delayElements(Duration.ofSeconds(4))
                .timeout(Duration.ofSeconds(1))
                .as(StepVerifier::create)
                .expectSubscription()
                .expectError(TimeoutException.class)
                .verify(Duration.ofSeconds(2));

        Flux.just("ok")
                .delayElements(Duration.ofSeconds(1))
                .timeout(Duration.ofSeconds(2))
                .as(StepVerifier::create)
                .expectSubscription()
                .expectNext("ok")
                .expectComplete()
                .verify(Duration.ofSeconds(3));

    }

    @Test
    void timeout1(){
        Mono.just("ok")
                .delayElement(Duration.ofSeconds(5))
                .timeout(Duration.ofSeconds(1),Mono.just("ab"))
                .as(StepVerifier::create)
                .expectSubscription()
                .expectNext("ab")
                .expectComplete()
                .verify(Duration.ofSeconds(2));
    }

    @Test
    void timeout2(){
        Mono.just("ok")
                .delayElement(Duration.ofSeconds(5))
                .timeout(Duration.ofSeconds(1),Mono.just("ab"))
                .timeout(Duration.ofMillis(200),Mono.just("xy"))
                .as(StepVerifier::create)
                .expectSubscription()
                .expectNext("xy")
                .expectComplete()
                .verify(Duration.ofMillis(300));
    }

    @Test
    void startWith(){
        Flux.range(1,2)
                .startWith(44)
                .as(StepVerifier::create)
                .expectNext(44,1,2)
                .verifyComplete();
    }

    @Test
    void startWith1(){
        Flux.range(1,5)
                .startWith(List.of(0,-1,-4))
                .take(2)
                .as(StepVerifier::create)
                .expectNext(0,-1)
                .verifyComplete();
    }

    @Test
    void startWith2(){
        var producer1 = Flux.just(11,22,55);
        var producer2 = Flux.just(33,88,54)
                .startWith(producer1)
                .startWith(1000)
                .as(StepVerifier::create)
                .expectNext(1000,11,22,55,33,88,54)
                .verifyComplete();
    }

    @Test
    void concatDelayError(){
        Flux.concatDelayError(Flux.just(1,2),Flux.error(new RuntimeException()),Flux.just(0))
                .as(StepVerifier::create)
                .expectNext(1,2,0)
                .verifyError(RuntimeException.class);
    }

    @Test
    void create(){
        Flux.create(fluxSink -> {
                    fluxSink.next(1);
                    fluxSink.next(2);
                    fluxSink.complete();
                }).as(StepVerifier::create)
                .expectNext(1,2)
                .verifyComplete();
    }

    private UnaryOperator<Flux<Integer>> addMapper(){
        return flux -> flux.map(x->x*2);
    }

    @Test
    void transform(){
        Flux.range(1,3)
                .transform(addMapper())
                .as(StepVerifier::create)
                .expectNext(2,4,6)
                .verifyComplete();
    }

    @Test
    void coldPublisher(){
        var flux = Flux.interval(Duration.ofSeconds(1))
                .take(5);

        flux
                .as(StepVerifier::create)
                .expectNext(0L,1l,2l,3l,4l)
                .verifyComplete();

        Util.sleep(2);

        flux
                .as(StepVerifier::create)
                .expectNext(0L,1l,2l,3l,4l)
                .verifyComplete();

    }

    record Car(String body, String engine, String tires){}
    @Test
    void zip(){
        var bodyFlux = Flux.range(1,5)
                .map(x->"Body-"+x);
        var engineFlux = Flux.range(1,3)
                .map(x->"Engine-"+x);
        var tiresFlux = Flux.range(1,10)
                .map(x->"Tire-"+x);

        Flux.zip(bodyFlux,engineFlux,tiresFlux)
                .map(x->new Car(x.getT1(),x.getT2(),x.getT3()))
                .as(StepVerifier::create)
                .expectNext(new Car("Body-1","Engine-1","Tire-1"),
                        new Car("Body-2","Engine-2","Tire-2"),
                        new Car("Body-3","Engine-3","Tire-3"))
                .verifyComplete();

    }

    @Test
    void distinct(){
        Flux.just(1,22,24,22)
                .distinct()
                .as(StepVerifier::create)
                .expectNext(1,22,24)
                .verifyComplete();
    }

    @Test
    void all(){
        Flux.just("a","ali","ab","ayoub")
                .all(x->x.startsWith("a"))
                .as(StepVerifier::create)
                .expectNext(true)
                .verifyComplete();
    }

    @Test
    void any(){
        Flux.just("a","ali","ab","ayoub")
                .any(x->x.endsWith("b"))
                .as(StepVerifier::create)
                .expectNext(true)
                .verifyComplete();
    }

    @Test
    void cache(){
        Mono<String> cache = Mono.just("data")
                .doOnSubscribe(x -> System.out.println("SOURCE SUBSCRIBED"))
                .cache();
        cache
                .doFirst(()-> System.out.println("********************"))
                .then()
                .as(StepVerifier::create)
                .verifyComplete();

        cache
                .doFirst(()-> System.out.println("********************"))
                .then()
                .as(StepVerifier::create)
                .verifyComplete();

        // 'SOURCE SUBSCRIBED' was printed only once
        // because in the 2nd time he used the cached value
        /*
                ********************
                SOURCE SUBSCRIBED
                ********************
         */

    }

    @Test
    void cacheWithDuration(){
        Mono<String> cache = Mono.just("data")
                .doOnSubscribe(x -> System.out.println("SOURCE SUBSCRIBED"))
                .cache(Duration.ofSeconds(2));

        cache
                .doFirst(()-> System.out.println("********************"))
                .then()
                .as(StepVerifier::create)
                .verifyComplete(); //'SOURCE SUBSCRIBED' was printed here since it's first time subscription

        Util.sleep(1);

        cache
                .doFirst(()-> System.out.println("********************"))
                .then()
                .as(StepVerifier::create)
                .verifyComplete(); //'SOURCE SUBSCRIBED' was NOT printed here, since the duration didn't expire

        Util.sleep(1);

        cache
                .doFirst(()-> System.out.println("********************"))
                .then()
                .as(StepVerifier::create)
                .verifyComplete(); //'SOURCE SUBSCRIBED' was printed here, since the duration expired.

        /*
            ********************
            SOURCE SUBSCRIBED
            ********************
            ********************
            SOURCE SUBSCRIBED
         */

    }

    @Test
    void cacheWithHistory(){
        Flux<Integer> flux = Flux.range(1, 5)
                .doOnSubscribe(x -> System.out.println("SOURCE SUBSCRIBED"))
                .cache(3);

        flux.as(StepVerifier::create)
                .expectNext(1,2,3,4,5)
                .verifyComplete(); // the 1st sub will get all the values, SOURCE SUBSCRIBED was printed here

        flux.as(StepVerifier::create)
                .expectNext(3,4,5)
                .verifyComplete(); // after the 1st, any sub will get only the last 3 cached values. SOURCE SUBSCRIBED was NOT printed
    }

    @Test
    void cacheWithHistoryAndDuration(){
        Flux<Integer> flux = Flux.range(1, 5)
                .doOnSubscribe(x -> System.out.println("SOURCE SUBSCRIBED"))
                .cache(3, Duration.ofSeconds(2));

        flux
                .doFirst(()-> System.out.println("********************"))
                .as(StepVerifier::create)
                .expectNext(1,2,3,4,5)
                .verifyComplete(); // the 1st sub will get all the values, SOURCE SUBSCRIBED was printed here

        Util.sleep(1);

        flux
                .doFirst(()-> System.out.println("********************"))
                .as(StepVerifier::create)
                .expectNext(3,4,5)
                .verifyComplete(); // after the 1st and unless the duration expired, any sub will get only the last 3 cached values. SOURCE SUBSCRIBED was NOT printed

        Util.sleep(1);

        flux
                .doFirst(()-> System.out.println("********************"))
                .as(StepVerifier::create)
                .expectNext(1,2,3,4,5)
                .verifyComplete(); // the  duration expired so this sub will get all the values, SOURCE SUBSCRIBED was printed here


        /*
        ********************
        SOURCE SUBSCRIBED
        ********************
        ********************
        SOURCE SUBSCRIBED
         */
    }

    @Test
    void delaySubscription() {
        SimpleDateFormat sdf = new SimpleDateFormat("HH:mm:ss.SSS");

        System.out.println("STARTED AT: " + sdf.format(new Date()));
        Mono.just("HELLO")
                .doOnSubscribe(x -> System.out.println("SUBSCRIBED AT: " + sdf.format(new Date())))
                .delaySubscription(Duration.ofSeconds(2))
                .then()
                .as(StepVerifier::create)
                .verifyComplete();

        //STARTED AT: 07:26:02.642
        //SUBSCRIBED AT: 07:26:04.786
    }

    @Test
    void delaySubscriptionUntilPublisherPublish() {


        System.out.println("WAITING TO PUBLISH... :" + LocalTime.now());
        Mono<String> first = Mono.just("HELLO")
                .delayElement(Duration.ofSeconds(2));

        Mono.just("HI")
                .delaySubscription(first)
                .doOnNext(x -> System.out.println("GOT VALUE AT: " + LocalTime.now()))
                .then()
                .as(StepVerifier::create)
                .verifyComplete();

        //WAITING TO PUBLISH... :07:35:21.519492900
        //GOT VALUE AT: 07:35:23.713782300

    }

    @Test
    void doOn(){
        Flux<Integer> integerFlux = Flux.range(1, 20)
                .take(10)
                .filter(x -> x % 2 == 0)
                .doOnSubscribe(x -> System.out.println("SUBSCRIBING TO PUBLISHER"))
                .doFirst(() -> System.out.println("THE FIRST THING TO DO"))
                .doFinally(x -> System.out.println("FINALLY..."))
                .doOnNext(v -> System.out.println("EMITTING VALUE: " + v))
                .doOnDiscard(Integer.class, d -> System.out.println("DISCARDING VALUE: " + d))
                .doOnComplete(() -> System.out.println("COMPLETED"))
                .doOnCancel(() -> System.out.println("CANCELLED"))
                .doAfterTerminate(() -> System.out.println("ERROR OCCURRED OR COMPLETED"))
                .doOnRequest(n -> System.out.println("REQUESTING " + n + " ITEMS"));


        integerFlux
                .as(flux-> StepVerifier.create(flux,2))
                .expectNext(2,4)
                .thenRequest(2)
                .expectNext(6,8)
                .thenCancel()
                .verify();

        //THE FIRST THING TO DO
        //SUBSCRIBING TO PUBLISHER
        //REQUESTING 2 ITEMS
        //DISCARDING VALUE: 1
        //EMITTING VALUE: 2
        //DISCARDING VALUE: 3
        //EMITTING VALUE: 4
        //REQUESTING 2 ITEMS
        //DISCARDING VALUE: 5
        //EMITTING VALUE: 6
        //DISCARDING VALUE: 7
        //EMITTING VALUE: 8
        //CANCELLED
        //FINALLY...

        integerFlux
                .as(flux-> StepVerifier.create(flux,2))
                .expectNext(2,4)
                .thenRequest(2)
                .expectNext(6,8)
                .thenRequest(1)
                .expectNext(10)
                .verifyComplete();

        //THE FIRST THING TO DO
        //SUBSCRIBING TO PUBLISHER
        //REQUESTING 2 ITEMS
        //DISCARDING VALUE: 1
        //EMITTING VALUE: 2
        //DISCARDING VALUE: 3
        //EMITTING VALUE: 4
        //REQUESTING 2 ITEMS
        //DISCARDING VALUE: 5
        //EMITTING VALUE: 6
        //DISCARDING VALUE: 7
        //EMITTING VALUE: 8
        //REQUESTING 1 ITEMS
        //DISCARDING VALUE: 9
        //EMITTING VALUE: 10
        //COMPLETED
        //ERROR OCCURRED OR COMPLETED
        //FINALLY...

    }

    @Test
    void elementAt(){
        Flux.range(1, 20)
                .elementAt(0)
                .as(StepVerifier::create)
                .expectNext(1)
                .verifyComplete();
    }

    @Test
    void hasElement(){
        Flux.range(1, 20)
                .hasElement(33)
                .as(StepVerifier::create)
                .expectNext(false)
                .verifyComplete();
    }

    @Test
    void hasElements(){
        Flux.empty()
                .hasElements()
                .as(StepVerifier::create)
                .expectNext(false)
                .verifyComplete();
    }

    @Test
    void index(){
        Flux.range(1, 5)
                .index()
                .doOnNext(System.out::println)
                .then()
                .as(StepVerifier::create)
                .verifyComplete();

        //[0,1]
        //[1,2]
        //[2,3]
        //[3,4]
        //[4,5]
    }

    @Test
    void last(){
        Flux.just(1,2,3)
                .last()
                .as(StepVerifier::create)
                .expectNext(3)
                .verifyComplete();

        Flux.empty()
                .last("default-value")
                .as(StepVerifier::create)
                .expectNext("default-value")
                .verifyComplete();
    }



}
