package com.example.reactivemaster.common;

import com.github.javafaker.Faker;

import java.time.Duration;

public class Util {
    private static final Faker faker = Faker.instance();
    public static Faker faker(){
        return faker;
    }

    public static void sleep(int secs) throws InterruptedException {
        Thread.sleep(Duration.ofSeconds(secs));
    }
}
