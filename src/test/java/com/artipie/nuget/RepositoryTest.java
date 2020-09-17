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

import com.artipie.asto.Key;
import com.artipie.asto.blocking.BlockingStorage;
import com.artipie.asto.memory.InMemoryStorage;
import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import javax.json.Json;
import javax.json.JsonReader;
import javax.json.JsonString;
import org.hamcrest.FeatureMatcher;
import org.hamcrest.MatcherAssert;
import org.hamcrest.Matchers;
import org.hamcrest.collection.IsEmptyCollection;
import org.hamcrest.core.AllOf;
import org.hamcrest.core.AnyOf;
import org.hamcrest.core.Every;
import org.hamcrest.core.IsEqual;
import org.hamcrest.core.IsInstanceOf;
import org.hamcrest.core.IsNot;
import org.hamcrest.core.StringContains;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.RepeatedTest;
import org.junit.jupiter.api.Test;

/**
 * Tests for {@link Repository}.
 *
 * @since 0.1
 * @checkstyle ClassDataAbstractionCouplingCheck (500 lines)
 * @checkstyle MagicNumberCheck (500 lines)
 * @checkstyle IllegalCatchCheck (500 lines)
 * @checkstyle ExecutableStatementCountCheck (500 lines)
 */
@SuppressWarnings({"PMD.TooManyMethods", "PMD.AvoidCatchingGenericException"})
class RepositoryTest {

    /**
     * Storage used in tests.
     */
    private BlockingStorage storage;

    /**
     * Repository to test.
     */
    private Repository repository;

    @BeforeEach
    void init() {
        final InMemoryStorage asto = new InMemoryStorage();
        this.storage = new BlockingStorage(asto);
        this.repository = new Repository(asto);
    }

    @Test
    void shouldAddPackage() throws Exception {
        final Key.From source = new Key.From("package.zip");
        this.storage.save(source, this.nupkg().bytes());
        this.repository.add(source);
        final PackageId id = new PackageId("newtonsoft.json");
        final String version = "12.0.3";
        final PackageIdentity identity = new PackageIdentity(id, new Version(version));
        MatcherAssert.assertThat(
            this.storage.value(identity.nupkgKey()),
            Matchers.equalTo(this.nupkg().bytes())
        );
        MatcherAssert.assertThat(
            new String(
                this.storage.value(identity.hashKey())
            ),
            Matchers.equalTo(
                // @checkstyle LineLength (1 lines)
                "aTRmXwR5xYu+mWxE8r8W1DWnL02SeV8LwdQMsLwTWP8OZgrCCyTqvOAe5hRb1VNQYXjln7qr0PKpSyO/pcc19Q=="
            )
        );
        final String nuspec = "newtonsoft.json.nuspec";
        MatcherAssert.assertThat(
            this.storage.value(identity.nuspecKey()),
            Matchers.equalTo(new NewtonJsonResource(nuspec).bytes())
        );
        MatcherAssert.assertThat(
            this.versions(id.versionsKey()),
            Matchers.contains(version)
        );
        MatcherAssert.assertThat(
            this.storage.exists(source),
            new IsEqual<>(false)
        );
    }

    @Test
    void shouldFailToAddInvalidPackage() throws Exception {
        final Key.From source = new Key.From("invalid");
        this.storage.save(source, "not a zip".getBytes());
        Assertions.assertThrows(
            InvalidPackageException.class,
            () -> this.repository.add(source),
            // @checkstyle LineLengthCheck (1 line)
            "Repository expected to throw InvalidPackageException if package is invalid and cannot be added"
        );
    }

    @Test
    void shouldGetPackageVersions() throws Exception {
        final byte[] bytes = "{\"versions\":[\"1.0.0\",\"1.0.1\"]}"
            .getBytes(StandardCharsets.US_ASCII);
        final PackageId foo = new PackageId("Foo");
        this.storage.save(foo.versionsKey(), bytes);
        final Versions versions = this.repository.versions(foo);
        final Key.From bar = new Key.From("bar");
        versions.save(this.storage, bar);
        MatcherAssert.assertThat(
            "Saved versions are not identical to versions initial content",
            this.storage.value(bar),
            new IsEqual<>(bytes)
        );
    }

    @Test
    void shouldGetEmptyPackageVersionsWhenNonePresent() throws Exception {
        final PackageId pack = new PackageId("MyLib");
        final Versions versions = this.repository.versions(pack);
        final Key.From sink = new Key.From("sink");
        versions.save(this.storage, sink);
        MatcherAssert.assertThat(
            "Versions created from scratch expected to be empty",
            this.versions(sink),
            new IsEmptyCollection<>()
        );
    }

