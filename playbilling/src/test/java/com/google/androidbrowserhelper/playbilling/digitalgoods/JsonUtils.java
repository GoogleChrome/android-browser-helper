// Copyright 2020 Google Inc. All Rights Reserved.
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
//     http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.

package com.google.androidbrowserhelper.playbilling.digitalgoods;

import androidx.annotation.Nullable;

/**
 * A collection of some utility functions for creating JSON strings. We need this because some Play
 * Billing types take JSON representations of their objects in the constructor.
 */
public class JsonUtils {
    private JsonUtils() {}

    /**
     * Adds a JSON field of the given name and value. Does not add any separating commas, so use
     * this for the initial field.
     */
    public static void addFieldWithoutLeadingComma(StringBuilder b, String name, Object value) {
        // "name" = "value"
        b.append('"');
        b.append(name);
        b.append("\" = \"");
        b.append(value.toString());
        b.append('"');
    }

    /**
     * Adds a JSON field of the given name and value. Adds a comma before the field.
     */
    public static void addField(StringBuilder b, String name, Object value) {
        b.append(',');
        addFieldWithoutLeadingComma(b, name, value);
    }

    /**
     * Adds the provided field if the value is not {@code null}.
     */
    public static void addOptionalField(StringBuilder b, String name, @Nullable Object value) {
        if (value == null) return;
        addField(b, name, value);
    }
}

