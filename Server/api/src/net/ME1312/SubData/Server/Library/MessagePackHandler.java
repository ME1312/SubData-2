package net.ME1312.SubData.Server.Library;

import net.ME1312.Galaxi.Library.Map.ObjectMap;
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
     * Convert an ObjectMap to a MessagePack Map
     *
     * @param map ObjectMap
     * @return MessagePack Map
     */
    public static MapValue pack(ObjectMap<?> map) {
        return (MapValue) complicate(map.get());
    }
    @SuppressWarnings("unchecked")
    private static Value complicate(Object value) {
        if (value == null) {
            return ValueFactory.newNil();
        } else if (value instanceof Value) {
            return (Value) value;
        } else if (value instanceof Map) {
            ValueFactory.MapBuilder map = ValueFactory.newMapBuilder();
            for (Object key : ((Map<?, ?>) value).keySet()) {
                Value k = complicate(key);
                Value v = complicate(((Map<?, ?>) value).get(key));
                if (v != null) map.put(k, v);
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
            if (((Number) value).doubleValue() == (double)(long) ((Number) value).doubleValue()) {
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
     * Convert a MessagePack Map to a ObjectMap
     *
     * @param msgpack MessagePack Map
     * @param <K> Key Type
     * @return ObjectMap
     */
    @SuppressWarnings("unchecked")
    public static <K> ObjectMap<K> unpack(MapValue msgpack) {
        ObjectMap<Object> section = new ObjectMap();

        Map<Value, Value> map = msgpack.map();
        for (Value key : map.keySet()) {
            section.set(simplify(key, true), simplify(map.get(key), false));
        }

        return (ObjectMap<K>) section;
    }
    private static Object simplify(Value value, boolean asKey) {
        Object simple = value;
        if (value.isNilValue()) {
            simple = null;
        } else if (value.isMapValue()) {
            simple = unpack(value.asMapValue());
        } else if (value.isArrayValue()) {
            LinkedList<Object> objects = new LinkedList<Object>();
            for (Value v : value.asArrayValue().list()) objects.add(simplify(v, false));
            simple = objects;
        } else if (value.isBooleanValue()) {
            simple = value.asBooleanValue().getBoolean();
        } else if (value.isFloatValue()) {
            if (asKey) {
                simple = value.asIntegerValue().toDouble();
            } else if (value.asFloatValue().toDouble() == (double)(float) value.asFloatValue().toDouble()) {
                simple = value.asFloatValue().toFloat();
            } else {
                simple = value.asFloatValue().toDouble();
            }
        } else if (value.isIntegerValue()) {
            if (asKey) {
                simple = value.asIntegerValue().asInt();
            } else if (value.asIntegerValue().isInByteRange()) {
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
