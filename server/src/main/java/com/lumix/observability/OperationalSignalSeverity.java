package com.lumix.observability;
/** 可觀測性事件嚴重度；僅為 routing evidence，不觸發外部 alert。 */
public enum OperationalSignalSeverity { INFO, WARNING, CRITICAL }
