package com.shivam.intelliflow.logaggregator.dlq;

import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/dlq")
public class DlqController {
    private final DlqMessageRepository repository;
    private final DlqReplayService replayService;

    public DlqController(DlqMessageRepository repository, DlqReplayService replayService) {
        this.repository = repository;
        this.replayService = replayService;
    }

    @GetMapping("/messages")
    public List<DlqMessageResponse> list(
            @RequestParam(value = "status", required = false) DlqMessageStatus status,
            @RequestParam(value = "limit", defaultValue = "50") int limit
    ) {
        int cappedLimit = Math.min(Math.max(limit, 1), 500);
        List<DlqMessage> messages = status == null
                ? repository.findRecent(cappedLimit)
                : repository.findByStatus(status, cappedLimit);
        return messages.stream().map(DlqMessageResponse::from).toList();
    }

    @GetMapping("/messages/{id}")
    public DlqMessageResponse get(@PathVariable UUID id) {
        return repository.findById(id)
                .map(DlqMessageResponse::from)
                .orElseThrow(() -> new DlqMessageNotFoundException(id));
    }

    @PostMapping("/messages/{id}/replay")
    public DlqReplayResult replay(@PathVariable UUID id) {
        return replayService.replay(id);
    }

    @ExceptionHandler(DlqMessageNotFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public ErrorResponse handleNotFound(DlqMessageNotFoundException exception) {
        return new ErrorResponse(exception.getMessage());
    }

    @ExceptionHandler(DlqReplayException.class)
    public ResponseEntity<ErrorResponse> handleReplayFailure(DlqReplayException exception) {
        return ResponseEntity.status(HttpStatus.CONFLICT).body(new ErrorResponse(exception.getMessage()));
    }

    public record ErrorResponse(String message) {
    }
}
