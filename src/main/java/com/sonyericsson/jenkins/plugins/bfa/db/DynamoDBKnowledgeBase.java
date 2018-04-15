package com.sonyericsson.jenkins.plugins.bfa.db;

import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBMapper;
import com.amazonaws.services.dynamodbv2.model.*;
import com.amazonaws.services.dynamodbv2.util.TableUtils;
import com.sonyericsson.jenkins.plugins.bfa.model.FailureCause;
import com.sonyericsson.jenkins.plugins.bfa.statistics.Statistics;
import hudson.model.Descriptor;
import com.amazonaws.AmazonClientException;
import com.amazonaws.auth.profile.ProfileCredentialsProvider;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDB;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDBClientBuilder;
import org.kohsuke.stapler.DataBoundConstructor;


import java.util.*;

public class DynamoDBKnowledgeBase extends KnowledgeBase {

    private static AmazonDynamoDB dynamoDB;
    private static DynamoDBMapper dbMapper;
    private String host;
    private int port;
    private String tableName;

    @DataBoundConstructor
    public DynamoDBKnowledgeBase(String host, int port, String tableName) {
        this.host = host;
        this.port = port;
        this.tableName = tableName;
    }

    /**
     * Get the list of {@link FailureCause}s. It is intended to be used in the scanning phase hence it should be
     * returned as quickly as possible, so the list could be cached.
     *
     * @return the full list of causes.
     * @throws Exception if something in the KnowledgeBase handling goes wrong.
     */
    @Override
    public Collection<FailureCause> getCauses() throws Exception {
        return null;
    }

    /**
     * Get the list of the {@link FailureCause}'s names and ids. The list should be the latest possible from the DB as
     * they will be used for editing. The objects returned should contain at least the id and the name of the cause.
     *
     * @return the full list of the names and ids of the causes.
     * @throws Exception if something in the KnowledgeBase handling goes wrong.
     */
    @Override
    public Collection<FailureCause> getCauseNames() throws Exception {
        return null;
    }

    /**
     * Get a shallow list of the {@link FailureCause}s. The list should be the latest possible from the DB as
     * they will be used in the list of causes to edit.
     * shallow meaning no indications but information enough to show a nice list; at least id and name but description,
     * comment, lastOccurred and categories are preferred as well.
     *
     * @return a shallow list of all causes.
     * @throws Exception if something in the KnowledgeBase handling goes wrong.
     * @see #getCauseNames()
     */
    @Override
    public Collection<FailureCause> getShallowCauses() throws Exception {
        return null;
    }

    /**
     * Get the cause with the given id. The cause returned is intended to be edited right away, so it should be as fresh
     * from the db as possible.
     *
     * @param id the id of the cause.
     * @return the cause or null if a cause with that id could not be found.
     * @throws Exception if something in the KnowledgeBase handling goes wrong.
     */
    @Override
    public FailureCause getCause(String id) throws Exception {
        try {
            return getDbMapper().load(FailureCause.class, id);
        } catch (Exception e) {
            throw e;
        }
    }

    /**
     * Saves a new cause to the db and generates a new id for the cause.
     *
     * @param cause the cause to add.
     * @return the same cause but with a new id.
     * @throws Exception if something in the KnowledgeBase handling goes wrong.
     */
    @Override
    public FailureCause addCause(FailureCause cause) throws Exception {
        return saveCause(cause);
    }

    /**
     * Removes the cause from the knowledge base.
     *
     * @param id the id of the cause to remove.
     * @return the removed FailureCause.
     * @throws Exception if so.
     */
    @Override
    public FailureCause removeCause(String id) throws Exception {
        return null;
    }

    /**
     * Saves a cause to the db. Assumes that the id is kept from when it was fetched. Can also be an existing cause in
     * another {@link KnowledgeBase} implementation with a preexisting id that is being converted via {@link
     * #convertFrom(KnowledgeBase)}.
     *
     * @param cause the cause to add.
     * @return the same cause but with a new id.
     * @throws Exception if something in the KnowledgeBase handling goes wrong.
     */
    @Override
    public FailureCause saveCause(FailureCause cause) throws Exception {
        try {
            ListTablesResult tables = getDynamoDb().listTables();
            if (!tables.getTableNames().contains(tableName)) {
                createTables();
            }

            getDbMapper().save(cause);
        } catch (Exception e) {
            throw e;
        }
        return cause;
    }

