/*
 * 檔案用途：Repository 介面，定義領域層需要的資料存取契約。
 */
package com.example.exchange.domain.repository;

import com.example.exchange.domain.model.entity.DlqEvent;

import java.util.List;

public interface DlqRepository {

    void append(DlqEvent event);

    List<DlqEvent> latest(int limit);
}
