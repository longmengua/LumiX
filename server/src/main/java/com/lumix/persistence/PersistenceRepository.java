package com.lumix.persistence;

/**
 * repository marker interface。
 *
 * 這個 marker 只負責標記「這是 persistence 邊界的一部分」，不提供 CRUD 預設行為，
 * 目的是讓後續 application service 有清楚的依賴方向。
 */
public interface PersistenceRepository {
}
