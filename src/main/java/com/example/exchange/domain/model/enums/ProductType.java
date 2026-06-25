/*
 * File purpose: Product type discriminator so spot and perpetual accounting cannot share the wrong path.
 */
package com.example.exchange.domain.model.enums;

public enum ProductType {
    SPOT,
    PERPETUAL
}
