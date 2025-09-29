package com.gjjfintech.aiprompts.controller;

import com.gjjfintech.aiprompts.dto.ChatAndSaveRequest;
import com.gjjfintech.aiprompts.dto.ChatAndSaveResponse;
import com.gjjfintech.aiprompts.service.ChatAndSaveService;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

@RestController
@CrossOrigin(origins = {"http://localhost:5173"})
public class ChatAndSaveController {

    private final ChatAndSaveService service;

    public ChatAndSaveController(ChatAndSaveService service) {
        this.service = service;
    }

    @PostMapping(path = "/chat-and-save", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public Mono<ResponseEntity<ChatAndSaveResponse>> chatAndSave(@RequestBody ChatAndSaveRequest req) {
        return service.process(req)
                .map(ResponseEntity::ok)
                .onErrorResume(ex -> Mono.just(ResponseEntity.internalServerError()
                        .body(ChatAndSaveResponse.failed("Error: " + ex.getMessage()))));
    }
}
