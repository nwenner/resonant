package com.wenroe.resonant.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * DEBUG ONLY: Test endpoint to verify async execution is working. DELETE THIS FILE after confirming
 * async works.
 */
@RestController
@RequestMapping("/api/debug")
@RequiredArgsConstructor
@Slf4j
public class AsyncDebugController {

  private final AsyncTestService asyncTestService;

  @GetMapping("/test-async")
  public ResponseEntity<String> testAsync() {
    log.info("=== CONTROLLER: Calling async method, thread: {}",
        Thread.currentThread().getName());

    asyncTestService.asyncMethod();

    log.info("=== CONTROLLER: Returned from async method call (should be immediate)");

    return ResponseEntity.ok("Check logs. If async works, you'll see different thread names.");
  }
}

@Service
@Slf4j
class AsyncTestService {

  @Async("scanExecutor")
  public void asyncMethod() {
    String threadName = Thread.currentThread().getName();
    log.info("=== ASYNC SERVICE: Method executing, thread: {}", threadName);

    if (threadName.startsWith("scan-")) {
      log.info("=== SUCCESS! Async is working correctly.");
    } else {
      log.error("=== FAILURE! Async not working. Thread should start with 'scan-' but got: {}",
          threadName);
    }

    try {
      Thread.sleep(2000); // Simulate work
      log.info("=== ASYNC SERVICE: Completed after 2 seconds");
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
    }
  }
}