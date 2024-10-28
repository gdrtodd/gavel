package approaches.symbolic.nodes;

import approaches.symbolic.SymbolMap;
import approaches.symbolic.SymbolMap.MappedSymbol;
import game.functions.booleans.BooleanConstant;
import game.functions.dim.DimConstant;
import game.functions.floats.FloatConstant;
import game.functions.ints.IntConstant;

import java.util.List;
import java.util.Objects;

/**
 * Represents string, int, float and boolean values. It's always a leaf node and compiles to a constant.
 */
public class PrimitiveNode extends GenerationNode {

    public enum PrimitiveType {INT, FLOAT, DIM, STRING, BOOLEAN}
    private Object value;

    public PrimitiveNode(MappedSymbol symbol, GenerationNode parent) {
        super(symbol, parent);
    }

    public void setValue(Object value) {
        this.value = value;
    }

    public void setUnparsedValue(String strValue) {
        value = switch (getType()) {
            case INT, DIM -> parseInt(strValue);
            case FLOAT -> Float.parseFloat(strValue);
            case STRING -> strValue;
            case BOOLEAN -> Boolean.parseBoolean(strValue);
        };

        if (IntConstant.class.isAssignableFrom(symbol.cls()))
            value = new IntConstant((Integer) value);

        if (DimConstant.class.isAssignableFrom(symbol.cls()))
            value = new DimConstant((Integer) value);

        if (FloatConstant.class.isAssignableFrom(symbol.cls()))
            value = new FloatConstant((Float) value);

        if (BooleanConstant.class.isAssignableFrom(symbol.cls()))
            value = new BooleanConstant((Boolean) value);

    }

    static int parseInt(String strValue) {
        return switch (strValue) {
            case "Infinity" -> 1000000000;
            case "-Infinity" -> -1000000000;
            default -> Integer.parseInt(strValue);
        };
    }

    Object instantiateLudeme() {
        return value;
    }

    public List<GenerationNode> nextPossibleParameters(SymbolMap symbolMap) {
        return List.of();
    }

    @Override
    public void addParameter(GenerationNode param) {
        throw new RuntimeException("Primitive nodes are terminal");
    }

    @Override
    public boolean isComplete() {
        return value != null;
    }

    @Override
    public String buildString() {
        return buildDescription();
    }

    @Override
    public String buildDescription() {
        String label = "";
        if (symbol.label != null)
            label = symbol.label + ":";

        return label + strValue();
    }

    String strValue() {
        if (value == null)
            return "NEW_" + getType() + "?";

        if (value instanceof String)
            return "\"" + value + "\"";

        String strValue = value.toString();
//        if (Objects.equals(strValue, "1000000000"))
//            return "Infinity";
//        if (Objects.equals(strValue, "-1000000000"))
//            return "-Infinity";

        if (Objects.equals(strValue, "true"))
            return "True";
        if (Objects.equals(strValue, "false"))
            return "False";

        if (strValue.endsWith(".0"))
            return strValue.substring(0, strValue.length() - 2); // TODO remove this

        return strValue;
    }

    @Override
    public PrimitiveNode copyDown() {
        PrimitiveNode clone = (PrimitiveNode) super.copyDown();
        clone.setValue(value);
        return clone;
    }

    public PrimitiveType getType() {
        return typeOf(symbol.path());
    }

    public Object value() {
        return value;
    }

    public static PrimitiveType typeOf(String path) {
        switch (path) {
            case "int", "java.lang.Integer", "game.functions.ints.IntConstant" -> {
                return PrimitiveType.INT;
            }

            case "game.functions.dim.DimConstant" -> {
                return PrimitiveType.DIM;
            }

            case "float", "java.lang.Float", "game.functions.floats.FloatConstant" -> {
                return PrimitiveType.FLOAT;
            }

            case "boolean", "java.lang.Boolean", "game.functions.booleans.BooleanConstant" -> {
                return PrimitiveType.BOOLEAN;
            }

            case "java.lang.String" -> {
                return PrimitiveType.STRING;
            }

            default -> {
                return null;
            }
        }
    }
}
