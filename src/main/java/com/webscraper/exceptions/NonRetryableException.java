package com.webscraper.exceptions;

public class NonRetryableException extends Exception {
  public NonRetryableException(String message, Throwable cause) {
    super(message, cause);
  }
}