/**
 * Copyright (c) 2015 Wandoujia Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.wandoulabs.nedis.protocol;

import java.nio.charset.StandardCharsets;

/**
 * @author Apache9
 */
public enum RedisKeyword {

    ALPHA, ASC, BY, COUNT, DESC, EX, EXISTS, FLUSH, GET, GETNAME, KILL, LIMIT, LIST, LOAD,
    MATCH, NX, PX, REPLACE, RESETSTAT, REWRITE, SET, SETNAME, STORE, WITHSCORES, XX;

    public final byte[] raw;

    RedisKeyword() {
        raw = name().getBytes(StandardCharsets.UTF_8);
    }
}
