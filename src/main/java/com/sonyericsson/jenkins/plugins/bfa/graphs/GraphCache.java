/*
 * The MIT License
 *
 * Copyright 2013 Sony Mobile Communications AB. All rights reserved.
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */
package com.sonyericsson.jenkins.plugins.bfa.graphs;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import hudson.util.Graph;

/**
 *
 * A class for caching {@link Graph}-objects. Maps a string-key to each {@link Graph}-object.
 *
 * @author Christoffer Lauri &lt;christoffer.lauri@sonymobile.com&gt;
 *
 */
public final class GraphCache {
    private static final int EXPIRATION_TIME_MINUTES = 30;
    /**
     * The {@link GraphCache} instance.
     */
    private static GraphCache instance;
    private Cache<String, Graph> cache;

    /**
     * Private constructor that creates a new cache.
     * Invalidation occurs at least {@link GraphCache#EXPIRATION_TIME_MINUTES} minutes after write.
     */
    private GraphCache() {
        cache = CacheBuilder.newBuilder().expireAfterWrite(EXPIRATION_TIME_MINUTES, TimeUnit.MINUTES).build();
    }

    /**
     * Get the singleton-instance.
     * @return {@link GraphCache}-instance
     */
    public static synchronized GraphCache getInstance() {
        if (instance == null) {
            instance = new GraphCache();
        }
        return instance;
    }

    /**
     * Invalidate the object with the specified key.
     * @param key The key whose Graph to invalidate
     */
    public void invalidate(String key) {
        cache.invalidate(key);
    }

    /**
     * Invalidates all graphs matching argument pattern.
     * @param pattern the pattern to match for graph ids
     */
    public void invalidateMatching(Pattern pattern) {
        for (String key : getMatchingCacheKeys(pattern)) {
            invalidate(key);
        }
    }

    /**
     * Gets all matching graph cache ids/keys.
     * @param pattern the pattern to match for graph ids
     * @return list of matching ids
     */
    public List<String> getMatchingCacheKeys(Pattern pattern) {
        List<String> keys = new ArrayList<String>();
        if (cache != null) {
            Set<String> keySet = cache.asMap().keySet();
            for (String key : keySet) {
                Matcher keyMatcher = pattern.matcher(key);
                if (keyMatcher.matches()) {
                    keys.add(key);
                }
            }
        }
        return keys;
    }

    /**
     * Gets the expiration time for cached objects.
     * @return expiration time in minutes
     */
    public static int getExpirationTime() {
        return EXPIRATION_TIME_MINUTES;
    }

    /**
     * Get the Graph corresponding to the specified key.
     * If there is no cached graph for the specified key
     * (or it has been invalidated) the specified
     * {@code Callable<Graph>} will be called.
     * @param key The key for the graph
     * @param callable A callback to construct the graph that wasn't cached
     * @return A cached graph, or a new graph if there was no cached instance
     * @throws ExecutionException If a graph e.g. cannot be generated
     */
    public Graph get(String key, Callable<Graph> callable) throws ExecutionException {
        return cache.get(key, callable);
    }
}
