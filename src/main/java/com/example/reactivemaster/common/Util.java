package com.example.reactivemaster.common;

import com.github.javafaker.Faker;
import org.reactivestreams.Subscriber;

import java.time.Duration;

public class Util {
    private static final Faker faker = Faker.instance();
    public static Faker faker(){
        return faker;
    }

    public static void sleep(int secs)  {
        try {
            Thread.sleep(Duration.ofSeconds(secs));
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    public static <T> Subscriber<T> subscriber(String name){
        return new DefaultSub<>(name);
    }

    public static <T> Subscriber<T> subscriber(){
        return new DefaultSub<>("");
    }

}
