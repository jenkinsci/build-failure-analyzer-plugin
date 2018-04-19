package com.sonyericsson.jenkins.plugins.bfa.db;

import com.amazonaws.regions.Region;
import com.amazonaws.regions.RegionUtils;
import com.amazonaws.regions.Regions;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBMapper;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBScanExpression;
import com.amazonaws.services.dynamodbv2.datamodeling.PaginatedScanList;
import com.amazonaws.services.dynamodbv2.model.*;
import com.amazonaws.services.dynamodbv2.util.TableUtils;
import com.sonyericsson.jenkins.plugins.bfa.model.FailureCause;
import com.sonyericsson.jenkins.plugins.bfa.Messages;
import com.sonyericsson.jenkins.plugins.bfa.statistics.Statistics;
import hudson.Extension;
import hudson.model.Descriptor;
import com.amazonaws.AmazonClientException;
import com.amazonaws.auth.profile.ProfileCredentialsProvider;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDB;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDBClientBuilder;
import hudson.util.FormValidation;
import hudson.util.ListBoxModel;
import jenkins.model.Jenkins;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.QueryParameter;


import javax.xml.stream.events.Attribute;
import java.io.File;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.stream.Collectors;

public class DynamoDBKnowledgeBase extends KnowledgeBase {

    private static final String DYNAMODB_DEFAULT_REGION = Regions.DEFAULT_REGION.getName();
    public static final String DYNAMODB_DEFAULT_CREDENTIALS_PATH = System.getProperty("user.home") + "/.aws/credentials";
    private static final String DYNAMODB_DEFAULT_CREDENTIAL_PROFILE = "default";
    static final Map<String, Condition> NOT_REMOVED_FILTER_EXPRESSION = new HashMap<String, Condition>(){{
        put("_removed", new Condition().withComparisonOperator("NULL"));
    }};

    private static AmazonDynamoDB dynamoDB;
    private DynamoDBMapper dbMapper;

    private String region;
    private String credentialsPath;
    private String credentialsProfile;

    @DataBoundConstructor
    public DynamoDBKnowledgeBase(String region, String credentialsPath, String credentialsProfile) {
        if (region == null || region.isEmpty()) {
            region = DYNAMODB_DEFAULT_REGION;
        }
        if (credentialsPath == null || credentialsPath.isEmpty()) {
            credentialsPath = DYNAMODB_DEFAULT_CREDENTIALS_PATH;
        }
        if (credentialsProfile == null || credentialsProfile.isEmpty()) {
            credentialsProfile = DYNAMODB_DEFAULT_CREDENTIAL_PROFILE;
        }

        this.region = region;
        this.credentialsPath = credentialsPath;
        this.credentialsProfile = credentialsProfile;
    }

    public String getRegion() {
        return region;
    }

    public String getCredentialsPath() {
        return credentialsPath;
    }

