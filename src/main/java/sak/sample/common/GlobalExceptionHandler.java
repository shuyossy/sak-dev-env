package sak.sample.common;

import java.util.List;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import sak.sample.itinerary.exception.ActivityNotFoundException;
import sak.sample.itinerary.exception.TripNotFoundException;

/** 全 Controller 共通の例外ハンドラ。 */
@Slf4j
@ControllerAdvice
public class GlobalExceptionHandler {

  private static final String KEY_MESSAGE = "message";
  private static final String KEY_TRACE_ID = "traceId";

  @ExceptionHandler(MethodArgumentNotValidException.class)
  public ResponseEntity<Map<String, Object>> handleValidation(
      final MethodArgumentNotValidException ex) {
    log.info("Validation error: {}", ex.getMessage());
    List<Map<String, String>> errors =
        ex.getBindingResult().getFieldErrors().stream()
            .map(
                e ->
                    Map.of(
                        "field", e.getField(), KEY_MESSAGE, String.valueOf(e.getDefaultMessage())))
            .toList();
    return ResponseEntity.badRequest().body(Map.of("errors", errors));
  }

  @ExceptionHandler(TripNotFoundException.class)
  public ResponseEntity<Map<String, Object>> handleTripNotFound(final TripNotFoundException ex) {
    log.info(ex.getMessage());
    return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of(KEY_MESSAGE, ex.getMessage()));
  }

  @ExceptionHandler(ActivityNotFoundException.class)
  public ResponseEntity<Map<String, Object>> handleActivityNotFound(
      final ActivityNotFoundException ex) {
    log.info(ex.getMessage());
    return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of(KEY_MESSAGE, ex.getMessage()));
  }

  @ExceptionHandler(IllegalArgumentException.class)
  public ResponseEntity<Map<String, Object>> handleIllegalArgument(
      final IllegalArgumentException ex) {
    log.info(ex.getMessage());
    return ResponseEntity.badRequest().body(Map.of(KEY_MESSAGE, ex.getMessage()));
  }

  @ExceptionHandler(Exception.class)
  public ResponseEntity<Map<String, Object>> handleOthers(final Exception ex) {
    log.error("Internal error", ex);
    String traceId = MDC.get(KEY_TRACE_ID);
    return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
        .body(Map.of(KEY_MESSAGE, "internal error", KEY_TRACE_ID, traceId == null ? "" : traceId));
  }
}
