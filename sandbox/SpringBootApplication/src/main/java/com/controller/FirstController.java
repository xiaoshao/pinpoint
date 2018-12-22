package com.controller;

import com.model.User;
import com.service.FirstService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.client.AsyncClientHttpRequest;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.stereotype.Controller;
import org.springframework.util.concurrent.ListenableFuture;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.client.AsyncRequestCallback;
import org.springframework.web.client.AsyncRestTemplate;
import org.springframework.web.client.ResponseExtractor;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;


@Controller
public class FirstController {

    @Autowired
    FirstService firstService;


    @ResponseBody
    @GetMapping("/index/{name1}{age}")
    public String index(@PathVariable("name1") int age, @PathVariable String name){
        return "hello world" + age;
    }

//    @ResponseBody
//    @GetMapping("/index/{name}")
//    public String index1(@PathVariable("name") String age){
//        return "hello world" + age;
//    }


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

    @GetMapping("/async/1")
    @ResponseBody
    public String async1(){
        AsyncRestTemplate asyncRestTemplate = new AsyncRestTemplate();
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.TEXT_PLAIN);
        AsyncRequestCallback asyncRequestCallback = new AsyncRequestCallback() {

            @Override
            public void doWithRequest(AsyncClientHttpRequest request) throws IOException {
                System.out.println(request.getURI());
            }
        };

        ResponseExtractor<String> responseExtractor = new ResponseExtractor<String>() {
            @Override
            public String extractData(ClientHttpResponse response) throws IOException {
                BufferedReader reader = new BufferedReader(new InputStreamReader(response.getBody()));
                String line = null;
                StringBuilder result = new StringBuilder();
                while((line = reader.readLine()) != null){
                    result.append(line);
                }
                return result.toString();
            }
        };

        Map<String,String> urlVariable = new HashMap<String, String>();
//        urlVariable.put("q", "Concretepage");

        ListenableFuture<String> future = asyncRestTemplate.execute("http://localhost:8080/index", HttpMethod.GET,
                asyncRequestCallback, responseExtractor, urlVariable);

        try {
            //waits for the result
            return future.get();
        } catch (InterruptedException e) {
            e.printStackTrace();
        } catch (ExecutionException e) {
            e.printStackTrace();
        }

        return "ex";
    }

    @GetMapping("/async")
    @ResponseBody
    public User test1() {
        firstService.findUser("xiaoshao");

        return new User();
    }
}
