module ElixirBot.main {
    requires kotlin.stdlib;
    requires com.github.jezza.toml;
    requires kord.gateway;
    requires kord.common;
    requires extra.phishing;
    requires kotlin.logging.jvm;
    requires org.kohsuke.github.api;
    requires kord.extensions;
    requires kord.core;
    requires kotlinx.datetime;
    requires kord.rest;
    requires kotlinx.coroutines.core.jvm;
    requires ktor.client.core.jvm;
    requires ktor.client.json.jvm;
    requires kotlinx.serialization.core;
    requires exposed.core;
    requires ktor.utils.jvm;
    requires kotlin.stdlib.jdk7;
    requires kmongo.async.shared;
    requires org.mongodb.driver.core;
    requires org.mongodb.bson;
    requires kmongo.coroutine.core;
    requires kmongo.property;
}