package com.wenroe.resonant.dto.aws;

import lombok.Data;

/**
 * Request DTO for updating alias.
 */
@Data
public class UpdateAliasRequest {
    private String accountAlias;
}