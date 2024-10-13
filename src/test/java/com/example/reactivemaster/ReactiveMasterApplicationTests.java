package com.example.reactivemaster;

import com.example.reactivemaster.common.Util;
import com.example.reactivemaster.sec01.subscriber.SubscriberImpl;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Predicate;

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

    @Test
    void just(){

        var mono = Mono.just(sum(List.of(1,2,3)));

        assert publisherExecuted.get();

        mono.as(StepVerifier::create)
                .expectNext(6)
                .verifyComplete();

        assert publisherExecuted.get();


    }

    @Test
    void from_Supplier(){

        var mono = Mono.fromSupplier(() ->sum(List.of(1,2,3)));

        assert !publisherExecuted.get();

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
            when he does, it create the data with the amount the sub wants.
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
        Flux.just(1,2)
                .concatWith(Flux.error(new IllegalArgumentException("Error")))
                .onErrorReturn(IllegalArgumentException.class,0)
                .as(StepVerifier::create)
                .expectNext(1,2,0)
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
                .buffer(2)
                .as(StepVerifier::create)
                .expectNext(List.of(1,2),List.of(3,4),List.of(5,6))
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
    void distinct(){
        Flux.just(1,22,24,22)
                .distinct()
                .as(StepVerifier::create)
                .expectNext(1,22,24)
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
                .collectMap(v->v,String::length)
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
    void startWith(){
        Flux.range(1,2)
                .startWith(44)
                .as(StepVerifier::create)
                .expectNext(44,1,2)
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

}
