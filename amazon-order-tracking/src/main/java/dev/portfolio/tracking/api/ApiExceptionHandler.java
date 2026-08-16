package dev.portfolio.tracking.api;

import dev.portfolio.tracking.api.ApiModels.ErrorResponse;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.http.*;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.*;
import java.time.Instant;

@RestControllerAdvice
public class ApiExceptionHandler {
  @ExceptionHandler(EntityNotFoundException.class) ResponseEntity<ErrorResponse> notFound(Exception e){return response(HttpStatus.NOT_FOUND,"NOT_FOUND",e.getMessage());}
  @ExceptionHandler(SecurityException.class) ResponseEntity<ErrorResponse> forbidden(Exception e){return response(HttpStatus.FORBIDDEN,"FORBIDDEN",e.getMessage());}
  @ExceptionHandler(MethodArgumentNotValidException.class) ResponseEntity<ErrorResponse> invalid(MethodArgumentNotValidException e){return response(HttpStatus.BAD_REQUEST,"INVALID_EVENT",e.getBindingResult().getFieldErrors().stream().findFirst().map(x->x.getField()+" "+x.getDefaultMessage()).orElse("Invalid request"));}
  private ResponseEntity<ErrorResponse> response(HttpStatus s,String code,String message){return ResponseEntity.status(s).body(new ErrorResponse(code,message,Instant.now()));}
}
