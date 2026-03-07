package com.beingadish.AroundU.chat.entity;

/**
 * Tracks delivery lifecycle of a chat message, similar to WhatsApp. SENT →
 * DELIVERED → READ
 */
public enum MessageStatus {
    SENT,
    DELIVERED,
    READ
}
