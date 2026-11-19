/**
 * Analytics module: statistics about the kitchen, fed by Core events through an ACL.
 */
@ApplicationModule(displayName = "Analytics", allowedDependencies = "core :: events")
package com.mishanthrope.queueboard.analytics;

import org.springframework.modulith.ApplicationModule;
