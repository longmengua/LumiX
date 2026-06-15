/*
 * File purpose: 訊息未讀數量聚合投影。
 */
package com.example.exchange.domain.repository;

public interface MessageCenterUnreadCountProjection {

    String getCategory();

    long getUnreadCount();
}
