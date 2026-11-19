package com.mishanthrope.queueboard.analytics.internal;

import java.time.Duration;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/** Mutable on purpose: tests switch the simulated slowness and failure without new contexts. */
@Component
public class SimulationSettings {

    private volatile Duration simulatedDelay;
    private volatile boolean fail;

    public SimulationSettings(
            @Value("${app.analytics.simulated-delay:PT0S}") Duration simulatedDelay,
            @Value("${app.analytics.fail:false}") boolean fail) {
        this.simulatedDelay = simulatedDelay;
        this.fail = fail;
    }

    public Duration getSimulatedDelay() {
        return simulatedDelay;
    }

    public void setSimulatedDelay(Duration simulatedDelay) {
        this.simulatedDelay = simulatedDelay;
    }

    public boolean isFail() {
        return fail;
    }

    public void setFail(boolean fail) {
        this.fail = fail;
    }

    /** Sleeps for the simulated delay, then throws if failure is switched on. */
    void apply() {
        var delay = simulatedDelay;
        if (!delay.isZero()) {
            try {
                Thread.sleep(delay.toMillis());
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
        if (fail) {
            throw new IllegalStateException("Simulated analytics failure");
        }
    }
}
