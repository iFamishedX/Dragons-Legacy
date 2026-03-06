package dev.dragonslegacy.config;

import dev.dragonslegacy.utils.ExpressionEvaluator;
import net.objecthunter.exp4j.Expression;
import net.objecthunter.exp4j.ExpressionBuilder;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.configurate.ConfigurationNode;
import org.spongepowered.configurate.serialize.SerializationException;
import org.spongepowered.configurate.serialize.TypeSerializer;

import java.lang.reflect.Type;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

public class Condition {
    public static final Condition EMPTY = new Condition(null);

    public final String expressionString;
    public final Expression expression;

    public Condition(@Nullable String expressionString) {
        this.expressionString = expressionString != null ? expressionString : "1";
        this.expression = new ExpressionBuilder(this.expressionString)
            .variables(Variables.ALL)
            .functions(ExpressionEvaluator.Functions.ALL)
            .operator(ExpressionEvaluator.Operators.ALL)
            .build();
    }

    public boolean isEmpty() {
        return Objects.equals(expressionString, "1");
    }

    public double evaluate(Map<String, Double> variables) {
        if (isEmpty()) return 1;
        expression.setVariables(variables);
        return expression.evaluate();
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof Condition condition)) return false;
        return Objects.equals(expressionString, condition.expressionString);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(expressionString);
    }

    @Override
    public String toString() {
        return expressionString;
    }

    public static class Variables {
        public static final String BEARER_TIME = "bearerTime";
        public static final String CONTINUOUS_BLOCK_TIME = "blockTime";
        public static final String TOTAL_BLOCK_TIME = "totalBlockTime";
        public static final String CONTINUOUS_PLAYER_TIME = "playerTime";
        public static final String TOTAL_PLAYER_TIME = "totalPlayerTime";
        public static final String CONTINUOUS_ITEM_TIME = "itemTime";
        public static final String TOTAL_ITEM_TIME = "totalItemTime";
        public static final String CONTINUOUS_ENTITY_TIME = "entityTime";
        public static final String TOTAL_ENTITY_TIME = "totalEntityTime";
        public static final String CONTINUOUS_INVENTORY_TIME = "invTime";
        public static final String TOTAL_INVENTORY_TIME = "totalInvTime";
        public static final String CONTINUOUS_FALLING_BLOCK_TIME = "fallingTime";
        public static final String TOTAL_FALLING_BLOCK_TIME = "totalFallingTime";
        public static final String X = "x";
        public static final String Y = "y";
        public static final String Z = "z";
        public static final String RAND_X = "randX";
        public static final String RAND_Y = "randY";
        public static final String RAND_Z = "randZ";

        public static final Set<String> ALL = Set.of(
            TOTAL_BLOCK_TIME, CONTINUOUS_BLOCK_TIME, BEARER_TIME, X, Y, Z, RAND_X, RAND_Y, RAND_Z
        );
    }

    public static class Serializer implements TypeSerializer<Condition> {
        public static final Serializer INSTANCE = new Serializer();

        private Serializer() {}

        @Override
        public Condition deserialize(Type type, ConfigurationNode node) {
            return new Condition(node.getString());
        }

        @Override
        public void serialize(
            Type type,
            @Nullable Condition obj,
            ConfigurationNode node
        ) throws SerializationException {
            if (obj == null) node.raw(null);
            else node.set(obj.expressionString);
        }
    }
}
