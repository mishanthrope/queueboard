package com.mishanthrope.queueboard.analytics.domain;

/** What happened to an order in the kitchen, in Analytics' own words. */
public enum KitchenAction {
    RECEIVED,
    STARTED,
    COMPLETED,
    HANDED_OUT,
    CANCELLED,
    REOPENED
}
