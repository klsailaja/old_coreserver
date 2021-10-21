package com.ab.core;

import java.util.Calendar;
import java.util.concurrent.TimeUnit;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import com.ab.core.common.LazyScheduler;
import com.ab.core.tasks.DeleteOldRecords;

@SpringBootApplication
public class CoreQuizServerApplication implements ApplicationRunner {

	private static final Logger logger = LogManager.getLogger(CoreQuizServerApplication.class);
	
	public static void main(String[] args) {
		SpringApplication.run(CoreQuizServerApplication.class, args);
	}
	
	@Override
	public void run(ApplicationArguments args) {
		
		Calendar calendar = Calendar.getInstance();
		calendar.add(Calendar.DATE, 1);
		calendar.set(Calendar.HOUR, 3);
		calendar.set(Calendar.MINUTE, 0);
		calendar.set(Calendar.SECOND, 0);
		calendar.set(Calendar.AM_PM, Calendar.AM);
		
		long initialDelay = calendar.getTimeInMillis() - System.currentTimeMillis();
		//initialDelay = 0;
		
		LazyScheduler.getInstance().submitRepeatedTask(new DeleteOldRecords(), initialDelay, 
				24 * 60 * 1000, TimeUnit.MILLISECONDS);

	}
}
