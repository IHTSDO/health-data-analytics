package org.snomed.heathanalytics.server.rest;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.snomed.heathanalytics.server.service.NotFoundException;
import org.springframework.http.HttpStatus;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.ResponseStatus;

import java.util.HashMap;
import java.util.Map;

@ControllerAdvice
public class RestControllerAdvice {

	private static final Logger logger = LoggerFactory.getLogger(RestControllerAdvice.class);

	@ExceptionHandler({
			IllegalArgumentException.class,
			IllegalStateException.class,
			HttpRequestMethodNotSupportedException.class,
			HttpMediaTypeNotSupportedException.class
	})
	@ResponseStatus(HttpStatus.BAD_REQUEST)
	@ResponseBody
	public Map<String,Object> handleIllegalArgumentException(Exception exception) {
		HashMap<String, Object> result = new HashMap<>();
		result.put("error", HttpStatus.BAD_REQUEST);
		result.put("message", exception.getMessage());
		logger.info("bad request {}", exception.getMessage(), exception);
		return result;
	}

	@ExceptionHandler({NotFoundException.class})
	@ResponseStatus(HttpStatus.NOT_FOUND)
	@ResponseBody
	public Map<String,Object> handleNotFoundException(Exception exception) {
		HashMap<String, Object> result = new HashMap<>();
		result.put("error", HttpStatus.NOT_FOUND);
		result.put("message", exception.getMessage());
		return result;
	}

	@ExceptionHandler(Exception.class)
	@ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
	@ResponseBody
	public Map<String,Object> handleException(Exception exception) {
		logger.error("Unhandled exception.", exception);
		HashMap<String, Object> result = new HashMap<>();
		result.put("error", HttpStatus.INTERNAL_SERVER_ERROR);
		result.put("message", exception.getMessage());
		return result;
	}

}
