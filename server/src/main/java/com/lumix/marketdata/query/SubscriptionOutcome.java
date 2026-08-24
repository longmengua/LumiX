package com.lumix.marketdata.query;

/** subscription decision 的可觀測結果；任何 loss/gap/non-healthy 都不會被標為正常 published delta。 */
public enum SubscriptionOutcome {
    PUBLISHED,
    DUPLICATE_IGNORED,
    RESNAPSHOT_REQUIRED,
    DISCONNECTED_AND_RESNAPSHOT
}
