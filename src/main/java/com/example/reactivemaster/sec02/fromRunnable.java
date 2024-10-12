package com.example.reactivemaster.sec02;

import com.example.reactivemaster.common.Util;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Mono;

@Slf4j
public class fromRunnable {
    public static void main(String [] args){
        getProductName(4)
                .subscribe(Util.subscriber());
    }

    private static Mono<String> getProductName(int productId){
        return productId==1
                ? Mono.just(Util.faker().commerce().productName())
                : Mono.fromRunnable(() -> notify(productId));
    }

    private static void notify(int productId){
        log.info("notify product {}",productId);
    }
}
