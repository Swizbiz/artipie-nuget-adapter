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
package com.artipie.nuget.http.metadata;

import com.artipie.asto.Storage;
import com.artipie.asto.blocking.BlockingStorage;
import com.artipie.asto.memory.InMemoryStorage;
import com.artipie.nuget.PackageId;
import com.artipie.nuget.PackageIdentity;
import com.artipie.nuget.Repository;
import com.artipie.nuget.Version;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import javax.json.JsonObject;
import org.hamcrest.Matcher;
import org.hamcrest.MatcherAssert;
import org.hamcrest.core.AllOf;
import org.hamcrest.core.StringContains;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import wtf.g4s8.hamcrest.json.JsonContains;
import wtf.g4s8.hamcrest.json.JsonHas;
import wtf.g4s8.hamcrest.json.JsonValueIs;

/**
 * Tests for {@link RegistrationPage}.
 *
 * @since 0.1
 * @checkstyle ClassDataAbstractionCouplingCheck (2 lines)
 */
class RegistrationPageTest {

    @Test
    void shouldGenerateJson() throws Exception {
        final Storage storage = new InMemoryStorage();
        final Repository repository = new Repository(storage);
        final PackageId id = new PackageId("My.Lib");
        final String lower = "0.1";
        final String upper = "0.2";
        final List<Version> versions = Stream.of(lower, "0.1.2", upper)
            .map(Version::new)
            .collect(Collectors.toList());
        for (final Version version : versions) {
            new BlockingStorage(storage).save(
                new PackageIdentity(id, version).nuspecKey(),
                String.join(
                    "",
                    "<?xml version=\"1.0\"?>",
                    "<package xmlns=\"http://schemas.microsoft.com/packaging/2013/05/nuspec.xsd\">",
                    "<metadata>",
                    String.format("<id>%s</id>", id.original()),
                    String.format("<version>%s</version>", version.normalized()),
                    "</metadata>",
                    "</package>"
                ).getBytes()
            );
        }
        MatcherAssert.assertThat(
            new RegistrationPage(repository, RegistrationPageTest::contentUrl, id, versions).json()
                .toCompletableFuture().join(),
            new AllOf<>(
                Arrays.asList(
                    new JsonHas("lower", new JsonValueIs(lower)),
                    new JsonHas("upper", new JsonValueIs(upper)),
                    new JsonHas("count", new JsonValueIs(versions.size())),
                    new JsonHas(
                        "items",
                        new JsonContains(
                            versions.stream()
                                .map(version -> entryMatcher(id, version))
                                .collect(Collectors.toList())
                        )
                    )
                )
            )
        );
    }

    @Test
    void shouldFailToGenerateJsonWhenEmpty() {
        final String id = "Some.Lib";
        final Throwable throwable = Assertions.assertThrows(
            IllegalStateException.class,
            () -> new RegistrationPage(
                new Repository(new InMemoryStorage()),
                RegistrationPageTest::contentUrl,
                new PackageId(id),
                Collections.emptyList()
            ).json()
        );
        MatcherAssert.assertThat(
            throwable.getMessage(),
            new AllOf<>(
                Arrays.asList(
                    new StringContains(true, "Registration page contains no versions"),
                    new StringContains(false, id)
                )
            )
        );
    }

    private static Matcher<JsonObject> entryMatcher(final PackageId id, final Version version) {
        return new AllOf<>(
            Arrays.asList(
                new JsonHas(
                    "catalogEntry",
                    new AllOf<>(
                        Arrays.asList(
                            new JsonHas("id", new JsonValueIs(id.original())),
                            new JsonHas("version", new JsonValueIs(version.normalized()))
                        )
                    )
                ),
                new JsonHas(
                    "packageContent",
                    new JsonValueIs(
                        RegistrationPageTest.contentUrl(new PackageIdentity(id, version)).toString()
                    )
                )
            )
        );
    }

    private static URL contentUrl(final PackageIdentity identity) {
        try {
            return new URL(
                String.format("http://localhost:8080/content/%s", identity.nupkgKey().string())
            );
        } catch (final MalformedURLException ex) {
            throw new IllegalStateException(ex);
        }
    }
}
