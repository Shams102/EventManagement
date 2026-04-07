package com.campus.event.domain;

/**
 * Describes how an event spans time:
 * <ul>
 *   <li><b>SINGLE_DAY</b> – starts and ends on the same calendar day</li>
 *   <li><b>MULTI_DAY_FIXED</b> – repeats at the same time each day (e.g. 10 AM–2 PM daily for 3 days)</li>
 *   <li><b>MULTI_DAY_CONTINUOUS</b> – one continuous block across midnight (e.g. hackathon)</li>
 *   <li><b>FLEXIBLE</b> – user-defined time slots per day (potentially different times each day)</li>
 * </ul>
 */
public enum EventTimingModel {
    SINGLE_DAY,
    MULTI_DAY_FIXED,
    MULTI_DAY_CONTINUOUS,
    FLEXIBLE
}
