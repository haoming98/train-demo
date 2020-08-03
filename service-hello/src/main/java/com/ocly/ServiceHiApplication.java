package com.ocly;

import com.netflix.hystrix.contrib.javanica.annotation.HystrixCommand;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.netflix.eureka.EnableEurekaClient;
import org.springframework.cloud.netflix.hystrix.EnableHystrix;
import org.springframework.cloud.netflix.hystrix.dashboard.EnableHystrixDashboard;
import org.springframework.context.annotation.Bean;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestTemplate;

@RestController
@EnableEurekaClient
@SpringBootApplication
@EnableHystrix
@EnableHystrixDashboard
public class ServiceHiApplication {

	public static void main(String[] args) {
		SpringApplication.run(ServiceHiApplication.class, args);
	}

	@Value("${server.port}")
	String port;
	@Value("${message.name}")
  String nickname;
	@RequestMapping("/")
	public String home() {
		return "hi "+nickname+",i am from port:" +port;
	}

	//@Bean
	//public RestTemplate getRestTemplate(){
	//	return new RestTemplate();
	//}

	//@GetMapping("/hello")
	//@HystrixCommand(fallbackMethod = "hiError")
	//public String homes(String name) {
	//	return "hi "+name+",i am from port:" +port;
	//}

	public String hiError(String name) {
		return "hi,"+name+",sorry,error!";
	}

}
