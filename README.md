# java21-OLAP

## Package Skeleton (Clean Architecture)
```
com.example
├─ application
│ ├─ command
│ │ ├─ PlaceOrderCommand.java
│ │ ├─ TransferMarginCommand.java
│ │ ├─ SnapshotRecoverCommand.java
│ │ └─ LiquidateCommand.java
│ ├─ event
│ │ ├─ DomainEventPublisher.java
│ │ └─ handlers/PositionLiquidatedHandler.java
│ ├─ scheduler
│ │ ├─ FundingRateScheduler.java
│ │ └─ SnapshotScheduler.java
│ ├─ service
│ │ ├─ OrderService.java
│ │ ├─ MarginService.java
│ │ └─ RecoveryService.java
│ └─ usecase
│ ├─ PlaceOrderUseCase.java
│ ├─ TransferMarginUseCase.java
│ ├─ SnapshotRecoverUseCase.java
│ └─ LiquidateUseCase.java
├─ domain
│ ├─ event
│ │ ├─ PositionLiquidated.java
│ │ ├─ SnapshotCreated.java
│ │ └─ TradeExecuted.java
│ ├─ model
│ │ ├─ Account.java
│ │ ├─ MarginMode.java
│ │ ├─ Order.java
│ │ ├─ OrderSide.java
│ │ ├─ OrderType.java
│ │ ├─ Position.java
│ │ ├─ Symbol.java
│ │ └─ Snapshot.java
│ └─ repository
│ ├─ AccountRepository.java
│ ├─ OrderRepository.java
│ ├─ PositionRepository.java
│ ├─ EventStore.java
│ └─ SnapshotRepository.java
├─ infra
│ ├─ config
│ │ ├─ KafkaConfig.java
│ │ └─ RedisConfig.java
│ ├─ kafka
│ │ ├─ KafkaDomainEventPublisher.java
│ │ └─ KafkaEventStore.java
│ └─ redis
│ ├─ RedisAccountRepository.java
│ ├─ RedisOrderRepository.java
│ ├─ RedisPositionRepository.java
│ └─ RedisSnapshotRepository.java
└─ interfaces
├─ consumer
│ └─ TradeEventConsumer.java
└─ web
├─ controller
│ ├─ OrderController.java
│ ├─ MarginController.java
│ └─ RecoveryController.java
├─ dto
│ ├─ PlaceOrderRequest.java
│ ├─ TransferRequest.java
│ └─ ApiResponse.java
├─ exception
│ ├─ GlobalExceptionHandler.java
│ └─ BizException.java
├─ interceptor
│ └─ RequestLoggingInterceptor.java
└─ validator
```