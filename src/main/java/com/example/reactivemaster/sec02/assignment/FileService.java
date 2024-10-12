package com.example.reactivemaster.sec02.assignment;

import reactor.core.publisher.Mono;

public interface FileService {
    Mono<String> readFile(String filename);
    Mono<Void> writeFile(String filename, String content);
    Mono<Void> deleteFile(String filename);
}