    /**
     * Converts the existing old knowledge base into this one. Will be called after the creation of a new object when
     * then Jenkins config is saved, So it could just be that the old one is exactly the same as this one.
     *
     * @param oldKnowledgeBase the old one.
     * @throws Exception if something in the KnowledgeBase handling goes wrong.
     */
    @Override
    public void convertFrom(KnowledgeBase oldKnowledgeBase) throws Exception {

    }

    /**
     * Gets the unique categories of all FailureCauses.
     *
     * @return the list of categories.
     * @throws Exception if something in the KnowledgeBase handling goes wrong.
     */
    @Override
    public List<String> getCategories() throws Exception {
        return null;
    }

    /**
     * Called to see if the configuration has changed.
     *
     * @param oldKnowledgeBase the previous config.
     * @return true if it is the same.
     */
    @Override
    public boolean equals(KnowledgeBase oldKnowledgeBase) {
        return false;
    }

    /**
     * Called when the KnowledgeBase should be up and running.
     *
     * @throws Exception if anything goes wrong during the startup.
     */
    @Override
    public void start() throws Exception {
        getDynamoDb();
    }

    /**
     * Called when it is time to clean up after the KnowledgeBase.
     */
    @Override
    public void stop() {

    }

    /**
     * If Statistics logging is enabled on this knowledge base or not.
     *
     * @return true if so. False if not or not implemented.
     */
    @Override
    public boolean isStatisticsEnabled() {
        return false;
    }

    /**
     * If all builds should be added to statistics logging, not just unsuccessful builds.
     * Only relevant if {@link #isStatisticsEnabled()} is true.
     *
     * @return true if set, false otherwise or if not implemented
     */
    @Override
    public boolean isSuccessfulLoggingEnabled() {
        return false;
    }

    /**
     * Saves the Statistics.
     *
     * @param stat the Statistics.
     * @throws Exception if something in the KnowledgeBase handling goes wrong.
     */
    @Override
    public void saveStatistics(Statistics stat) throws Exception {

    }

    public AmazonDynamoDB getDynamoDb() {
        /*
         * The ProfileCredentialsProvider will return your [default]
         * credential profile by reading from the credentials file located at
         * (~/.aws/credentials).
         */

        if (dynamoDB != null) {
            return dynamoDB;
        }

        ProfileCredentialsProvider credentialsProvider = new ProfileCredentialsProvider();
        try {
            credentialsProvider.getCredentials();
        } catch (Exception e) {
            throw new AmazonClientException(
                    "Cannot load the credentials from the credential profiles file. " +
                            "Please make sure that your credentials file is at the correct " +
                            "location (~/.aws/credentials), and is in valid format.",
                    e);
        }

        dynamoDB = AmazonDynamoDBClientBuilder.standard()
                .withCredentials(credentialsProvider)
                .withRegion("us-west-2")
//                .withEndpointConfiguration(
//                        new AwsClientBuilder.EndpointConfiguration("http://localhost:8000", "us-west-2"))
                .build();

        return dynamoDB;
    }

    public void createTables() {
        try {
            AmazonDynamoDB db = getDynamoDb();
            CreateTableRequest causeTableRequest = new CreateTableRequest()
                    .withTableName(tableName)
                    .withKeySchema(new KeySchemaElement().withAttributeName("id").withKeyType(KeyType.HASH))
                    .withAttributeDefinitions(new AttributeDefinition().withAttributeName("id").withAttributeType(ScalarAttributeType.S))
                    .withProvisionedThroughput(new ProvisionedThroughput().withReadCapacityUnits(1L).withWriteCapacityUnits(1L));

            TableUtils.createTableIfNotExists(db, causeTableRequest);
            TableUtils.waitUntilActive(db, tableName);

            dynamoDB.listTables();

        } catch (Exception e) {
            throw new AmazonClientException(e);
        }
    }

    public DynamoDBMapper getDbMapper() {
        if (dbMapper != null) {
            return dbMapper;
        }
        dbMapper = new DynamoDBMapper(getDynamoDb());
        return dbMapper;
    }

    @Override
    public Descriptor<KnowledgeBase> getDescriptor() {
        return null;
    }
}
