package com.controller;

import com.model.User;
import com.service.FirstService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;


@Controller
public class FirstController {

    @Autowired
    FirstService firstService;

    @ResponseBody
    @GetMapping("/index")
    public String index(){
        return "hello world";
    }

    @GetMapping("/hello")
    @ResponseBody
    Callable<String> test() {
        Callable<String> callable = new Callable<String>() {

            public String call() throws Exception {
                Thread.sleep(2000);
                return "first";
            }
        };
        return callable;

    }

    @GetMapping("/async")
    @ResponseBody
    public User test1() {
        firstService.findUser("xiaoshao");

        return new User();
    }
}
