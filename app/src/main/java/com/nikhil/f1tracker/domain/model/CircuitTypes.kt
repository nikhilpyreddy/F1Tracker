package com.nikhil.f1tracker.domain.model

enum class CircuitType(val label: String, val note: String) {
    STREET("Street circuit", "Walls close by: higher Safety Car risk, qualifying position matters more"),
    TEMPORARY("Temporary circuit", "Public roads or park roads closed for the race; low grip early in the weekend"),
    PERMANENT("Permanent circuit", "Purpose-built track with run-off areas"),
}

/** Curated by hand: no API provides this. Unknown circuits simply show no type. */
val CIRCUIT_TYPES: Map<String, CircuitType> = mapOf(
    "monaco" to CircuitType.STREET,
    "baku" to CircuitType.STREET,
    "marina_bay" to CircuitType.STREET,
    "jeddah" to CircuitType.STREET,
    "vegas" to CircuitType.STREET,
    "madring" to CircuitType.STREET,
    "miami" to CircuitType.TEMPORARY,
    "albert_park" to CircuitType.TEMPORARY,
    "villeneuve" to CircuitType.TEMPORARY,
    "bahrain" to CircuitType.PERMANENT,
    "shanghai" to CircuitType.PERMANENT,
    "suzuka" to CircuitType.PERMANENT,
    "imola" to CircuitType.PERMANENT,
    "catalunya" to CircuitType.PERMANENT,
    "red_bull_ring" to CircuitType.PERMANENT,
    "silverstone" to CircuitType.PERMANENT,
    "hungaroring" to CircuitType.PERMANENT,
    "spa" to CircuitType.PERMANENT,
    "zandvoort" to CircuitType.PERMANENT,
    "monza" to CircuitType.PERMANENT,
    "americas" to CircuitType.PERMANENT,
    "rodriguez" to CircuitType.PERMANENT,
    "interlagos" to CircuitType.PERMANENT,
    "losail" to CircuitType.PERMANENT,
    "yas_marina" to CircuitType.PERMANENT,
    "sepang" to CircuitType.PERMANENT,
    "portimao" to CircuitType.PERMANENT,
    "istanbul" to CircuitType.PERMANENT,
    "ricard" to CircuitType.PERMANENT,
)
