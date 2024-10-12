package com.example.reactivemaster.sec02;

import com.example.reactivemaster.common.Util;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Mono;
import reactor.netty.http.client.HttpClient;

@Slf4j
public class externalService {
    public static void main(String[] args) {
        demo1();
    }

    private static Mono<String> getProductName(){
        return HttpClient.create()
                .get()
                .uri("localhost:7070/demo/01/product/2")
                .responseContent()
                .asString()
                .next();
    }

    private static  void demo1(){
        getProductName().subscribe(Util.subscriber());
        Util.sleep(2);
    }
}
