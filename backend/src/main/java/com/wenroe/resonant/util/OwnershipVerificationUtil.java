package com.wenroe.resonant.util;

import com.wenroe.resonant.model.entity.AwsAccount;
import com.wenroe.resonant.model.entity.AwsResource;
import com.wenroe.resonant.model.entity.User;

import java.util.UUID;

public class OwnershipVerificationUtil {
    public static boolean unverifiedOwnership(User user, AwsAccount account) {
        return !account.getUser().getId().equals(user.getId());
    }

    public static boolean unverifiedOwnership(User user, AwsResource resource) {
        return !resource.getAwsAccount().getUser().getId().equals(user.getId());
    }

    public static boolean unverifiedOwnershipById(UUID ownerId, User user) {
        return !ownerId.equals(user.getId());
    }
}
