package com.motherhood.journey;

import org.junit.jupiter.api.extension.ConditionEvaluationResult;
import org.junit.jupiter.api.extension.ExecutionCondition;
import org.junit.jupiter.api.extension.ExtensionContext;

import java.net.UnixDomainSocketAddress;
import java.nio.channels.SocketChannel;

/**
 * Skips Testcontainers tests when Docker is not reachable, so the build
 * stays green in environments without Docker access (CI agents, restricted laptops).
 * Evaluated before @Testcontainers starts containers, so failures become SKIPPED not ERRORED.
 */
public class DockerAvailableCondition implements ExecutionCondition {

    private static final ConditionEvaluationResult ENABLED =
        ConditionEvaluationResult.enabled("Docker socket is reachable");
    private static final ConditionEvaluationResult DISABLED =
        ConditionEvaluationResult.disabled(
            "Docker socket not reachable — container-backed test skipped");

    @Override
    public ConditionEvaluationResult evaluateExecutionCondition(ExtensionContext context) {
        if (canConnect("/var/run/docker.sock")) {
            return ENABLED;
        }
        String home = System.getProperty("user.home", "");
        if (canConnect(home + "/.docker/desktop/docker.sock")) {
            return ENABLED;
        }
        return DISABLED;
    }

    private static boolean canConnect(String socketPath) {
        try {
            UnixDomainSocketAddress addr = UnixDomainSocketAddress.of(socketPath);
            try (SocketChannel ch = SocketChannel.open(addr)) {
                return ch.isConnected();
            }
        } catch (Exception e) {
            return false;
        }
    }
}
