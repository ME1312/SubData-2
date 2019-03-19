package net.ME1312.SubData.Server.Library;

import net.ME1312.Galaxi.Library.Config.YAMLSection;
import org.msgpack.value.MapValue;
import org.msgpack.value.Value;
import org.msgpack.value.ValueFactory;

import java.util.Collection;
import java.util.LinkedList;
import java.util.Map;

/**
 * Message Pack Handler Class
 */
public class MessagePackHandler {

    /**
     * Convert a YAMLSection to a MessagePack Map
     *
     * @param config YAMLSection
     * @return MessagePack Map
     */
    public static MapValue pack(YAMLSection config) {
        return (MapValue) complicate(config.get());
    }
    @SuppressWarnings("unchecked")
    private static Value complicate(Object value) {
        if (value == null) {
            return ValueFactory.newNil();
        } else if (value instanceof Value) {
            return (Value) value;
        } else if (value instanceof Map) {
            ValueFactory.MapBuilder map = ValueFactory.newMapBuilder();
            for (String key : ((Map<String, ?>) value).keySet()) {
                Value v = complicate(((Map<String, ?>) value).get(key));
                if (v != null) map.put(ValueFactory.newString(key), v);
            }
            return map.build();
        } else if (value instanceof Collection) {
            LinkedList<Value> values = new LinkedList<Value>();
            for (Object object : (Collection<?>) value) {
                Value v = complicate(object);
                if (v != null) values.add(v);
            }
            return ValueFactory.newArray(values);
        } else if (value instanceof Boolean) {
            return ValueFactory.newBoolean((boolean) value);
        } else if (value instanceof Number) {
            if (((Number) value).doubleValue() == (double)(int) ((Number) value).doubleValue()) {
                return ValueFactory.newInteger(((Number) value).longValue());
            } else {
                return ValueFactory.newFloat(((Number) value).doubleValue());
            }
        } else if (value instanceof String) {
            return ValueFactory.newString((String) value);
        } else {
            return null;
        }
    }

    /**
     * Convert a MessagePack Map to a YAMLSection
     *
     * @param msgpack MessagePack Map
     * @return YAMLSection
     */
    @SuppressWarnings("unchecked")
    public static YAMLSection unpack(MapValue msgpack) {
        YAMLSection section = new YAMLSection();

        boolean warned = false;
        Map<Value, Value> map = msgpack.map();
        for (Value key : map.keySet()) {
            if (key.isStringValue()) {
                section.set(key.asStringValue().asString(), simplify(map.get(key)));
            } else if (!warned) {
                new IllegalStateException("MessagePack contains non-string key(s)").printStackTrace();
                warned = true;
            }
        }

        return section;
    }
    private static Object simplify(Value value) {
        Object simple = value;
        if (value.isNilValue()) {
            simple = null;
        } else if (value.isMapValue()) {
            Map<Value, Value> map = value.asMapValue().map();
            simple = unpack(value.asMapValue());
        } else if (value.isArrayValue()) {
            simple = value.asArrayValue().list();
        } else if (value.isBooleanValue()) {
            simple = value.asBooleanValue().getBoolean();
        } else if (value.isFloatValue()) {
            if (value.asFloatValue().toDouble() == (double)(float) value.asFloatValue().toDouble()) {
                simple = value.asFloatValue().toFloat();
            } else {
                simple = value.asFloatValue().toDouble();
            }
        } else if (value.isIntegerValue()) {
            if (value.asIntegerValue().isInByteRange()) {
                simple = value.asIntegerValue().asByte();
            } else if (value.asIntegerValue().isInShortRange()) {
                simple = value.asIntegerValue().asShort();
            } else if (value.asIntegerValue().isInIntRange()) {
                simple = value.asIntegerValue().asInt();
            } else if (value.asIntegerValue().isInLongRange()) {
                simple = value.asIntegerValue().asLong();
            } else {
                simple = value.asIntegerValue().asBigInteger();
            }
        } else if (value.isStringValue()) {
            simple = value.asStringValue().asString();
        }

        return simple;
    }
}
