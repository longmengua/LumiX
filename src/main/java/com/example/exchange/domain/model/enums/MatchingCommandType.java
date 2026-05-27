/*
 * 檔案用途：領域 enum，描述 matching command log 中的指令種類。
 */
package com.example.exchange.domain.model.enums;

/**
 * 可 replay 撮合指令類型。
 */
public enum MatchingCommandType {
    SUBMIT,
    CANCEL,
    AMEND
}
