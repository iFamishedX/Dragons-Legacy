package dev.dragonslegacy.config;

import eu.pb4.placeholders.api.node.TextNode;
import eu.pb4.placeholders.api.parsers.NodeParser;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.spongepowered.configurate.ConfigurationNode;
import org.spongepowered.configurate.serialize.SerializationException;
import org.spongepowered.configurate.serialize.TypeSerializer;

import java.lang.reflect.Type;

public class MessageString {
    public final String value;
    public final TextNode node;

    public MessageString(NodeParser parser, String value) {
        this.value = value;
        this.node = parser.parseNode(value);
    }

    public String toString() {
        return this.value;
    }

    public static class Serializer implements TypeSerializer<MessageString> {
        public final NodeParser PARSER;

        public Serializer(NodeParser parser) {
            this.PARSER = parser;
        }

        @Override
        public MessageString deserialize(Type type, ConfigurationNode node) {
            return new MessageString(PARSER, node.getString());
        }

        @Override
        public void serialize(
            Type type,
            @Nullable MessageString obj,
            ConfigurationNode node
        ) throws SerializationException {
            if (obj == null) node.raw(null);
            else node.set(obj.value);
        }
    }
}