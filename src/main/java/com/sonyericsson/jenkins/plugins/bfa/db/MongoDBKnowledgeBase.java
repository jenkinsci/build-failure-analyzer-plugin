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

import com.mongodb.BasicDBObject;
import com.mongodb.DB;
import com.mongodb.DBCollection;
import com.mongodb.DBObject;
import com.mongodb.Mongo;
import com.mongodb.MongoException;
import com.sonyericsson.jenkins.plugins.bfa.Messages;
import com.sonyericsson.jenkins.plugins.bfa.model.FailureCause;
import hudson.Extension;
import hudson.Util;
import hudson.model.Descriptor;
import hudson.util.FormValidation;
import hudson.util.Secret;
import jenkins.model.Jenkins;
import net.vz.mongodb.jackson.DBCursor;
import net.vz.mongodb.jackson.JacksonDBCollection;
import net.vz.mongodb.jackson.WriteResult;
import org.bson.types.ObjectId;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.QueryParameter;

import javax.naming.AuthenticationException;
import java.net.UnknownHostException;
import java.util.Collection;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Handling of the MongoDB way of saving the knowledge base.
 *
 * @author Tomas Westling &lt;tomas.westling@sonyericsson.com&gt;
 */
public class MongoDBKnowledgeBase extends KnowledgeBase {

    private static final long serialVersionUID = 4984133048405390951L;
    /**The name of the cause collection in the database.*/
    public static final String COLLECTION_NAME = "failureCauses";
    private static final int MONGO_DEFAULT_PORT = 27017;
    /**
     * Query to single out documents that doesn't have a "removed" property
     */
    static final BasicDBObject NOT_REMOVED_QUERY = new BasicDBObject("_removed", new BasicDBObject("$exists", false));
    private static final Logger logger = Logger.getLogger(MongoDBKnowledgeBase.class.getName());

    private transient Mongo mongo;
    private transient DB db;
    private transient DBCollection collection;
    private transient JacksonDBCollection<FailureCause, String> jacksonCollection;
    private transient MongoDBKnowledgeBaseCache cache;

    private String host;
    private int port;
    private String dbName;
    private String userName;
    private Secret password;
    private boolean enableStatistics;

    /**
     * Getter for the MongoDB user name.
     * @return the user name.
     */
    public String getUserName() {
        return userName;
    }

    /**
     * Getter for the MongoDB password.
     * @return the password.
     */
    public Secret getPassword() {
        return password;
    }

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
     * @param userName the user name for the database.
     * @param password the password for the database.
     * @param enableStatistics if statistics logging should be enabled or not.
     */
    @DataBoundConstructor
    public MongoDBKnowledgeBase(String host, int port, String dbName, String userName, Secret password,
                                boolean enableStatistics) {
        this.host = host;
        this.port = port;
        this.dbName = dbName;
        this.userName = userName;
        this.password = password;
        this.enableStatistics = enableStatistics;
    }

    @Override
    public synchronized void start() throws UnknownHostException, AuthenticationException {
        initCache();
    }

    @Override
    public synchronized void stop() {
        if (cache != null) {
            cache.stop();
            cache = null;
        }
    }

    /**
     * Initializes the cache if it is null.
     * @throws UnknownHostException if we cannot connect to the database.
     * @throws AuthenticationException if we cannot authenticate towards the database.
     */
    private void initCache() throws UnknownHostException, AuthenticationException {
        if (cache == null) {
            cache = new MongoDBKnowledgeBaseCache(getJacksonCollection());
            cache.start();
        }
    }

    /**
     * @see KnowledgeBase#getCauses()
     * Can throw MongoException if unknown fields exist in the database.
     * @return the full list of causes.
     * @throws UnknownHostException if a connection to the host cannot be made.
     * @throws AuthenticationException if we cannot authenticate towards the database.
     */
    @Override
    public Collection<FailureCause> getCauses() throws UnknownHostException, AuthenticationException {
        initCache();
        return cache.getCauses();
    }

