package com.example.reactivemaster.common;

import lombok.extern.slf4j.Slf4j;
import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;

@Slf4j
public class DefaultSub<T> implements Subscriber<T> {
    private final String name;

    public DefaultSub(String name) {
        this.name = name.isBlank()?"ANONYMOUS":name;
    }

    @Override
    public void onSubscribe(Subscription subscription) {
        subscription.request(Long.MAX_VALUE);
    }

    @Override
    public void onNext(T item) {
        log.info("{} RECEIVED ITEM: {}", name, item);
    }

    @Override
    public void onError(Throwable throwable) {
        log.info("{} CAUSED ERROR: {}", name, throwable.getMessage());
    }

    @Override
    public void onComplete() {
        log.info("{} RECEIVED COMPLETED",name);
    }
}
