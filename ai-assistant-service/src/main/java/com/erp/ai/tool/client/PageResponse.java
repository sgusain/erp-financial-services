package com.erp.ai.tool.client;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/** The part of Spring Data's Page JSON that we need. */
@JsonIgnoreProperties(ignoreUnknown = true)
public record PageResponse<T>(List<T> content, long totalElements) {}
