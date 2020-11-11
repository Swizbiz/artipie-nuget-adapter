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

import com.google.common.hash.Hashing;
import com.google.common.io.ByteSource;
import com.google.common.io.ByteStreams;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

/**
 * Package in .nupkg format.
 *
 * @since 0.1
 */
@SuppressWarnings("PMD.AvoidDuplicateLiterals")
public final class Nupkg implements NuGetPackage {

    /**
     * Binary content of package.
     */
    private final ByteSource content;

    /**
     * Ctor.
     *
     * @param content Binary content of package.
     */
    public Nupkg(final ByteSource content) {
        this.content = content;
    }

    @Override
    @SuppressWarnings("PMD.CyclomaticComplexity")
    public Nuspec nuspec() {
        Nuspec nuspec = null;
        try (ZipInputStream zipStream = new ZipInputStream(this.content.openStream())) {
            while (true) {
                final ZipEntry entry = zipStream.getNextEntry();
                if (entry == null) {
                    break;
                }
                if (entry.getName().endsWith(".nuspec")) {
                    if (nuspec != null) {
                        throw new IllegalArgumentException(
                            "More then one .nuspec file found inside the package."
                        );
                    }
                    nuspec = new Nuspec(ByteSource.wrap(ByteStreams.toByteArray(zipStream)));
                }
            }
        } catch (final IOException ex) {
            throw new UncheckedIOException(ex);
        }
        if (nuspec == null) {
            throw new IllegalArgumentException("No .nuspec file found inside the package.");
        }
        return nuspec;
    }

    @Override
    public Hash hash() {
        try {
            return new Hash(Hashing.sha512().hashBytes(this.content.read()));
        } catch (final IOException ex) {
            throw new UncheckedIOException(ex);
        }
    }
}
