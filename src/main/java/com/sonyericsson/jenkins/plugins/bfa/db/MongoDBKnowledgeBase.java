/*
 * The MIT License
 *
 * Copyright 2012 Sony Mobile Communications AB. All rights reserved.
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

import com.mongodb.DB;
import com.mongodb.DBCollection;
import com.mongodb.Mongo;
import com.sonyericsson.jenkins.plugins.bfa.Messages;
import com.sonyericsson.jenkins.plugins.bfa.model.FailureCause;
import hudson.Extension;
import hudson.model.Descriptor;
import jenkins.model.Jenkins;
import net.vz.mongodb.jackson.DBCursor;
import net.vz.mongodb.jackson.JacksonDBCollection;
import net.vz.mongodb.jackson.WriteResult;
import org.kohsuke.stapler.DataBoundConstructor;

import java.net.UnknownHostException;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import java.util.logging.Logger;

/**
 * Handling of the MongoDB way of saving the knowledge base.
 *
 * @author Tomas Westling &lt;tomas.westling@sonyericsson.com&gt;
 */
public class MongoDBKnowledgeBase extends KnowledgeBase {
    private Mongo mongo;
    private DB db;
    private DBCollection collection;
    private JacksonDBCollection<FailureCause, String> jacksonCollection;
    private String host;
    private int port;
    private String dbName;
    /**The name of the cause collection in the database.*/
    public static final String COLLECTION_NAME = "failureCauses";

    private static final Logger logger = Logger.getLogger(MongoDBKnowledgeBase.class.getName());

   /**
     * Getter for the host value.
     * @return the host string.
     */
    public String getHost() {
        return host;
    }

    /**
     * Getter for the port value.
     * @return the port number.
     */
    public int getPort() {
        return port;
    }

    /**
     * Getter for the database name value.
     * @return the database name string.
     */
    public String getDbName() {
        return dbName;
    }

    /**
     * Standard constructor.
     * @param host the host to connect to.
     * @param port the port to connect to.
     * @param dbName the database name to connect to.
     */
    @DataBoundConstructor
    public MongoDBKnowledgeBase(String host, int port, String dbName) {
        this.host = host;
        this.port = port;
        this.dbName = dbName;
    }

    /**
     * @see KnowledgeBase#getCauses()
     * Can throw MongoException if unknown fields exist in the database.
     * @return the full list of causes.
     * @throws UnknownHostException if a connection to the host cannot be made.
     */
    @Override
    public Collection<FailureCause> getCauses() throws UnknownHostException {
        List<FailureCause> list = new LinkedList<FailureCause>();
        DBCursor<FailureCause> dbCauses =  getJacksonCollection().find();
        while (dbCauses.hasNext()) {
            list.add(dbCauses.next());
        }
        return list;
    }

    /**
     * @see KnowledgeBase#getCauseNames()
     * Can throw MongoException if unknown fields exist in the database.
     * @return the full list of the names and ids of the causes..
     * @throws UnknownHostException if a connection to the host cannot be made.
     */
    @Override
    public Collection<FailureCause> getCauseNames() throws UnknownHostException {
        return getCauses();
    }

    @Override
    public FailureCause getCause(String id) throws UnknownHostException {
        FailureCause returnCase = null;
        try {
            returnCase = getJacksonCollection().findOneById(id);
        } catch (IllegalArgumentException e) {
         logger.fine("Could not find the id, returning null for id: " + id);
            return returnCase;
        }
        return returnCase;
    }

    @Override
    public FailureCause addCause(FailureCause cause) throws UnknownHostException {
        WriteResult<FailureCause, String> result = getJacksonCollection().insert(cause);
        return result.getSavedObject();
    }

    @Override
    public FailureCause saveCause(FailureCause cause) throws UnknownHostException {
        WriteResult<FailureCause, String> result =  getJacksonCollection().save(cause);
        return result.getSavedObject();
    }

    @Override
    public void convertFrom(KnowledgeBase oldKnowledgeBase) throws Exception {
        convertFromAbstract(oldKnowledgeBase);
    }

    @Override
    public boolean equals(KnowledgeBase oldKnowledgeBase) {
        if (getClass().isInstance(oldKnowledgeBase)) {
            MongoDBKnowledgeBase oldMongoDBKnowledgeBase = (MongoDBKnowledgeBase)oldKnowledgeBase;
            return oldMongoDBKnowledgeBase.getHost().equals(host)
                    && oldMongoDBKnowledgeBase.getPort() == port
                    && oldMongoDBKnowledgeBase.getDbName().equals(dbName);
        } else {
            return false;
        }
    }

    @Override
    public boolean equals(Object other) {
        if (other instanceof KnowledgeBase) {
            return this.equals((KnowledgeBase)other);
        } else {
            return false;
        }
    }

    @Override
    public int hashCode() {
        //Making checkstyle happy.
        return getClass().getName().hashCode();
    }

    @Override
    public Descriptor<KnowledgeBase> getDescriptor() {
        return Jenkins.getInstance().getDescriptorByType(MongoDBKnowledgeBaseDescriptor.class);
    }

    /**
     * Gets the connection to the MongoDB
     * @return the Mongo.
     * @throws UnknownHostException if the host cannot be found.
     */
    private Mongo getMongoConnection() throws UnknownHostException {
        if (mongo == null) {
            mongo = new Mongo(host, port);
        }
        return mongo;
    }

    /**
     * Gets the DB.
     * @return The DB.
     * @throws UnknownHostException if the host cannot be found.
     */
    private DB getDb() throws UnknownHostException {
        if (db == null) {
            db = getMongoConnection().getDB(dbName);
        }
        return db;
    }

    /**
     * Gets the DBCollection.
     * @return The db collection.
     * @throws UnknownHostException if the host cannot be found.
     */
    private DBCollection getCollection() throws UnknownHostException {
        if (collection == null) {
            collection = getDb().getCollection(COLLECTION_NAME);
        }
        return collection;
    }

    /**
     * Gets the JacksonDBCollection.
     * @return The jackson db collection.
     * @throws UnknownHostException if the host cannot be found.
     */
    private synchronized JacksonDBCollection<FailureCause, String> getJacksonCollection()
            throws UnknownHostException {
        if (jacksonCollection == null) {
            if (collection == null) {
                collection = getCollection();
            }
            jacksonCollection = JacksonDBCollection.wrap(collection, FailureCause.class, String.class);
        }
        return jacksonCollection;
    }

    /**
     * Descriptor for {@link LocalFileKnowledgeBase}.
     */
    @Extension
    public static class MongoDBKnowledgeBaseDescriptor extends KnowledgeBaseDescriptor {

        @Override
        public String getDisplayName() {
            return Messages.MongoDBKnowledgeBase_DisplayName();
        }
    }
}
