package com.lumix.api.gateway;
/** 被 transport 顯式帶入的資料健康度；非 HEALTHY 不能服務敏感 contract。 */
public enum ApiHealthState { HEALTHY, STALE, DEGRADED, UNKNOWN }
