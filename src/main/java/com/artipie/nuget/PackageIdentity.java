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

/**
 * Package version identity.
 *
 * @since 0.1
 */
public final class PackageIdentity {

    /**
     * Package identity.
     */
    private final PackageId id;

    /**
     * Package version.
     */
    private final Version version;

    /**
     * Ctor.
     *
     * @param id Package identity.
     * @param version Package version.
     */
    public PackageIdentity(final PackageId id, final Version version) {
        this.id = id;
        this.version = version;
    }

    /**
     * Get key for .nupkg file.
     *
     * @return Key to .nupkg file.
     */
    public Key nupkgKey() {
        return new Key.From(
            this.rootKey(),
            String.format("%s.%s.nupkg", this.id.lower(), this.version.normalized())
        );
    }

    /**
     * Get key for hash file.
     *
     * @return Key to hash file.
     */
    public Key hashKey() {
        return new Key.From(
            this.rootKey(),
            String.format("%s.%s.nupkg.sha512", this.id.lower(), this.version.normalized())
        );
    }

    /**
     * Get key for .nuspec file.
     *
     * @return Key to .nuspec file.
     */
    public Key nuspecKey() {
        return new Key.From(this.rootKey(), String.format("%s.nuspec", this.id.lower()));
    }

    /**
     * Get root key for package.
     *
     * @return Root key.
     */
    public Key rootKey() {
        return new Key.From(this.id.rootKey(), this.version.normalized());
    }

    @Override
    public String toString() {
        return String.format("Package: '%s' Version: '%s'", this.id, this.version);
    }
}