    @Test
    void shouldFailToAddPackageWhenItAlreadyExists() throws Exception {
        final Key.From first = new Key.From("first");
        this.storage.save(first, this.nupkg().bytes());
        this.repository.add(first);
        final Key.From second = new Key.From("second");
        this.storage.save(second, this.nupkg().bytes());
        Assertions.assertThrows(
            PackageVersionAlreadyExistsException.class,
            () -> this.repository.add(second)
        );
    }

    @Test
    void shouldReadNuspec() throws Exception {
        final PackageIdentity identity = new PackageIdentity(
            new PackageId("UsefulLib"),
            new Version("2.0")
        );
        this.storage.save(
            identity.nuspecKey(),
            String.join(
                "",
                "<?xml version=\"1.0\"?>",
                "<package xmlns=\"http://schemas.microsoft.com/packaging/2013/05/nuspec.xsd\">",
                "<metadata><id>UsefulLib</id></metadata>",
                "</package>"
            ).getBytes()
        );
        MatcherAssert.assertThat(
            this.repository.nuspec(identity).packageId().lower(),
            new IsEqual<>("usefullib")
        );
    }

    @Test
    void shouldFailToReadNuspecWhenValueAbsent() {
        final PackageIdentity identity = new PackageIdentity(
            new PackageId("MyPack"),
            new Version("1.0")
        );
        Assertions.assertThrows(
            IllegalArgumentException.class,
            () -> this.repository.nuspec(identity)
        );
    }

    @RepeatedTest(10)
    void throwsExceptionWhenPackagesAddedSimultaneously() throws Exception {
        final int count = 3;
        final CountDownLatch latch = new CountDownLatch(count);
        final List<CompletableFuture<Void>> tasks = new ArrayList<>(count);
        for (int number = 0; number < count; number += 1) {
            final CompletableFuture<Void> future = new CompletableFuture<>();
            tasks.add(future);
            new Thread(
                () -> {
                    try {
                        latch.countDown();
                        latch.await();
                        final Key.From source = new Key.From(UUID.randomUUID().toString());
                        this.storage.save(source, this.nupkg().bytes());
                        this.repository.add(source);
                        future.complete(null);
                    } catch (final Exception exception) {
                        future.completeExceptionally(exception);
                    }
                }
            ).start();
        }
        final List<Throwable> failures = tasks.stream().flatMap(
            task -> {
                Stream<Throwable> result;
                try {
                    task.join();
                    result = Stream.empty();
                } catch (final RuntimeException ex) {
                    result = Stream.of(ex.getCause());
                }
                return result;
            }
        ).collect(Collectors.toList());
        MatcherAssert.assertThat(
            "Some updates failed",
            failures,
            new IsNot<>(new IsEmptyCollection<>())
        );
        MatcherAssert.assertThat(
            "All failure due to concurrent lock access or that version already exists",
            failures,
            new Every<>(
                new AnyOf<>(
                    Arrays.asList(
                        new AllOf<>(
                            Arrays.asList(
                                new IsInstanceOf(IllegalStateException.class),
                                new FeatureMatcher<Throwable, String>(
                                    new StringContains("Failed to acquire lock."),
                                    "an exception with message",
                                    "message"
                                ) {
                                    @Override
                                    protected String featureValueOf(final Throwable actual) {
                                        return actual.getMessage();
                                    }
                                }
                            )
                        ),
                        new IsInstanceOf(PackageVersionAlreadyExistsException.class)
                    )
                )
            )
        );
        MatcherAssert.assertThat(
            "Storage has no locks",
            this.storage.list(Key.ROOT).stream().noneMatch(key -> key.string().contains("lock")),
            new IsEqual<>(true)
        );
    }

    private List<String> versions(final Key key) throws Exception {
        final byte[] bytes = this.storage.value(key);
        try (JsonReader reader = Json.createReader(new ByteArrayInputStream(bytes))) {
            return reader.readObject()
                .getJsonArray("versions")
                .getValuesAs(JsonString.class)
                .stream()
                .map(JsonString::getString)
                .collect(Collectors.toList());
        }
    }

    private NewtonJsonResource nupkg() {
        return new NewtonJsonResource("newtonsoft.json.12.0.3.nupkg");
    }
}
