package com.erp.ai.dto;

import java.util.List;

/** The answer plus the policy files it came from, so users can check it. */
public record PolicyAnswer(String answer, List<String> sources) {}
