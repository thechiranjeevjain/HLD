package dev.interview.orders.web;
import org.springframework.http.*; import org.springframework.web.bind.MethodArgumentNotValidException; import org.springframework.web.bind.annotation.*; import java.time.Instant; import java.util.*;
@RestControllerAdvice public class ApiExceptionHandler {record Problem(Instant timestamp,int status,String error,String detail){}
 @ExceptionHandler(NoSuchElementException.class) ResponseEntity<Problem> notFound(Exception e){return problem(HttpStatus.NOT_FOUND,e);}
 @ExceptionHandler({IllegalArgumentException.class,MethodArgumentNotValidException.class}) ResponseEntity<Problem> bad(Exception e){return problem(HttpStatus.BAD_REQUEST,e);}
 @ExceptionHandler(SecurityException.class) ResponseEntity<Problem> forbidden(Exception e){return problem(HttpStatus.FORBIDDEN,e);}
 private ResponseEntity<Problem> problem(HttpStatus s,Exception e){return ResponseEntity.status(s).body(new Problem(Instant.now(),s.value(),s.getReasonPhrase(),e.getMessage()));}}
