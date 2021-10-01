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
package com.sonyericsson.jenkins.plugins.bfa.db;

import de.flapdoodle.embed.mongo.Command;
import de.flapdoodle.embed.mongo.MongodExecutable;
import de.flapdoodle.embed.mongo.MongodProcess;
import de.flapdoodle.embed.mongo.MongodStarter;
import de.flapdoodle.embed.mongo.config.ArtifactStoreBuilder;
import de.flapdoodle.embed.mongo.config.DownloadConfigBuilder;
import de.flapdoodle.embed.mongo.config.IMongodConfig;
import de.flapdoodle.embed.mongo.config.MongodConfigBuilder;
import de.flapdoodle.embed.mongo.config.Net;
import de.flapdoodle.embed.mongo.config.RuntimeConfigBuilder;
import de.flapdoodle.embed.mongo.distribution.Version;
import de.flapdoodle.embed.process.config.IRuntimeConfig;
import de.flapdoodle.embed.process.runtime.Network;
import hudson.util.Secret;

import org.junit.After;
import org.junit.Before;

import java.io.IOException;
import java.util.Collections;

import com.mongodb.BasicDBObject;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;

/**
 * A JUnit rule that can be used for running tests with Embedded MongoDB.
 * A knowledgeBase is created during setUp, which is backed up by a
 * real MongoDB instance.
 *
 * @author Fredrik Persson &lt;fredrik6.persson@sonymobile.com&gt;
 *
 */
public class EmbeddedMongoRule extends org.junit.rules.ExternalResource {

    /**
     * @return KnowledgeBase to be used for testing.
     */
    public KnowledgeBase getKnowledgeBase() {
        return knowledgeBase;
    };
    private KnowledgeBase knowledgeBase;

    private static final String LOCALHOST = "127.0.0.1";
    private String dbName;
    private String username;
    private String password;

    private static String mongoURL = System.getProperty(EmbeddedMongoRule.class.getName() + ".mongoURL");

    private MongodExecutable mongodExe = null;
    private MongodProcess mongodProc = null;
    /**
     * @return Port used by embedded mongo db
     */
    public int getPort() {
        return mongoPort;
    };
    private int mongoPort = 0;

    /**
     * Use an embedded mongo db with a random port and jenkinsbfa as the db name.
     */
    public EmbeddedMongoRule() {
        this(0, "jenkinsbfa", null, null);
    }

    /**
     * Use an embedded mongo db with the given settings.
     * @param port desired port of the mongodb, use 0 for a random port
     * @param dbName name of the database to use when creating the user
     * @param username a username to create in the db
     * @param password the password to use for the new user
     */
    public EmbeddedMongoRule(int port, String dbName, String username, String password) {
        mongoPort = port;
        this.dbName = dbName;
        this.username = username;
        this.password = password;
    }

    /**
     * Sets up an instance of {@link MongoDBKnowledgeBase} backed up by a real MongoDB, to be used for testing.
     * @throws IOException if something goes wrong
     */
    @Before
    public void before() throws IOException {
        MongodStarter runtime;
        if (mongoURL != null) {
            // Use separate URL for fetching mongoDB artifacts.
            Command command = Command.MongoD;
            de.flapdoodle.embed.process.config.store.DownloadConfigBuilder downloadConf = new DownloadConfigBuilder()
                    .defaultsForCommand(command).downloadPath(mongoURL);
            de.flapdoodle.embed.process.store.ArtifactStoreBuilder artifactStoreBuilder = new ArtifactStoreBuilder()
                    .defaults(command).download(downloadConf);

            IRuntimeConfig runtimeConfig = new RuntimeConfigBuilder()
                    .defaults(command)
                    .artifactStore(artifactStoreBuilder)
                    .build();

            runtime = MongodStarter.getInstance(runtimeConfig);
        } else {
            runtime = MongodStarter.getDefaultInstance();
        }

        MongodConfigBuilder confBuilder = new MongodConfigBuilder()
            .version(Version.Main.V3_4);

        if (mongoPort != 0) {
            confBuilder = confBuilder.net(new Net(mongoPort, Network.localhostIsIPv6()));
        }

        IMongodConfig conf = confBuilder.build();

        mongodExe = runtime.prepare(conf);
        mongodProc = mongodExe.start();
        mongoPort = conf.net().getPort();

        // even if authentication isn't enabled, connecting with a username requires it to exist
        if (username != null && password != null) {
            try (MongoClient client = MongoClients.create(String.format("mongodb://localhost:%d", mongoPort))) {
                client
                    .getDatabase(dbName)
                    .runCommand(
                        new BasicDBObject("createUser", username)
                        .append("pwd", password)
                        .append("roles",
                            Collections.singletonList(new BasicDBObject("role", "dbOwner").append("db", dbName))));
            }
        }

        Secret encryptedPassword = null;
        if (password != null) {
            encryptedPassword = Secret.fromString(password);
        }
        knowledgeBase = new MongoDBKnowledgeBase(LOCALHOST, mongoPort, dbName, username, encryptedPassword, true, false);
    }

    /**
     * Tears down the test environment.
     */
    @After
    public void after() {
        if (this.mongodProc != null) {
            this.mongodProc.stop();
            this.mongodExe.stop();
        }
    }
}
