package com.mt.aggregator.provider;

import com.mt.aggregator.exception.UpstreamInterruptedException;
import com.mt.aggregator.exception.UpstreamUnavailableException;
import java.util.concurrent.ThreadLocalRandom;

public abstract class BaseDataProvider {

    /*
     * Simulates latency of an external upstream service.
     *
     * Most requests use a normal latency range around the typical latency.
     * With a probability defined by slowRequestProbability, the request enters
     * the slow-request flow and its latency is deliberately generated above the
     * configured timeout. This simulates an upstream request that exceeds
     * the allowed response time.
     *
     * slowRequestProbability is a probability, not a latency threshold.
     * For example, with slowRequestProbability = 0.01, nextDouble() returns
     * a value between 0.0 (inclusive) and 1.0 (exclusive):
     *
     * random = 0.005 -> 0.005 < 0.01 -> slow-request flow
     * random = 0.009 -> 0.009 < 0.01 -> slow-request flow
     * random = 0.015 -> 0.015 < 0.01 -> normal-request flow
     * random = 0.500 -> 0.500 < 0.01 -> normal-request flow
     *
     * This means approximately 1% of requests enter the slow-request flow
     * and approximately 99% use the normal latency range.
     *
     * The constants below are simulation assumptions, not values defined
     * by the service specification:
     *
     * 0.8 -> lower bound for normal latency (80% of typical)
     * 1.2 -> upper bound for normal latency (120% of typical)
     * 2 -> upper bound for slow-request latency (2x timeout)
     *
     * The resulting model provides realistic latency variation around the
     * typical value while occasionally simulating requests that exceed
     * the configured timeout.
     */
    protected void jitter(
            int typicalLatencyMs,
            int timeoutMs,
            double slowRequestProbability) {

        int latency;

        if (ThreadLocalRandom.current().nextDouble() < slowRequestProbability) {
            latency = ThreadLocalRandom.current().nextInt(
                    timeoutMs + 1,
                    timeoutMs * 2);
        } else {
            latency = ThreadLocalRandom.current().nextInt(
                    (int) (typicalLatencyMs * 0.8),
                    (int) (typicalLatencyMs * 1.2) + 1);
        }

        try {
            if (latency > timeoutMs) {
                Thread.sleep(timeoutMs);

                throw new UpstreamUnavailableException(
                        "Upstream request timed out after "
                                + timeoutMs + " ms");
            }

            Thread.sleep(latency);

        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();

            throw new UpstreamInterruptedException(
                    "Upstream request interrupted",
                    ex);
        }
    }

    protected void reliability(double failureRate, String message) {
        if (ThreadLocalRandom.current().nextDouble() < failureRate) {
            throw new UpstreamUnavailableException(message);
        }
    }
}
