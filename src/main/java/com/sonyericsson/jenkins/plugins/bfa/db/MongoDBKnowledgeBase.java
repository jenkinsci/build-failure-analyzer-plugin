/*
 * The MIT License
 *
 * Copyright 2012 Sony Mobile Communications Inc. All rights reserved.
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

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.type.TypeFactory;
import com.mongodb.BasicDBObject;
import com.mongodb.DBObject;
import com.mongodb.DBRef;
import com.mongodb.MongoClientSettings;
import com.mongodb.MongoCredential;
import com.mongodb.MongoException;
import com.mongodb.ServerAddress;
import com.mongodb.client.FindIterable;
import com.mongodb.client.MongoClient;

import static com.mongodb.client.model.Filters.not;
import static com.mongodb.client.model.Filters.exists;
import static com.mongodb.client.model.Filters.eq;

import com.mongodb.client.MongoClients;
import com.mongodb.client.MongoCursor;
import com.mongodb.client.MongoDatabase;
import com.mongodb.connection.ClusterConnectionMode;
import com.sonyericsson.jenkins.plugins.bfa.Messages;
import com.sonyericsson.jenkins.plugins.bfa.model.FailureCause;
import com.sonyericsson.jenkins.plugins.bfa.model.indication.FoundIndication;
import com.sonyericsson.jenkins.plugins.bfa.statistics.FailureCauseStatistics;
import com.sonyericsson.jenkins.plugins.bfa.statistics.Statistics;
import com.sonyericsson.jenkins.plugins.bfa.utils.BfaUtils;
import hudson.Extension;
import hudson.Util;
import hudson.model.Descriptor;
import hudson.model.Run;
import hudson.util.FormValidation;
import hudson.util.Secret;

import java.util.Calendar;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;
import java.util.SimpleTimeZone;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import jenkins.model.Jenkins;
import org.bson.UuidRepresentation;
import org.bson.conversions.Bson;
import org.bson.types.ObjectId;
import org.jfree.data.time.Day;
import org.jfree.data.time.Hour;
import org.jfree.data.time.Month;
import org.jfree.data.time.TimePeriod;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.DataBoundSetter;
import org.kohsuke.stapler.QueryParameter;
import org.mongojack.JacksonMongoCollection;
import org.mongojack.internal.MongoJackModule;

import java.util.Collection;


/**
 * Handling of the MongoDB way of saving the knowledge base.
 *
 * @author Tomas Westling &lt;tomas.westling@sonyericsson.com&gt;
 */
public class MongoDBKnowledgeBase extends KnowledgeBase {

    private static final long serialVersionUID = 4984133048405390951L;
    /**The name of the cause collection in the database.*/
    public static final String COLLECTION_NAME = "failureCauses";
    /**The name of the statistics collection in the database.*/
    public static final String STATISTICS_COLLECTION_NAME = "statistics";
    private static final int MONGO_DEFAULT_PORT = 27017;
    /**
     * Query to single out documents that doesn't have a "removed" property
     */
    static final BasicDBObject NOT_REMOVED_QUERY = new BasicDBObject("_removed", new BasicDBObject("$exists", false));
    static final Bson NOT_REMOVED_QUERY_FILTER = not(exists("_removed"));
    private static final Logger logger = Logger.getLogger(MongoDBKnowledgeBase.class.getName());
    private static final int CONNECT_TIMEOUT = 5000;
    private static final int SERVER_SELECTION_TIMEOUT = 5000;

    private static final TypeFactory TYPE_FACTORY = TypeFactory.defaultInstance()
            .withClassLoader(MongoDBKnowledgeBase.class.getClassLoader());

    private static final ObjectMapper OBJECT_MAPPER = MongoJackModule
            .configure(new ObjectMapper())
            .setTypeFactory(TYPE_FACTORY);

    private transient MongoClient mongo;
    private transient MongoDatabase db;
    private transient JacksonMongoCollection<FailureCause> jacksonCollection;
    private transient JacksonMongoCollection<DBObject> jacksonStatisticsCollection;
    private transient MongoDBKnowledgeBaseCache cache;

    private String host;
    private int port;
    private String dbName;
    private String userName;
    private Secret password;
    private boolean enableStatistics;
    private boolean successfulLogging;
    private boolean tls;

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
     * Whether to use TLS when connecting to the mongo server.
     * @return the tls option
     */
    public boolean isTls() {
        return tls;
    }

    /**
     * Set whether or not to use TLS when connecting to the mongo server.
     * @param tls the tls option
     */
    @DataBoundSetter
    public void setTls(boolean tls) {
        this.tls = tls;
    }

