package com.example.reactivemaster.sec01.publisher;

import com.example.reactivemaster.common.Util;
import lombok.extern.slf4j.Slf4j;
import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;

@Slf4j
public class SubscriptionImpl implements Subscription {
    private Subscriber<? super String> subscriber;
    private int count;
    private static final long MAX_ITEMS = 10;
    private boolean isCancelled;

    public SubscriptionImpl(Subscriber<? super String> subscriber) {
        this.subscriber = subscriber;
    }

    @Override
    public void request(long requests) {
        log.info("SUBSCRIBER REQUESTED {} ITEMS", requests);
        if(isCancelled) return;
        if(requests>MAX_ITEMS) {
            subscriber.onError(new IllegalArgumentException("MAX ITEMS REACHED"));
            isCancelled = true;
            return;
        }
        for(int i=0; i<requests && count<MAX_ITEMS; i++) {
            subscriber.onNext(Util.faker().internet().emailAddress());
            count++;
        }
        if(count==MAX_ITEMS) {
            log.info("NO MORE DATA TO PRODUCE");
            subscriber.onComplete();
            isCancelled = true;
        }
    }

    @Override
    public void cancel() {
        log.info("SUBSCRIBER HAS CANCELLED");
        isCancelled = true;
    }
}
