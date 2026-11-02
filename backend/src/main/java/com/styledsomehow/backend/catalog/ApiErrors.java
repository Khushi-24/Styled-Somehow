package com.styledsomehow.backend.catalog;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.http.*;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.dao.DataIntegrityViolationException;
import java.util.Map;
@RestControllerAdvice
public class ApiErrors {
 @ExceptionHandler(org.springframework.web.server.ResponseStatusException.class)
 public ResponseEntity<Map<String,String>> status(org.springframework.web.server.ResponseStatusException e){
  String message=e.getReason();
  if(message==null||message.isBlank())message="The request could not be completed. Please try again.";
  return ResponseEntity.status(e.getStatusCode()).headers(e.getHeaders()).body(Map.of("message",message));
 }

 @ExceptionHandler(org.springframework.web.multipart.MaxUploadSizeExceededException.class)
 public ResponseEntity<Map<String,String>> oversizedUpload(){return ResponseEntity.status(HttpStatus.PAYLOAD_TOO_LARGE).body(Map.of("message","Choose a photo under 8 MB. Large photos can be resized in the product editor before uploading."));}

 @ExceptionHandler(MethodArgumentNotValidException.class) public ResponseEntity<Map<String,String>> invalid(MethodArgumentNotValidException e){
  String message=e.getBindingResult().getAllErrors().stream().map(error->error.getDefaultMessage()).distinct().sorted().collect(java.util.stream.Collectors.joining("; "));
  return ResponseEntity.badRequest().body(Map.of("message",message));
 }
 @ExceptionHandler({ObjectOptimisticLockingFailureException.class,DataIntegrityViolationException.class}) public ResponseEntity<Map<String,String>> conflict(Exception e){return ResponseEntity.status(HttpStatus.CONFLICT).body(Map.of("message","Product changed or this URL already exists. Reload the catalogue before saving."));}
}
