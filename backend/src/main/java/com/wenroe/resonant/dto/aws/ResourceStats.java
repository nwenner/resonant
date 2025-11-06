package com.wenroe.resonant.dto.aws;

import lombok.Data;

import java.util.Map;

@Data
public class ResourceStats {
    private long total;
    private Map<String, Long> byType;
}