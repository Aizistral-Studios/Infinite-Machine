package com.aizistral.infmachine.utils;

import lombok.Value;

@Value
public class Tuple<A, B> {
    A a;
    B b;
}