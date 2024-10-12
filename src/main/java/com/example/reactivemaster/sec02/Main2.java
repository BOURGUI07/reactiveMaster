package com.example.reactivemaster.sec02;

import com.example.reactivemaster.common.Util;
import reactor.core.publisher.Mono;

public class Main2 {
    public static void main(String[] args) {
       demo2();
    }

    private static Mono<String> getUserName(int userId){
        return switch (userId){
          case 1 -> Mono.just("John");
          case 2 -> Mono.empty();
          default -> Mono.error(new Exception("Invalid user id"));
        };
    }
    private static void demo1(){
        getUserName(3).subscribe(Util.subscriber());
    }
    private static void demo2(){
        getUserName(1)
        .subscribe(
                x-> System.out.println(x),
                error -> {}

        );
    }
}
