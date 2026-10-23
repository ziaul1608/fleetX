package com.fleetx.auth.repository;

import com.fleetx.auth.entity.OtpChallenge;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.Optional;

@Repository
public interface OtpChallengeRepository extends JpaRepository<OtpChallenge, Long> {

    Optional<OtpChallenge> findTopByEmailAndPurposeOrderByIdDesc(String email, String purpose);

    long countByEmailAndCreatedAtAfter(String email, Instant after);

    long countByIpAddressAndCreatedAtAfter(String ipAddress, Instant after);
}
