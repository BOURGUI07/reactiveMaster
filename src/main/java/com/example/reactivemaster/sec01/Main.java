package com.example.reactivemaster.sec01;

import com.example.reactivemaster.common.Util;
import com.example.reactivemaster.sec01.publisher.PublisherImpl;
import com.example.reactivemaster.sec01.subscriber.SubscriberImpl;
import org.reactivestreams.Publisher;

public class Main {
    public static void main(String[] args) throws InterruptedException {
        demo2();
    }
    public static void demo1(){
        var publisher = new PublisherImpl();
        var subscriber = new SubscriberImpl();
        publisher.subscribe(subscriber);

    }

    public static void demo2() throws InterruptedException {
        var publisher = new PublisherImpl();
        var subscriber = new SubscriberImpl();
        publisher.subscribe(subscriber);
        subscriber.getSubscription().request(11);
    }

    public static void demo3() throws InterruptedException {
        var publisher = new PublisherImpl();
        var subscriber = new SubscriberImpl();
        publisher.subscribe(subscriber);
        subscriber.getSubscription().request(3);
        Util.sleep(2);
        subscriber.getSubscription().request(3);
        Util.sleep(2);
        subscriber.getSubscription().request(3);
        Util.sleep(2);
        subscriber.getSubscription().request(3);
        Util.sleep(2);
    }

    public static void demo4() throws InterruptedException {
        var publisher = new PublisherImpl();
        var subscriber = new SubscriberImpl();
        publisher.subscribe(subscriber);
        subscriber.getSubscription().request(3);
        Util.sleep(2);
        subscriber.getSubscription().request(3);
        subscriber.getSubscription().cancel();
        Util.sleep(2);
        subscriber.getSubscription().request(3);
        Util.sleep(2);
        subscriber.getSubscription().request(3);
        Util.sleep(2);
    }
}
