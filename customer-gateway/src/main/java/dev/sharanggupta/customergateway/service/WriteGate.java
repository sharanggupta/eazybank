package dev.sharanggupta.customergateway.service;

import reactor.core.publisher.Mono;

public interface WriteGate {
    Mono<Void> checkWriteAllowed();
}
