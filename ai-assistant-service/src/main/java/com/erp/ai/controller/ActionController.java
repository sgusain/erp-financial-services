package com.erp.ai.controller;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.erp.ai.dto.PendingActionView;
import com.erp.ai.security.CurrentUser;
import com.erp.ai.service.PendingActionService;

/** Where a human approves or rejects what the assistant proposed. */
@RestController
@RequestMapping("/api/ai/actions")
public class ActionController {

    private final PendingActionService pendingActions;

    public ActionController(PendingActionService pendingActions) {
        this.pendingActions = pendingActions;
    }

    @GetMapping
    public ResponseEntity<Page<PendingActionView>> list(Pageable pageable) {
        return ResponseEntity.ok(pendingActions.listPending(CurrentUser.username(), pageable));
    }

    @PostMapping("/{id}/confirm")
    public ResponseEntity<PendingActionView> confirm(@PathVariable String id) {
        return ResponseEntity.ok(pendingActions.confirm(CurrentUser.username(), id, CurrentUser.bearerToken()));
    }

    @PostMapping("/{id}/reject")
    public ResponseEntity<PendingActionView> reject(@PathVariable String id) {
        return ResponseEntity.ok(pendingActions.reject(CurrentUser.username(), id));
    }
}
