package com.example.reactivemaster.sec02.assignment;

import reactor.core.publisher.Mono;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;

public class FileServiceImpl implements FileService {
    @Override
    public Mono<String> readFile(String filename) {
        String x = "";
        try(BufferedReader br = new BufferedReader(new FileReader(filename))) {
            String line;
            while((line = br.readLine()) != null) {
                x+=line+"\n";
            }
        }catch (IOException e) {
            return Mono.error(e);
        }
        String finalX = x;
        return Mono.fromSupplier(() -> finalX);
    }

    @Override
    public Mono<Void> writeFile(String filename, String content) {
        return null;
    }

    @Override
    public Mono<Void> deleteFile(String filename) {
        return null;
    }
}