    /**
     * Standard constructor.
     * @param host the host to connect to.
     * @param port the port to connect to.
     * @param dbName the database name to connect to.
     * @param userName the user name for the database.
     * @param password the password for the database.
     * @param enableStatistics if statistics logging should be enabled or not.
     * @param successfulLogging if all builds should be logged to the statistics DB
     */
    @DataBoundConstructor
    public MongoDBKnowledgeBase(String host, int port, String dbName, String userName, Secret password,
                                boolean enableStatistics, boolean successfulLogging) {
        this.host = host;
        this.port = port;
        this.dbName = dbName;
        this.userName = userName;
        this.password = password;
        this.enableStatistics = enableStatistics;
        this.successfulLogging = successfulLogging;
    }

    @Override
    public synchronized void start() {
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
     */
    private void initCache() {
        if (cache == null) {
            cache = new MongoDBKnowledgeBaseCache(getJacksonCollection());
            cache.start();
        }
    }

    /**
     * @see KnowledgeBase#getCauses()
     * Can throw MongoException if unknown fields exist in the database.
     * @return the full list of causes.
     */
    @Override
    public Collection<FailureCause> getCauses() {
        initCache();
        return cache.getCauses();
    }

    /**
     * @see KnowledgeBase#getCauseNames()
     * Can throw MongoException if unknown fields exist in the database.
     * @return the full list of the names and ids of the causes..
     */
    @Override
    public Collection<FailureCause> getCauseNames() {
        List<FailureCause> list = new LinkedList<FailureCause>();
        DBObject keys = new BasicDBObject();
        keys.put("name", 1);
        final FindIterable<FailureCause> dbCauses = getJacksonCollection().find(NOT_REMOVED_QUERY_FILTER);
        final MongoCursor<FailureCause> iterator = dbCauses.iterator();
        while (iterator.hasNext()) {
            list.add(iterator.next());
        }
        return list;

    }

    @Override
    public Collection<FailureCause> getShallowCauses() {
        List<FailureCause> list = new LinkedList<>();
        DBObject keys = new BasicDBObject();
        keys.put("name", 1);
        keys.put("description", 1);
        keys.put("categories", 1);
        keys.put("comment", 1);
        keys.put("modifications", 1);
        keys.put("lastOccurred", 1);
        BasicDBObject orderBy = new BasicDBObject("name", 1);
        final FindIterable<FailureCause> dbCauses = getJacksonCollection().find(NOT_REMOVED_QUERY);
        dbCauses.sort(orderBy);

        final MongoCursor<FailureCause> iterator = dbCauses.iterator();
        while (iterator.hasNext()) {
            list.add(iterator.next());
        }
        return list;
    }

    @Override
    public FailureCause getCause(String id) {
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
    public FailureCause addCause(FailureCause cause) {
        return addCause(cause, true);
    }

    @Override
    public FailureCause removeCause(String id) {
        BasicDBObject removedInfo = new BasicDBObject("timestamp", new Date());
        removedInfo.put("by", Jenkins.getAuthentication().getName());
        BasicDBObject update = new BasicDBObject("$set", new BasicDBObject("_removed", removedInfo));
        getJacksonCollection().updateById(id, update);
        final FailureCause modifiedFailureCause = getJacksonCollection().findOneById(id);
        initCache();
        cache.updateCache();
        return modifiedFailureCause;
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
     * @see MongoDBKnowledgeBase#addCause(FailureCause)
     */
    public FailureCause addCause(FailureCause cause, boolean doUpdate) {
        getJacksonCollection().insert(cause);
        if (doUpdate) {
            initCache();
            cache.updateCache();
        }

        final FailureCause modifiedFailureCause = getJacksonCollection().find(eq("name", cause.getName())).first();
        return modifiedFailureCause;
    }

    @Override
    public FailureCause saveCause(FailureCause cause) {
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
     * @see MongoDBKnowledgeBase#saveCause(FailureCause)
     */
    public FailureCause saveCause(FailureCause cause, boolean doUpdate) {
        getJacksonCollection().save(cause);
        if (doUpdate) {
            initCache();
            cache.updateCache();
        }
        final FailureCause modifiedFailureCause = getJacksonCollection().find(eq("name", cause.getName())).first();
        return modifiedFailureCause;
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
    public List<String> getCategories() {
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
        List<FailureCause> removed = oldKnowledgeBase.getRemovedCauses();
        for (FailureCause fc : removed) {
            getJacksonCollection().save(fc);
        }
    }

    /**
     * Gets all causes flagged as removed in a "raw" JSON format.
     *
     * @return the list of removed causes.
     * @throws Exception if so.
     */
    protected List<FailureCause> getRemovedCauses() throws Exception {
        BasicDBObject query = new BasicDBObject("_removed", new BasicDBObject("$exists", true));
        FindIterable<FailureCause> causes = getJacksonCollection().find(query);
        List<FailureCause> removed = new LinkedList<FailureCause>();
        while (causes.iterator().hasNext()) {
            removed.add(causes.iterator().next());
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
                    && this.tls == oldMongoDBKnowledgeBase.tls
                    && this.enableStatistics == oldMongoDBKnowledgeBase.enableStatistics
                    && this.successfulLogging == oldMongoDBKnowledgeBase.successfulLogging;
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
    public boolean isEnableStatistics() {
        return enableStatistics;
    }

    @Override
    public boolean isSuccessfulLogging() {
        return successfulLogging;
    }

    @Override
    public void saveStatistics(Statistics stat) {
        DBObject object = new BasicDBObject();
        object.put("projectName", stat.getProjectName());
        object.put("buildNumber", stat.getBuildNumber());
        object.put("displayName", stat.getDisplayName());
        object.put("master", stat.getMaster());
        object.put("slaveHostName", stat.getSlaveHostName());
        object.put("startingTime", stat.getStartingTime());
        object.put("duration", stat.getDuration());
        object.put("timeZoneOffset", stat.getTimeZoneOffset());
        object.put("triggerCauses", stat.getTriggerCauses());
        DBObject cause = null;
        if (stat.getUpstreamCause() != null) {
            cause = new BasicDBObject();
            Statistics.UpstreamCause upstreamCause = stat.getUpstreamCause();
            cause.put("project", upstreamCause.getUpstreamProject());
            cause.put("build", upstreamCause.getUpstreamBuild());
        }
        object.put("upstreamCause", cause);
        object.put("result", stat.getResult());
        List<FailureCauseStatistics> failureCauseStatisticsList = stat.getFailureCauseStatisticsList();
        addFailureCausesToDBObject(object, failureCauseStatisticsList);

        getJacksonStatisticsCollection().insert(object);
       }

    @Override
    public Date getLatestFailureForCause(String id) {
        try {
            BasicDBObject match = new BasicDBObject("failureCauses.failureCause.$id", new ObjectId(id));
            FindIterable<DBObject> output = getJacksonStatisticsCollection()
                    .find(match)
                    .sort(new BasicDBObject("startingTime", -1))
                    .limit(1);

            for (DBObject result : output) {
                Date startingTime = (Date)result.get("startingTime");

                if (startingTime != null) {
                    return startingTime;
                }
            }
        } catch (Exception e) {
            logger.log(Level.WARNING, "Failed getting latest failure of cause", e);
        }

        return null;
    }

    @Override
    public Date getCreationDateForCause(String id) {
        Date creationDate;
        try {
            //Get the creation date using time information in MongoDB id:
            creationDate = new ObjectId(id).getDate();
        } catch (IllegalArgumentException e) {
            logger.log(Level.WARNING, "Could not retrieve original modification", e);
            creationDate = new Date(0);
        }
        return creationDate;
    }

    @Override
    public void updateLastSeen(List<String> ids, Date seen) {
        BasicDBObject set = new BasicDBObject("$set", new BasicDBObject("lastOccurred", seen));
        for (String id : ids) {
            getJacksonCollection().updateById(id, set);
        }
    }

    /**
     * Puts argument value to the dbObject if the value is non-null.
     * @param dbObject object to put value to.
     * @param key the key to map the value to.
     * @param value the value to set.
     */
    private static void putNonNullStringValue(DBObject dbObject, String key, String value) {
        if (value != null) {
            dbObject.put(key, value);
        }
    }

    /**
     * Puts argument value to the dbObject if the value is non-null.
     * The value will be added with an MongoDB operator, for example "$in" or "$gte".
     * @param dbObject object to put value to.
     * @param key the key to map the value to.
     * @param operator the MongoDB operator to add together with the value.
     * @param value the value to set.
     */
    private static void putNonNullBasicDBObject(DBObject dbObject, String key,
            String operator, Object value) {
        if (value != null) {
            dbObject.put(key, new BasicDBObject(operator, value));
        }
    }

    /**
     * Generates a {@link DBObject} used for grouping data into time intervals
     * @param intervalSize the interval size, should be set to Calendar.HOUR_OF_DAY,
     * Calendar.DATE or Calendar.MONTH.
     * @return DBObject to be used for time grouping
     */
    private DBObject generateTimeGrouping(int intervalSize) {
        DBObject timeFields = new BasicDBObject();
        if (intervalSize == Calendar.HOUR_OF_DAY) {
            timeFields.put("hour", new BasicDBObject("$hour", "$startingTime"));
        }
        if (intervalSize == Calendar.HOUR_OF_DAY || intervalSize == Calendar.DATE) {
            timeFields.put("dayOfMonth", new BasicDBObject("$dayOfMonth", "$startingTime"));
        }
        timeFields.put("month", new BasicDBObject("$month", "$startingTime"));
        timeFields.put("year", new BasicDBObject("$year", "$startingTime"));
        return timeFields;
    }

    /**
     * Generates a {@link TimePeriod} based on a MongoDB grouping aggregation result.
     * @param result the result to interpret
     * @param intervalSize the interval size, should be set to Calendar.HOUR_OF_DAY,
     * Calendar.DATE or Calendar.MONTH.
     * @return TimePeriod
     */
    private TimePeriod generateTimePeriodFromResult(DBObject result, int intervalSize) {
        BasicDBObject groupedAttrs = (BasicDBObject)result.get("_id");
        int month = groupedAttrs.getInt("month");
        int year = groupedAttrs.getInt("year");

        Calendar c = Calendar.getInstance();
        c.set(Calendar.YEAR, year);
        c.set(Calendar.MONTH, month - 1);
        // MongoDB timezone is UTC:
        c.setTimeZone(new SimpleTimeZone(0, "UTC"));

        TimePeriod period;
        if (intervalSize == Calendar.HOUR_OF_DAY) {
            int dayOfMonth = groupedAttrs.getInt("dayOfMonth");
            c.set(Calendar.DAY_OF_MONTH, dayOfMonth);
            int hour = groupedAttrs.getInt("hour");
            c.set(Calendar.HOUR_OF_DAY, hour);

            period = new Hour(c.getTime());
        } else if (intervalSize == Calendar.DATE) {
            int dayOfMonth = groupedAttrs.getInt("dayOfMonth");
            c.set(Calendar.DAY_OF_MONTH, dayOfMonth);

            period = new Day(c.getTime());
        } else {
            period = new Month(c.getTime());
        }
        return period;
    }

    @Override
    public void removeBuildfailurecause(Run build) {
        BasicDBObject searchObj = new BasicDBObject();
        searchObj.put("projectName", build.getParent().getFullName());
        searchObj.put("buildNumber", build.getNumber());
        searchObj.put("master", BfaUtils.getMasterName());
        getJacksonStatisticsCollection().getMongoCollection().deleteMany(searchObj);
    }

    /**
     * Adds the FailureCauses from the list to the DBObject.
     * @param object the DBObject to add to.
     * @param failureCauseStatisticsList the list of FailureCauseStatistics to add.
     */
    private void addFailureCausesToDBObject(DBObject object, List<FailureCauseStatistics> failureCauseStatisticsList) {
        if (failureCauseStatisticsList != null && !failureCauseStatisticsList.isEmpty()) {
            List<DBObject> failureCauseStatisticsObjects = new LinkedList<DBObject>();

            for (FailureCauseStatistics failureCauseStatistics : failureCauseStatisticsList) {
                DBObject failureCauseStatisticsObject = new BasicDBObject();
                ObjectId id = new ObjectId(failureCauseStatistics.getId());
                DBRef failureCauseRef = new DBRef(dbName, COLLECTION_NAME, id);
                failureCauseStatisticsObject.put("failureCause", failureCauseRef);
                List<FoundIndication> foundIndicationList = failureCauseStatistics.getIndications();
                addIndicationsToDBObject(failureCauseStatisticsObject, foundIndicationList);
                failureCauseStatisticsObjects.add(failureCauseStatisticsObject);
            }
            object.put("failureCauses", failureCauseStatisticsObjects);
        }
    }

    /**
     * Adds the indications from the list to the DBObject.
     * @param object the DBObject to add to.
     * @param indications the list of indications to add.
     */
    private void addIndicationsToDBObject(DBObject object, List<FoundIndication> indications) {
        if (indications != null && !indications.isEmpty()) {
            List<DBObject> foundIndicationObjects = new LinkedList<DBObject>();
            for (FoundIndication foundIndication : indications) {
                DBObject foundIndicationObject = new BasicDBObject();
                foundIndicationObject.put("pattern", foundIndication.getPattern());
                foundIndicationObject.put("matchingFile", foundIndication.getMatchingFile());
                foundIndicationObject.put("matchingString", foundIndication.getMatchingString());
                foundIndicationObject.put("matchingLine", foundIndication.getMatchingLine());
                foundIndicationObjects.add(foundIndicationObject);
            }
            object.put("indications", foundIndicationObjects);
        }
    }

    @Override
    public Descriptor<KnowledgeBase> getDescriptor() {
        return Jenkins.getInstance().getDescriptorByType(MongoDBKnowledgeBaseDescriptor.class);
    }

    /**
     * Gets the connection to the MongoDB
     * @return the Mongo.
     */
    private MongoClient getMongoConnection() {
        if (mongo == null) {
            StringBuilder connectionStringBuilder = new StringBuilder(host);
            connectionStringBuilder.append(":").append(port);
            MongoClientSettings.Builder builder = MongoClientSettings.builder().applyToClusterSettings(
                    builder1 -> {
                        List<ServerAddress> hostlist = new LinkedList<ServerAddress>();
                        hostlist.add(new ServerAddress(host, port));
                        builder1.hosts(hostlist).
                                serverSelectionTimeout(SERVER_SELECTION_TIMEOUT, TimeUnit.MILLISECONDS).
                                mode(ClusterConnectionMode.SINGLE);
                    }).applyToConnectionPoolSettings(builder15 -> {

                    }).applyToServerSettings(builder12 -> {
            }).applyToSocketSettings(builder13 -> builder13.connectTimeout((CONNECT_TIMEOUT), TimeUnit.MILLISECONDS)).
                    applyToSslSettings(builder14 -> builder14.enabled(tls));

            if (password != null && Util.fixEmpty(password.getPlainText()) != null) {
                char[] pwd = password.getPlainText().toCharArray();
                MongoCredential credential = MongoCredential.createCredential(userName, dbName, pwd);
                builder.credential(credential);
            }
            mongo = MongoClients.create(builder.build());
        }
        return mongo;
    }

    /**
     * Gets the DB.
     * @return The DB.
     */
    private MongoDatabase getDb() {
        if (db == null) {
            db = getMongoConnection().getDatabase(dbName);
        }
        return db;
    }

    /**
     * Gets the JacksonDBCollection for FailureCauses.
     * @return The jackson db collection.
     */
    private synchronized JacksonMongoCollection<FailureCause> getJacksonCollection() {
        if (jacksonCollection == null) {
            jacksonCollection = JacksonMongoCollection.builder().withObjectMapper(OBJECT_MAPPER).build(
                    getMongoConnection(), dbName, COLLECTION_NAME, FailureCause.class, UuidRepresentation.STANDARD);
        }
        return jacksonCollection;
    }

    /**
     * Gets the JacksonDBCollection for Statistics.
     * @return The jackson db collection.
     */
    private synchronized JacksonMongoCollection<DBObject> getJacksonStatisticsCollection() {
        if (jacksonStatisticsCollection == null) {
            jacksonStatisticsCollection = JacksonMongoCollection.builder().withObjectMapper(OBJECT_MAPPER).build(
                    getMongoConnection(),
                    dbName,
                    STATISTICS_COLLECTION_NAME,
                    DBObject.class,
                    UuidRepresentation.STANDARD);

        }
        return jacksonStatisticsCollection;
    }

    /**
     * Descriptor for {@link MongoDBKnowledgeBase}.
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
        public FormValidation doCheckDbName(@QueryParameter("value") String value) {
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
         * @param tls the tls option.
         * @return {@link FormValidation#ok() } if can be done,
         *         {@link FormValidation#error(java.lang.String) } otherwise.
         */
        public FormValidation doTestConnection(
                @QueryParameter("host") final String host,
                @QueryParameter("port") final int port,
                @QueryParameter("dbName") final String dbName,
                @QueryParameter("userName") final String userName,
                @QueryParameter("password") final String password,
                @QueryParameter("tls") final boolean tls) {
            MongoDBKnowledgeBase base = new MongoDBKnowledgeBase(host, port, dbName, userName,
                    Secret.fromString(password), false, false);
            base.setTls(tls);
            try {
                BasicDBObject ping = new BasicDBObject("ping", "1");
                base.getDb().runCommand(ping);
            } catch (Exception e) {
                return FormValidation.error(e, Messages.MongoDBKnowledgeBase_ConnectionError());
            }
            return FormValidation.ok(Messages.MongoDBKnowledgeBase_ConnectionOK());
        }
    }
}