    /**
     * @see KnowledgeBase#getCauseNames()
     * Can throw MongoException if unknown fields exist in the database.
     * @return the full list of the names and ids of the causes..
     * @throws UnknownHostException if a connection to the host cannot be made.
     * @throws AuthenticationException if we cannot authenticate towards the database.
     */
    @Override
    public Collection<FailureCause> getCauseNames() throws UnknownHostException, AuthenticationException {
        List<FailureCause> list = new LinkedList<FailureCause>();
        DBObject keys = new BasicDBObject();
        keys.put("name", 1);
        DBCursor<FailureCause> dbCauses =  getJacksonCollection().find(NOT_REMOVED_QUERY, keys);
        while (dbCauses.hasNext()) {
            list.add(dbCauses.next());
        }
        return list;

    }

    @Override
    public Collection<FailureCause> getShallowCauses() throws Exception {
        List<FailureCause> list = new LinkedList<FailureCause>();
        DBObject keys = new BasicDBObject();
        keys.put("name", 1);
        keys.put("description", 1);
        keys.put("categories", 1);
        BasicDBObject orderBy = new BasicDBObject("name", 1);
        DBCursor<FailureCause> dbCauses =  getJacksonCollection().find(NOT_REMOVED_QUERY, keys);
        dbCauses = dbCauses.sort(orderBy);
        while (dbCauses.hasNext()) {
            list.add(dbCauses.next());
        }
        return list;
    }

