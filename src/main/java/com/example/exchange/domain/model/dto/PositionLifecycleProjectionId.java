/*
 * 檔案用途：position_lifecycle_projection 複合主鍵。
 */
package com.example.exchange.domain.model.dto;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.io.Serializable;

@Getter
@Setter
@NoArgsConstructor
@EqualsAndHashCode
public class PositionLifecycleProjectionId implements Serializable {

    private Long uid;
    private String symbol;
}
