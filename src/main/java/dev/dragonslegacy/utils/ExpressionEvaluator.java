package dev.dragonslegacy.utils;

import net.objecthunter.exp4j.Expression;
import net.objecthunter.exp4j.ExpressionBuilder;
import net.objecthunter.exp4j.function.Function;
import net.objecthunter.exp4j.operator.Operator;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static dev.dragonslegacy.DragonsLegacyMod.LOGGER;

public class ExpressionEvaluator {
    private final Map<String, Double> variables;

    public ExpressionEvaluator(Map<String, Double> variables) {
        this.variables = variables;
    }

    /**
     * Evaluates all expressions in the given commandTemplate string and replaces them with their evaluated values.
     *
     * @param command The commandTemplate string containing expressions in the format ${expression}.
     * @return The commandTemplate string with all expressions evaluated and replaced.
     * @throws IllegalArgumentException If there is an error evaluating one or more expressions.
     */
    public String evaluateExpressions(String command) throws IllegalArgumentException {
        Pattern expressionPattern = Pattern.compile("\\$\\{([^{}]+)}");
        Matcher expressionMatcher = expressionPattern.matcher(command);
        Set<String> expressions = expressionMatcher
            .results()
            .map(r -> r.group(1))
            .collect(Collectors.toSet());

        DecimalFormat decimalFormat = new DecimalFormat(
            "#.########",
            DecimalFormatSymbols.getInstance(Locale.ENGLISH)
        );
        boolean evaluationError = false;
        for (String expression : expressions) {
            String number;
            try {
                number = decimalFormat.format(evaluate(expression));
            } catch (Exception e) {
                evaluationError = true;
                LOGGER.warn("Error evaluating expression '{}': {}", expression, e.getMessage());
                continue;
            }

            command = command.replace("${" + expression + "}", number);
        }
        if (evaluationError) throw new IllegalArgumentException("Error evaluating one or more expressions");
        return command;
    }

    public double evaluate(String expression) {
        Expression exp = new ExpressionBuilder(expression)
            .variables(variables.keySet())
            .functions(Functions.ALL)
            .operator(Operators.ALL)
            .build();
        exp.setVariables(variables);
        return exp.evaluate();
    }

    public static class Functions {
        public static final Function rnd = new Function("rnd") {
            @Override
            public double apply(double... args) {
                return Math.random() * args[0];
            }
        };
        public static final Function min = new Function("min", 2) {
            @Override
            public double apply(double... args) {
                return Math.max(args[0], args[1]);
            }
        };
        public static final Function max = new Function("max", 2) {
            @Override
            public double apply(double... args) {
                return Math.max(args[0], args[1]);
            }
        };
        public static final Function round = new Function("round", 2) {
            @Override
            public double apply(double... args) {
                if (args[1] < 0) throw new IllegalArgumentException();

                BigDecimal bd = BigDecimal.valueOf(args[0]);
                bd = bd.setScale((int) args[1], RoundingMode.HALF_UP);
                return bd.doubleValue();
            }
        };

        public static final List<Function> ALL = List.of(rnd, min, max, round);
    }

    public static class Operators {
        public static Operator eq = new Operator("==", 2, true, Operator.PRECEDENCE_ADDITION - 1) {
            @Override
            public double apply(double[] values) {
                return values[0] == values[1] ? 1 : 0;
            }
        };
        public static Operator neq = new Operator("!=", 2, true, Operator.PRECEDENCE_ADDITION - 1) {
            @Override
            public double apply(double[] values) {
                return values[0] != values[1] ? 1 : 0;
            }
        };
        public static Operator lt = new Operator("<", 2, true, Operator.PRECEDENCE_ADDITION - 1) {
            @Override
            public double apply(double[] values) {
                return values[0] < values[1] ? 1 : 0;
            }
        };
        public static Operator gt = new Operator(">", 2, true, Operator.PRECEDENCE_ADDITION - 1) {
            @Override
            public double apply(double[] values) {
                return values[0] > values[1] ? 1 : 0;
            }
        };
        public static Operator lte = new Operator("<=", 2, true, Operator.PRECEDENCE_ADDITION - 1) {
            @Override
            public double apply(double[] values) {
                return values[0] <= values[1] ? 1 : 0;
            }
        };
        public static Operator gte = new Operator(">=", 2, true, Operator.PRECEDENCE_ADDITION - 1) {
            @Override
            public double apply(double[] values) {
                return values[0] >= values[1] ? 1 : 0;
            }
        };
        public static Operator and = new Operator("&&", 2, true, Operator.PRECEDENCE_ADDITION - 2) {
            @Override
            public double apply(double[] values) {
                return values[0] != 0 && values[1] != 0 ? 1 : 0;
            }
        };
        public static Operator or = new Operator("||", 2, true, Operator.PRECEDENCE_ADDITION - 3) {
            @Override
            public double apply(double[] values) {
                return values[0] != 0 || values[1] != 0 ? 1 : 0;
            }
        };

        public static final List<Operator> ALL = List.of(eq, neq, lt, gt, lte, gte, and, or);
    }
}
