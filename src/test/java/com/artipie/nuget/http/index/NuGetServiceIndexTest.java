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
package com.artipie.nuget.http.index;

import com.artipie.asto.memory.InMemoryStorage;
import com.artipie.http.Response;
import com.artipie.http.hm.RsHasBody;
import com.artipie.http.hm.RsHasStatus;
import com.artipie.http.rq.RequestLine;
import com.artipie.http.rq.RqMethod;
import com.artipie.http.rs.RsStatus;
import com.artipie.nuget.Repository;
import com.artipie.nuget.http.NuGet;
import io.reactivex.Flowable;
import java.io.ByteArrayInputStream;
import java.net.URL;
import java.util.Arrays;
import java.util.Collections;
import javax.json.Json;
import javax.json.JsonObject;
import javax.json.JsonReader;
import org.hamcrest.BaseMatcher;
import org.hamcrest.Description;
import org.hamcrest.Matcher;
import org.hamcrest.MatcherAssert;
import org.hamcrest.TypeSafeMatcher;
import org.hamcrest.core.AllOf;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import wtf.g4s8.hamcrest.json.JsonContains;
import wtf.g4s8.hamcrest.json.JsonHas;
import wtf.g4s8.hamcrest.json.JsonValueIs;

/**
 * Tests for {@link NuGet}.
 * Service index resource.
 *
 * @since 0.1
 * @checkstyle ClassDataAbstractionCouplingCheck (500 lines)
 */
@SuppressWarnings("PMD.AvoidDuplicateLiterals")
class NuGetServiceIndexTest {

    /**
     * Base URL for services.
     */
    private URL url;

    /**
     * Tested NuGet slice.
     */
    private NuGet nuget;

    @BeforeEach
    void init() throws Exception {
        this.url = new URL("http://localhost:4321/repo");
        this.nuget = new NuGet(this.url, new Repository(new InMemoryStorage()));
    }

    @Test
    void shouldGetIndex() {
        final Response response = this.nuget.response(
            new RequestLine(RqMethod.GET, "/index.json").toString(),
            Collections.emptyList(),
            Flowable.empty()
        );
        MatcherAssert.assertThat(
            response,
            new AllOf<>(
                Arrays.asList(
                    new RsHasStatus(RsStatus.OK),
                    new RsHasBody(
                        new IsJson(
                            new AllOf<>(
                                Arrays.asList(
                                    new JsonHas("version", new JsonValueIs("3.0.0")),
                                    new JsonHas(
                                        "resources",
                                        new JsonContains(
                                            new IsService(
                                                "PackagePublish/2.0.0",
                                                String.format("%s/package", this.url)
                                            ),
                                            new IsService(
                                                "RegistrationsBaseUrl/Versioned",
                                                String.format("%s/registrations", this.url)
                                            ),
                                            new IsService(
                                                "PackageBaseAddress/3.0.0",
                                                String.format("%s/content", this.url)
                                            )
                                        )
                                    )
                                )
                            )
                        )
                    )
                )
            )
        );
    }

    @Test
    void shouldFailPutIndex() {
        final Response response = this.nuget.response(
            new RequestLine(RqMethod.PUT, "/index.json").toString(),
            Collections.emptyList(),
            Flowable.empty()
        );
        MatcherAssert.assertThat(response, new RsHasStatus(RsStatus.METHOD_NOT_ALLOWED));
    }

    /**
     * Matcher for bytes array representing JSON.
     *
     * @since 0.1
     */
    private class IsJson extends TypeSafeMatcher<byte[]> {

        /**
         * Matcher for JSON.
         */
        private final Matcher<? extends JsonObject> json;

        IsJson(final Matcher<? extends JsonObject> json) {
            this.json = json;
        }

        @Override
        public void describeTo(final Description description) {
            description.appendText("JSON ").appendDescriptionOf(this.json);
        }

        @Override
        public boolean matchesSafely(final byte[] bytes) {
            final JsonObject root;
            try (JsonReader reader = Json.createReader(new ByteArrayInputStream(bytes))) {
                root = reader.readObject();
            }
            return this.json.matches(root);
        }
    }

    /**
     * Matcher for JSON object representing service.
     *
     * @since 0.1
     */
    private class IsService extends BaseMatcher<JsonObject> {

        /**
         * Expected service type.
         */
        private final String type;

        /**
         * Expected service id.
         */
        private final String id;

        IsService(final String type, final String id) {
            this.type = type;
            this.id = id;
        }

        @Override
        public void describeTo(final Description description) {
            this.delegate().describeTo(description);
        }

        @Override
        public boolean matches(final Object item) {
            return this.delegate().matches(item);
        }

        private Matcher<JsonObject> delegate() {
            return new AllOf<>(
                Arrays.asList(
                    new JsonHas("@type", new JsonValueIs(this.type)),
                    new JsonHas("@id", new JsonValueIs(this.id))
                )
            );
        }
    }
}
