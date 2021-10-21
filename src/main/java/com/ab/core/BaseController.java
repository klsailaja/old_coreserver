package com.ab.core;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

import com.ab.core.exceptions.InternalException;
import com.ab.core.exceptions.NotAllowedException;

@ControllerAdvice
public class BaseController {
	
	@ExceptionHandler(value = NotAllowedException.class)
	public ResponseEntity<Object> exception(NotAllowedException exception) {
		return new ResponseEntity<>(exception.getMessage(), HttpStatus.NOT_ACCEPTABLE);
	}
	
	@ExceptionHandler(value = InternalException.class)
	public ResponseEntity<Object> exception1(InternalException exception) {
		return new ResponseEntity<>("Internal Server Error", HttpStatus.INTERNAL_SERVER_ERROR);
	}
}
