package com.aizistral.infmachine.utils;

import lombok.Value;

@Value
public class Triple<A, B, C> {
    A a;
    B b;
    C c;
}
