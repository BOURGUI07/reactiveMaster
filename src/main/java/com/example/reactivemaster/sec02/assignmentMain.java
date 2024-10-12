package com.example.reactivemaster.sec02;

import com.example.reactivemaster.common.Util;
import com.example.reactivemaster.sec02.assignment.FileServiceImpl;

public class assignmentMain {
    public static void main(String[] args) {
        var fileName = "C:\\Users\\hp\\Documents\\projects\\reactiveMaster\\src\\main\\java\\com\\example\\reactivemaster\\sec02\\assignment\\example.txt";
        FileServiceImpl fileService = new FileServiceImpl();
        fileService.readFile(fileName).subscribe(Util.subscriber());
    }
}