    public String getCredentialsProfile() {
        return credentialsProfile;
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
        try {
            DynamoDBScanExpression scan = new DynamoDBScanExpression();
            scan.setScanFilter(NOT_REMOVED_FILTER_EXPRESSION);
            return getDbMapper().scan(FailureCause.class, scan);
        } catch (Exception e) {
            throw e;
        }
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
        try {
            DynamoDBScanExpression scan = new DynamoDBScanExpression();
            scan.addExpressionAttributeNamesEntry("#n", "name");
            scan.setProjectionExpression("id,#n");
            scan.setScanFilter(NOT_REMOVED_FILTER_EXPRESSION);
            return getDbMapper().scan(FailureCause.class, scan);
        } catch (Exception e) {
            throw e;
        }
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
        try {
            DynamoDBScanExpression scan = new DynamoDBScanExpression();
            scan.addExpressionAttributeNamesEntry("#n", "name");
            scan.addExpressionAttributeNamesEntry("#c", "comment");
            scan.addExpressionAttributeNamesEntry("#r", "_removed");
            scan.setProjectionExpression("id,#n,description,categories,#c,modifications,lastOccurred");
            scan.setFilterExpression(" attribute_not_exists(#r) ");
            return getDbMapper().scan(FailureCause.class, scan);
        } catch (Exception e) {
            throw e;
        }
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
     * Marks the cause as removed in the knowledge base.
     *
     * @param id the id of the cause to remove.
     * @return the removed FailureCause.
     * @throws Exception if so.
     */
    @Override
    public FailureCause removeCause(String id) throws Exception {
        try {
            FailureCause cause = getDbMapper().load(FailureCause.class, id);
            cause.setRemoved();
            getDbMapper().save(cause);
            return cause;
        } catch (Exception e) {
            throw e;
        }
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
        try {
            DynamoDBScanExpression scan = new DynamoDBScanExpression();
            scan.setProjectionExpression("categories");
            scan.setFilterExpression(" attribute_exists(categories) ");
            List<FailureCause> causes = getDbMapper().scan(FailureCause.class, scan);
            Set<String> categories = new HashSet<>();
            for (FailureCause c:causes) {
                categories.addAll(c.getCategories());
            }
            return new ArrayList<>(categories);
        } catch (Exception e) {
            throw e;
        }
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
        if (dynamoDB != null) {
            return dynamoDB;
        }

        ProfileCredentialsProvider credentialsProvider = new ProfileCredentialsProvider(credentialsPath, credentialsProfile);
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
                .withRegion(region)
                .build();

        return dynamoDB;
    }

    private DynamoDBMapper getDbMapper() {
        if (dbMapper != null) {
            return dbMapper;
        }
        dbMapper = new DynamoDBMapper(getDynamoDb());
        createTable(dbMapper.generateCreateTableRequest(FailureCause.class));

        return dbMapper;
    }

    private void createTable(CreateTableRequest request) {
        try {
            String tableName = request.getTableName();
            AmazonDynamoDB db = getDynamoDb();
            request.setProvisionedThroughput(new ProvisionedThroughput().withReadCapacityUnits(1L).withWriteCapacityUnits(1L));
            TableUtils.createTableIfNotExists(db, request);
            TableUtils.waitUntilActive(db, tableName);

        } catch (Exception e) {
            throw new AmazonClientException(e);
        }
    }

    @Override
    public Descriptor<KnowledgeBase> getDescriptor() {
        return Jenkins.getInstance().getDescriptorByType(DynamoDBKnowledgeBaseDescriptor.class);
    }

    /**
     * Descriptor for {@link DynamoDBKnowledgeBase}.
     */
    @Extension
    public static class DynamoDBKnowledgeBaseDescriptor extends KnowledgeBaseDescriptor {

        @Override
        public String getDisplayName() {
            return Messages.DynamoDBKnowledgeBase_DisplayName();
        }

        /**
         * Convenience method for jelly.
         * @return the default region.
         */
        public String getDefaultRegion() {
            return DYNAMODB_DEFAULT_REGION;
        }

        /**
         * Convenience method for jelly.
         * @return the default region.
         */
        public String getDefaultCredentialsPath() {
            return DYNAMODB_DEFAULT_CREDENTIALS_PATH;
        }

        /**
         * Convenience method for jelly.
         * @return the default region.
         */
        public String getDefaultCredentialProfile() {
            return DYNAMODB_DEFAULT_CREDENTIAL_PROFILE;
        }

        public ListBoxModel doFillRegionItems() {
            ListBoxModel items = new ListBoxModel();
            for (Region r:RegionUtils.getRegions()) {
                String regionName = r.getName();
                items.add(regionName, regionName);
            }
            return items;
        }

        /**
         * Checks that the credential file exists.
         *
         * @param value the pattern to check.
         * @return {@link hudson.util.FormValidation#ok()} if everything is well.
         */
        public FormValidation doCheckCredentialsPath(@QueryParameter("value") final String value) {
            File f = new File(value);
            if(!f.exists()) {
                return FormValidation.error("Credential file does not exist!");
            }

            if (f.isDirectory()) {
                return FormValidation.error("Credential file can not be a directory!");
            }
            return FormValidation.ok();
        }

        /**
         * Checks that the credential profile is set.
         *
         * @param value the pattern to check.
         * @return {@link hudson.util.FormValidation#ok()} if everything is well.
         */
        public FormValidation doCheckCredentialsProfile(@QueryParameter("value") final String value) {
            if (value == null || value.isEmpty()) {
                return FormValidation.warning("No credential profile entered, using \"default\" profile");
            }

            return FormValidation.ok();
        }

        /**
         * Tests if the provided parameters can connect to the DynamoDB service.
         * @param region the region name.
         * @return {@link FormValidation#ok() } if can be done,
         *         {@link FormValidation#error(java.lang.String) } otherwise.
         */
        public FormValidation doTestConnection(
                @QueryParameter("region") final String region,
                @QueryParameter("credentialsPath") final String credentialsPath,
                @QueryParameter("credentialProfile") final String credentialProfile
                ) {
            DynamoDBKnowledgeBase base = new DynamoDBKnowledgeBase(region, credentialsPath, credentialProfile);
            try {
                base.getDynamoDb();
            } catch (Exception e) {
                return FormValidation.error(e, Messages.DynamoDBKnowledgeBase_ConnectionError());
            }
            return FormValidation.ok(Messages.DynamoDBKnowledgeBase_ConnectionOK());
        }
    }
}
