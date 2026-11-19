package com.erp.ai.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

public record AgentRequest(@Min(2000) @Max(2100) int fiscalYear) {}
