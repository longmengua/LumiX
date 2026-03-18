package com.example.exchange.application.command;

import java.util.UUID;

public record CancelOrderCommand(UUID orderId, long uid) {}