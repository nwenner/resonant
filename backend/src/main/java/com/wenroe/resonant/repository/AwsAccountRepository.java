package com.wenroe.resonant.repository;

import com.wenroe.resonant.model.entity.AwsAccount;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface AwsAccountRepository extends JpaRepository<AwsAccount, UUID> {
    List<AwsAccount> findByUserId(UUID userId);
    Optional<AwsAccount> findByUserIdAndAccountId(UUID userId, String accountId);
    List<AwsAccount> findByStatus(AwsAccount.Status status);
}