    @Override
    public FailureCause getCause(String id) throws UnknownHostException, AuthenticationException {
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
    public FailureCause addCause(FailureCause cause) throws UnknownHostException, AuthenticationException {
        return addCause(cause, true);
    }

    @Override
    public FailureCause removeCause(String id) throws Exception {
        BasicDBObject idq = new BasicDBObject("_id", new ObjectId(id));
        BasicDBObject removedInfo = new BasicDBObject("timestamp", new Date());
        removedInfo.put("by", Jenkins.getAuthentication().getName());
        BasicDBObject update = new BasicDBObject("$set", new BasicDBObject("_removed", removedInfo));
        return getJacksonCollection().findAndModify(idq, null, null, false, update, true, false);
    }

    /**
     * Does not update the cache, used when we know we will have a lot of save/add calls all at once,
     * e.g. during a convert.
     *
     * @param cause the FailureCause to add.
     * @param doUpdate true if a cache update should be made, false if not.
     *
     * @return the added FailureCause.
     *
     * @throws UnknownHostException If a connection to the Mongo database cannot be made.
     * @throws javax.naming.AuthenticationException if we cannot authenticate towards the database.
     *
     * @see MongoDBKnowledgeBase#addCause(FailureCause)
     */
    public FailureCause addCause(FailureCause cause, boolean doUpdate) throws UnknownHostException,
            AuthenticationException {
        WriteResult<FailureCause, String> result = getJacksonCollection().insert(cause);
        if (doUpdate) {
            initCache();
            cache.updateCache();
        }
        return result.getSavedObject();
    }

    @Override
    public FailureCause saveCause(FailureCause cause) throws UnknownHostException, AuthenticationException {
        return saveCause(cause, true);
    }

    /**
     * Does not update the cache, used when we know we will have a lot of save/add calls all at once,
     * e.g. during a convert.
     *
     * @param cause the FailureCause to save.
     * @param doUpdate true if a cache update should be made, false if not.
     *
     * @return the saved FailureCause.
     *
     * @throws UnknownHostException If a connection to the Mongo database cannot be made.
     * @throws AuthenticationException if we cannot authenticate towards the database.
     *
     * @see MongoDBKnowledgeBase#saveCause(FailureCause)
     */
    public FailureCause saveCause(FailureCause cause, boolean doUpdate) throws UnknownHostException,
            AuthenticationException {
        WriteResult<FailureCause, String> result =  getJacksonCollection().save(cause);
        if (doUpdate) {
            initCache();
            cache.updateCache();
        }
        return result.getSavedObject();
    }

    @Override
    public void convertFrom(KnowledgeBase oldKnowledgeBase) throws Exception {
        if (oldKnowledgeBase instanceof MongoDBKnowledgeBase) {
            convertFromAbstract(oldKnowledgeBase);
            convertRemoved((MongoDBKnowledgeBase)oldKnowledgeBase);
        } else {
            for (FailureCause cause : oldKnowledgeBase.getCauseNames()) {
                try {
                    //try finding the id in the knowledgebase, if so, update it.
                    if (getCause(cause.getId()) != null) {
                        //doing all the additions to the database first and then fetching to the cache only once.
                        saveCause(cause, false);
                    //if not found, add a new.
                    } else {
                        cause.setId(null);
                        addCause(cause, false);
                    }
                  //Safety net for the case that Mongo should throw anything if the id has a really weird form.
                } catch (MongoException e) {
                    cause.setId(null);
                    addCause(cause, false);
                }
            }
            initCache();
            cache.updateCache();
        }
    }

    @Override
    public List<String> getCategories() throws UnknownHostException, AuthenticationException {
        initCache();
        return cache.getCategories();
    }

    /**
     * Copies all causes flagged as removed from the old database to this one.
     *
     * @param oldKnowledgeBase the old database.
     * @throws Exception if something goes wrong.
     */
    protected void convertRemoved(MongoDBKnowledgeBase oldKnowledgeBase) throws Exception {
        List<DBObject> removed = oldKnowledgeBase.getRemovedCauses();
        DBCollection dbCollection = getJacksonCollection().getDbCollection();
        for (DBObject obj : removed) {
            dbCollection.save(obj);
        }
    }

    /**
     * Gets all causes flagged as removed in a "raw" JSON format.
     *
     * @return the list of removed causes.
     * @throws Exception if so.
     */
    protected List<DBObject> getRemovedCauses() throws Exception {
        BasicDBObject query = new BasicDBObject("_removed", new BasicDBObject("$exists", true));
        com.mongodb.DBCursor dbCursor = getJacksonCollection().getDbCollection().find(query);
        List<DBObject> removed = new LinkedList<DBObject>();
        while (dbCursor.hasNext()) {
            removed.add(dbCursor.next());
        }
        return removed;
    }

    @Override
    public boolean equals(KnowledgeBase oldKnowledgeBase) {
        if (getClass().isInstance(oldKnowledgeBase)) {
            MongoDBKnowledgeBase oldMongoDBKnowledgeBase = (MongoDBKnowledgeBase)oldKnowledgeBase;
            return equals(oldMongoDBKnowledgeBase.getHost(), host)
                    && oldMongoDBKnowledgeBase.getPort() == port
                    && equals(oldMongoDBKnowledgeBase.getDbName(), dbName)
                    && equals(oldMongoDBKnowledgeBase.getUserName(), userName)
                    && equals(oldMongoDBKnowledgeBase.getPassword(), password)
                    && this.enableStatistics == oldMongoDBKnowledgeBase.enableStatistics;
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

    /**
     * Checks if two objects equal each other, both being null counts as being equal.
     * @param firstObject the firstObject.
     * @param secondObject the secondObject.
     * @return true if equal or both null, false otherwise.
     */
    public static boolean equals(Object firstObject, Object secondObject) {
        if (firstObject == null) {
            if (secondObject == null) {
                return true;
            }
            return false;
        }
        if (secondObject == null) {
            return false;
        }
        return secondObject.equals(firstObject);
    }

    @Override
    public int hashCode() {
        //Making checkstyle happy.
        return getClass().getName().hashCode();
    }

    @Override
    public boolean isStatisticsEnabled() {
        return enableStatistics;
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
     * @throws AuthenticationException if we cannot authenticate towards the database.
     */
    private DB getDb() throws UnknownHostException, AuthenticationException {
        if (db == null) {
            db = getMongoConnection().getDB(dbName);
        }
        if (Util.fixEmpty(userName) != null && Util.fixEmpty(Secret.toString(password)) != null) {
            char[] pwd = password.getPlainText().toCharArray();
            if (!db.authenticate(userName, pwd)) {
                throw new AuthenticationException("Could not athenticate with the mongo database");
            }
        }
        return db;
    }

    /**
     * Gets the DBCollection.
     * @return The db collection.
     * @throws UnknownHostException if the host cannot be found.
     * @throws AuthenticationException if we cannot authenticate towards the database.
     */
    private DBCollection getCollection() throws UnknownHostException, AuthenticationException {
        if (collection == null) {
            collection = getDb().getCollection(COLLECTION_NAME);
        }
        return collection;
    }

    /**
     * Gets the JacksonDBCollection.
     * @return The jackson db collection.
     * @throws UnknownHostException if the host cannot be found.
     * @throws AuthenticationException if we cannot authenticate towards the database.
     */
    private synchronized JacksonDBCollection<FailureCause, String> getJacksonCollection()
            throws UnknownHostException, AuthenticationException {
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

        /**
         * Convenience method for jelly.
         * @return the default port.
         */
        public int getDefaultPort() {
            return MONGO_DEFAULT_PORT;
        }

        /**
         * Checks that the host name is not empty.
         *
         * @param value the pattern to check.
         * @return {@link hudson.util.FormValidation#ok()} if everything is well.
         */
        public FormValidation doCheckHost(@QueryParameter("value") final String value) {
            if (Util.fixEmpty(value) == null) {
                return FormValidation.error("Please provide a host name!");
            } else {
                Matcher m = Pattern.compile("\\s").matcher(value);
                if (m.find()) {
                    return FormValidation.error("Host name contains white space!");
                }
                return FormValidation.ok();
            }
        }

        /**
         * Checks that the port number is not empty and is a number.
         *
         * @param value the port number to check.
         * @return {@link hudson.util.FormValidation#ok()} if everything is well.
         */
        public FormValidation doCheckPort(@QueryParameter("value") String value) {
            try {
                Long.parseLong(value);
                return FormValidation.ok();
            } catch (NumberFormatException e) {
                return FormValidation.error("Please provide a port number!");
            }
        }

        /**
         * Checks that the database name is not empty.
         *
         * @param value the database name to check.
         * @return {@link hudson.util.FormValidation#ok()} if everything is well.
         */
        public FormValidation doCheckDBName(@QueryParameter("value") String value) {
            if (value == null || value.isEmpty()) {
                return FormValidation.error("Please provide a database name!");
            } else {
                Matcher m = Pattern.compile("\\s").matcher(value);
                if (m.find()) {
                    return FormValidation.error("Database name contains white space!");
                }
                return FormValidation.ok();
            }
        }

        /**
         * Tests if the provided parameters can connect to the Mongo database.
         * @param host the host name.
         * @param port the port.
         * @param dbName the database name.
         * @param userName the user name.
         * @param password the password.
         * @return {@link FormValidation#ok() } if can be done,
         *         {@link FormValidation#error(java.lang.String) } otherwise.
         */
        public FormValidation doTestConnection(
                @QueryParameter("host") final String host,
                @QueryParameter("port") final int port,
                @QueryParameter("dbName") final String dbName,
                @QueryParameter("userName") final String userName,
                @QueryParameter("password") final String password) {
            MongoDBKnowledgeBase base = new MongoDBKnowledgeBase(host, port, dbName, userName,
                    Secret.fromString(password), false);
            try {
                base.getCollection();
            } catch (Exception e) {
                return FormValidation.error(e, Messages.MongoDBKnowledgeBase_ConnectionError());
            }
            return FormValidation.ok(Messages.MongoDBKnowledgeBase_ConnectionOK());
        }
    }
}
