package com.shallowinggg.doran.client;

import com.shallowinggg.doran.common.util.Assert;
import com.shallowinggg.doran.common.util.CollectionUtils;
import com.shallowinggg.doran.transport.protocol.DoranSerializable;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.Serializable;
import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Map;
import java.util.function.Supplier;

/**
 * Base message object, only support {@link String} property
 * and byte[] body.
 * <p>
 * You can create message by invoke static method, e.g.
 * {@link #createMessage(String)}, {@link #createMessage(Map)},
 * {@link #createMessage(Map, String)} and
 * {@link #createMessage(Map, byte[])}.
 * If you want create empty message, you can also invoke
 * {@link #createEmptyMessage()}.
 * <p>
 * Besides, this object can be serialized by invoking its method
 * {@link #encode()} and deserialize by {@link #decode(byte[])}.
 * Its transport structure is like this:
 * =================================================
 * \ properties len \ body len \ properties \ body \
 * \     4 bytes    \ 4 bytes  \    any     \  any \
 * =================================================
 *
 * <p>
 * Note: the property value must not be null, this can guarantee
 * the semantic if some property is exist.
 *
 * @author shallowinggg
 * @see #createMessage(String)
 * @see #createMessage(Map)
 * @see #createMessage(Map, String)
 * @see #createMessage(Map, byte[])
 * @see #createEmptyMessage()
 */
public class Message implements Serializable {
    private static final Charset UTF_8 = StandardCharsets.UTF_8;
    private static final Supplier<Message> EMPTY_MESSAGE = () -> new Message(null, null);
    private static final byte[] EMPTY_BYTE_ARRAY = new byte[0];

    @Nullable
    private final Map<String, String> properties;

    @Nullable
    private final byte[] body;

    private Message(@Nullable Map<String, String> properties, @Nullable byte[] body) {
        this.properties = properties;
        this.body = body;
    }

    /**
     * Create message with the given String. The string value
     * will only be used to construct {@link #body}.
     *
     * @param body the string that represents body
     * @return message with nonnull body
     */
    public static Message createMessage(@NotNull String body) {
        Assert.notNull(body, "'body' must not be null");
        return new Message(null, body.getBytes(UTF_8));
    }

    /**
     * Create message with the given Map. The map will only
     * be used to construct {@link #properties}.
     * <p>
     * Note: the map's value must not be null.
     *
     * @param properties the given properties
     * @return message with nonnull properties
     */
    public static Message createMessage(@NotNull Map<String, String> properties) {
        Assert.notNull(properties, "'properties' must not be null");
        checkMap(properties);
        return new Message(properties, null);
    }

    /**
     * Create message with the given String and Map. They will
     * be used to construct {@link #properties} and {@link #body}.
     * <p>
     * Note: the map's value must not be null.
     *
     * @param properties the given properties
     * @param body       the string that represents body
     * @return message with nonnull properties and body
     */
    public static Message createMessage(@NotNull Map<String, String> properties, @NotNull String body) {
        Assert.notNull(body, "'body' must not be null");
        Assert.notNull(properties, "'properties' must not be null");
        checkMap(properties);
        return new Message(properties, body.getBytes(UTF_8));
    }

    /**
     * Create message with the given byte array and Map. They will
     * be used to construct {@link #properties} and {@link #body}.
     * <p>
     * Note: the map's value must not be null.
     *
     * @param properties the given properties
     * @param body       the byte array
     * @return message with nonnull properties and body
     */
    public static Message createMessage(@NotNull Map<String, String> properties, @NotNull byte[] body) {
        Assert.notNull(body, "'body' must not be null");
        Assert.notNull(properties, "'properties' must not be null");
        checkMap(properties);
        return new Message(properties, body);
    }

    /**
     * Create an empty message that properties and body are all null.
     *
     * @return empty message
     */
    public static Message createEmptyMessage() {
        return EMPTY_MESSAGE.get();
    }

    /**
     * Retrieve the value of property key. If the property
     * is not exist, this method will return {@code null}.
     * Otherwise, it will return an nonnull value.
     *
     * @param key property key
     * @return {@code null} if properties is empty or property key is not exist,
     * otherwise return the value of property key.
     */
    @Nullable
    public String getProperty(@NotNull String key) {
        Assert.hasText(key);
        final Map<String, String> properties = this.properties;
        if (CollectionUtils.isNotEmpty(properties)) {
            return properties.get(key);
        }
        return null;
    }

    @Nullable
    public byte[] getBody() {
        return this.body;
    }

    @Nullable
    public Map<String, String> getProperties() {
        return this.properties;
    }

    public byte[] encode() {
        // properties and body length
        int length = 8;
        byte[] props;
        byte[] body = this.body;
        if (CollectionUtils.isNotEmpty(properties)) {
            props = DoranSerializable.mapSerialize(properties);
        } else {
            props = EMPTY_BYTE_ARRAY;
        }
        if (body == null) {
            body = EMPTY_BYTE_ARRAY;
        }
        length += props.length;
        length += body.length;

        ByteBuffer buffer = ByteBuffer.allocate(length);
        buffer.putInt(props.length);
        buffer.putInt(body.length);
        buffer.put(props);
        buffer.put(body);

        return buffer.array();
    }

    public static Message decode(@NotNull byte[] message) {
        Assert.notNull(message, "'message' must not be null");
        ByteBuffer buffer = ByteBuffer.wrap(message);
        int propsLen = buffer.getInt();
        int bodyLen = buffer.getInt();
        Map<String, String> props = null;
        if (propsLen != 0) {
            byte[] propsData = new byte[propsLen];
            buffer.get(propsData);
            props = DoranSerializable.mapDeserialize(propsData);
        }
        byte[] body = null;
        if (bodyLen != 0) {
            body = new byte[bodyLen];
            buffer.get(body);
        }

        if (props == null && body == null) {
            return createEmptyMessage();
        }
        return new Message(props, body);
    }

    private static void checkMap(Map<String, String> properties) {
        for (Map.Entry<String, String> entry : properties.entrySet()) {
            String value = entry.getValue();
            if (value == null) {
                throw new IllegalArgumentException("'properties' must not have null value, key: " + entry.getKey());
            }
        }
    }

    @Override
    public String toString() {
        return "Message{" +
                "properties=" + properties +
                ", body=" + Arrays.toString(body) +
                '}';
    }
}
