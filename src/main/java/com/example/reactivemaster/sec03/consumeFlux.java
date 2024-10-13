package com.example.reactivemaster.sec03;

import com.example.reactivemaster.common.Util;
import reactor.core.publisher.Flux;
import reactor.netty.http.client.HttpClient;

public class consumeFlux {
    public static void main(String[] args) {
        getNames().subscribe(Util.subscriber());
        Util.sleep(3);

    }

    public static  Flux<String> getNames(){
        return HttpClient.create()
                .get()
                .uri("localhost:7070/demo02/name/stream")
                .responseContent()
                .asString();
    }
}
