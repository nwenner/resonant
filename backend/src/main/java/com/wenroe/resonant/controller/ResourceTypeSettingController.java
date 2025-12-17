package com.wenroe.resonant.controller;

import com.wenroe.resonant.dto.resource.ResourceTypeSettingResponse;
import com.wenroe.resonant.dto.resource.UpdateResourceTypeSettingRequest;
import com.wenroe.resonant.model.entity.ResourceTypeSetting;
import com.wenroe.resonant.service.ResourceTypeSettingService;
import jakarta.validation.Valid;
import java.util.List;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/resource-type-settings")
@RequiredArgsConstructor
@Slf4j
public class ResourceTypeSettingController {

  private final ResourceTypeSettingService service;

  @GetMapping
  public ResponseEntity<List<ResourceTypeSettingResponse>> getAll() {
    log.info("Fetching all resource type settings");
    List<ResourceTypeSetting> settings = service.getAll();
    return ResponseEntity.ok(settings.stream()
        .map(ResourceTypeSettingResponse::fromEntity)
        .collect(Collectors.toList()));
  }

  @PutMapping("/{resourceType}/enabled")
  public ResponseEntity<ResourceTypeSettingResponse> updateEnabled(
      @PathVariable String resourceType,
      @Valid @RequestBody UpdateResourceTypeSettingRequest request) {

    log.info("Updating resource type {} enabled status to {}", resourceType, request.getEnabled());
    ResourceTypeSetting updated = service.updateEnabled(resourceType, request.getEnabled());
    return ResponseEntity.ok(ResourceTypeSettingResponse.fromEntity(updated));
  }
}