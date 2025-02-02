package com.absolute.chessplatform.webscraper.exceptions;

import lombok.extern.slf4j.Slf4j;
import org.apache.coyote.BadRequestException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

import java.net.URISyntaxException;

@Slf4j
@ControllerAdvice
public class GlobalExceptionHandler extends ResponseEntityExceptionHandler {
    @ExceptionHandler(URISyntaxException.class)
    protected void handleBadRequest(URISyntaxException ex) {
        log.error(ex.getMessage(), ex);

    }
}
