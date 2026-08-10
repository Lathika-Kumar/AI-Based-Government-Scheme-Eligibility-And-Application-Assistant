package com.schemebridge.notificationservice.strategy;

import com.schemebridge.notificationservice.enums.Channel;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Factory class resolving the correct NotificationProviderStrategy implementation
 * based on requested Channel.
 */
@Component
@RequiredArgsConstructor
public class NotificationProviderStrategyFactory {

    private final List<NotificationProviderStrategy> strategies;

    public NotificationProviderStrategy getStrategy(Channel channel) {
        return strategies.stream()
                .filter(strategy -> strategy.supports(channel))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("No provider strategy registered for channel: " + channel));
    }
}
