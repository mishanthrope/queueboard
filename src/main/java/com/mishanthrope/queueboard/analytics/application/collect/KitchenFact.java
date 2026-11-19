package com.mishanthrope.queueboard.analytics.application.collect;

/** What the ACL tells Analytics: a fact in Analytics' own terms. */
public sealed interface KitchenFact permits OrderReceived, ActionPerformed {
}
