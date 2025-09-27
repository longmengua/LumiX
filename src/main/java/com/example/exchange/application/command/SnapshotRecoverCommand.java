package com.example.exchange.application.command;

/** 快照恢復指令（依 uid 恢復該使用者狀態） */
public record SnapshotRecoverCommand(long uid) {}
