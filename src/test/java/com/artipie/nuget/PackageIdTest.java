/*
 * MIT License
 *
 * Copyright (c) 2020 Artipie
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */
package com.artipie.nuget;

import org.hamcrest.MatcherAssert;
import org.hamcrest.Matchers;
import org.hamcrest.core.IsEqual;
import org.junit.jupiter.api.Test;

/**
 * Tests for {@link PackageId}.
 *
 * @since 0.1
 */
public class PackageIdTest {

    @Test
    void shouldPreserveOriginal() {
        final String id = "Microsoft.Extensions.Logging";
        MatcherAssert.assertThat(
            new PackageId(id).original(),
            Matchers.is(id)
        );
    }

    @Test
    void shouldGenerateRootKey() {
        MatcherAssert.assertThat(
            new PackageId("Artipie.Module").rootKey().string(),
            new IsEqual<>("artipie.module")
        );
    }

    @Test
    void shouldGenerateVersionsKey() {
        MatcherAssert.assertThat(
            new PackageId("Newtonsoft.Json").versionsKey().string(),
            Matchers.is("newtonsoft.json/index.json")
        );
    }

    @Test
    void shouldGenerateLower() {
        MatcherAssert.assertThat(
            new PackageId("My.Lib").lower(),
            Matchers.is("my.lib")
        );
    }
}
