/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.shallowinggg.doran.transport.protocol;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.List;

public abstract class RemotingSerializable {
    private final static Charset CHARSET_UTF8 = StandardCharsets.UTF_8;
    private final static ObjectMapper MAPPER = new ObjectMapper();

    public static byte[] encode(final Object obj) {
        final String json = toJson(obj);
        return json.getBytes(CHARSET_UTF8);
    }

    public static String toJson(final Object obj) {
        try {
            return MAPPER.writeValueAsString(obj);
        } catch (JsonProcessingException e) {
            // won't happen
        }
        return "";
    }

    public static <T> T decode(final byte[] data, Class<T> classOfT) {
        final String json = new String(data, CHARSET_UTF8);
        return fromJson(json, classOfT);
    }

    public static <T> T fromJson(String json, Class<T> classOfT) {
        try {
            return MAPPER.readValue(json, classOfT);
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }

    public byte[] encode() {
        final String json = this.toJson();
        if (json != null) {
            return json.getBytes(CHARSET_UTF8);
        }
        return null;
    }

    public String toJson() {
        return toJson(this);
    }

    public static <T> List<T> decodeArray(final byte[] data) {
        final String json = new String(data, CHARSET_UTF8);
        try {
            return MAPPER.readValue(json, new TypeReference<List<T>>() {
            });
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }
}
