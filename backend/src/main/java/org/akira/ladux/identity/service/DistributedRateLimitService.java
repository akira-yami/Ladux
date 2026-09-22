package org.akira.ladux.identity.service;

public interface DistributedRateLimitService {

    void check(String scope, String subject, int capacity, long refillMinutes, String message);

    void checkOtpDestination(String channel, String destination);

    void checkOtpUser(String channel, Integer userId);
}
