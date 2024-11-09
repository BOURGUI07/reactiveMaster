package com.example.reactivemaster;

import com.example.reactivemaster.common.Util;
import com.example.reactivemaster.sec01.subscriber.SubscriberImpl;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.publisher.Sinks;
import reactor.test.StepVerifier;
import reactor.util.retry.Retry;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Predicate;
import java.util.function.UnaryOperator;

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
    void flatMapMany(){
        Mono.just(1)
                .flatMapMany(i->Flux.range(i,3))
                .as(StepVerifier::create)
                .expectNext(1,2,3)
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


}
