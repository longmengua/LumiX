package com.lumix.persistence;

/**
 * persistence 層的受控錯誤分類。
 *
 * 這些分類只描述資料存取失敗的性質，不對外暴露 SQL 或連線細節。
 */
public enum PersistenceErrorCode {
    CONSTRAINT_VIOLATION,
    NOT_FOUND,
    CONNECTION_FAILURE,
    QUERY_FAILURE,
    UNKNOWN
}
