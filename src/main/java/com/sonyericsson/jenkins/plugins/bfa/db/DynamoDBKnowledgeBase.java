package com.sonyericsson.jenkins.plugins.bfa.db;

import com.amazonaws.regions.Region;
import com.amazonaws.regions.RegionUtils;
import com.amazonaws.regions.Regions;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBMapper;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBScanExpression;
import com.amazonaws.services.dynamodbv2.model.Condition;
import com.amazonaws.services.dynamodbv2.model.CreateTableRequest;
import com.amazonaws.services.dynamodbv2.model.ProvisionedThroughput;
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
import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Handling of the Amazon Web Services DynamoDB way of saving the knowledge base.
 *
 * @author Ken Petti &lt;kpetti@constantcontact.com&gt;
 */
public class DynamoDBKnowledgeBase extends KnowledgeBase {

    private static final long serialVersionUID = 1;
    private static final String DYNAMODB_DEFAULT_REGION = Regions.DEFAULT_REGION.getName();
    private static final String DYNAMODB_DEFAULT_CREDENTIALS_PATH =
            System.getProperty("user.home") + "/.aws/credentials";
    private static final String DYNAMODB_DEFAULT_CREDENTIAL_PROFILE = "default";
    static final Map<String, Condition> NOT_REMOVED_FILTER_EXPRESSION = new HashMap<String, Condition>(){{
        put("_removed", new Condition().withComparisonOperator("NULL"));
    }};

    private static AmazonDynamoDB dynamoDB;
    private transient DynamoDBMapper dbMapper;

    private String region;
    private String credentialsPath;
    private String credentialsProfile;

    /**
     * Getter for the DynamoDB region.
     * @return the region.
     */
    public String getRegion() {
        return region;
    }

    /**
     * Getter for the AWS credentials path.
     * @return the credentialsPath.
     */
    public String getCredentialsPath() {
        return credentialsPath;
    }

    /**
     * Getter for the AWS credentials profile.
     * @return the credentialsProfile string.
     */
    public String getCredentialsProfile() {
        return credentialsProfile;
    }

    /**
     * Standard constructor.
     * @param region the AWS region to connect to DynamoDB with.
     * @param credentialsPath the path to a local file containing AWS credentials.
     * @param credentialsProfile the AWS credential profile to use for connecting to DynamoDB.
     */
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

    /**
     * Get the list of {@link FailureCause}s except those marked as removed. It is intended to be used in the scanning
     * phase hence it should be returned as quickly as possible, so the list could be cached.
     *
     * @return the full list of causes.
     */
    // TODO: 4/19/18 Add caching here
    @Override
    public Collection<FailureCause> getCauses() {
        DynamoDBScanExpression scan = new DynamoDBScanExpression();
        scan.setScanFilter(NOT_REMOVED_FILTER_EXPRESSION);
        return getDbMapper().scan(FailureCause.class, scan);
    }

    /**
     * Get the list of the {@link FailureCause}'s names and ids. The list should be the latest possible from the DB as
     * they will be used for editing. The objects returned should contain at least the id and the name of the cause.
     *
     * @return the full list of the names and ids of the causes.
     */
    @Override
    public Collection<FailureCause> getCauseNames() {
        DynamoDBScanExpression scan = new DynamoDBScanExpression();
        scan.addExpressionAttributeNamesEntry("#n", "name");
        scan.setProjectionExpression("id,#n");
        scan.setScanFilter(NOT_REMOVED_FILTER_EXPRESSION);
        return getDbMapper().scan(FailureCause.class, scan);
    }

    /**
     * Get a shallow list of the {@link FailureCause}s. The list should be the latest possible from the DB as
     * they will be used in the list of causes to edit.
     * shallow meaning no indications but information enough to show a nice list; at least id and name but description,
     * comment, lastOccurred and categories are preferred as well.
     *
     * @return a shallow list of all causes.
     * @see #getCauseNames()
     */
    @Override
    public Collection<FailureCause> getShallowCauses() {
        DynamoDBScanExpression scan = new DynamoDBScanExpression();
        // The following attributes are reserved words in Dynamo, so we need to substitute the actual name for
        // something safe
        scan.addExpressionAttributeNamesEntry("#n", "name");
        scan.addExpressionAttributeNamesEntry("#c", "comment");
        scan.addExpressionAttributeNamesEntry("#r", "_removed");
        scan.setProjectionExpression("id,#n,description,categories,#c,modifications,lastOccurred");
        scan.setFilterExpression(" attribute_not_exists(#r) ");
        return getDbMapper().scan(FailureCause.class, scan);
    }

    /**
     * Get the cause with the given id. The cause returned is intended to be edited right away, so it should be as fresh
     * from the db as possible.
     *
     * @param id the id of the cause.
     * @return the cause or null if a cause with that id could not be found.
     */
    @Override
    public FailureCause getCause(String id) {
        return getDbMapper().load(FailureCause.class, id);
    }

    /**
     * Saves a new cause to the db, which generates a new id for the cause.
     *
     * @param cause the cause to add.
     * @return the same cause but with a new id.
     */
    @Override
    public FailureCause addCause(FailureCause cause) {
        return saveCause(cause);
    }

    /**
     * Marks the cause as removed in the knowledge base.
     *
     * @param id the id of the cause to remove.
     * @return the removed FailureCause.
     */
    @Override
    public FailureCause removeCause(String id) {
        FailureCause cause = getDbMapper().load(FailureCause.class, id);
        cause.setRemoved();
        getDbMapper().save(cause);
        return cause;
    }

    /**
     * Saves a cause to the db. Assumes that the id is kept from when it was fetched. Can also be an existing cause in
     * another {@link KnowledgeBase} implementation with a preexisting id that is being converted via {@link
     * #convertFrom(KnowledgeBase)}.
     *
     * @param cause the cause to add.
     * @return the same cause but with a new id.
     */
    @Override
    public FailureCause saveCause(FailureCause cause) {
        getDbMapper().save(cause);
        return cause;
    }

    /**
     * Converts the existing old knowledge base into this one. Will be called after the creation of a new object when
     * then Jenkins config is saved, So it could just be that the old one is exactly the same as this one.
     *
     * @param oldKnowledgeBase the old one.
     * @throws Exception if converting DB fails or something in the KnowledgeBase handling goes wrong.
     */
    @Override
    public void convertFrom(KnowledgeBase oldKnowledgeBase) throws Exception {
        if (oldKnowledgeBase instanceof DynamoDBKnowledgeBase) {
            convertFromAbstract(oldKnowledgeBase);
            convertRemoved((DynamoDBKnowledgeBase)oldKnowledgeBase);
        } else {
            for (FailureCause cause : oldKnowledgeBase.getCauseNames()) {
                saveCause(cause);
            }
        }
    }

    /**
     * Copies all causes flagged as removed from the old database to this one.
     *
     * @param oldKnowledgeBase the old database.
     */
    private void convertRemoved(DynamoDBKnowledgeBase oldKnowledgeBase) {
        Collection<FailureCause> removed = oldKnowledgeBase.getRemovedCauses();
        for (FailureCause obj : removed) {
            saveCause(obj);
        }
    }

    /**
     * Gets all causes flagged as removed in a "raw" JSON format.
     *
     * @return the list of removed causes.
     */
    private Collection<FailureCause> getRemovedCauses() {
        DynamoDBScanExpression scan = new DynamoDBScanExpression();
        scan.setFilterExpression(" attribute_exists(#r) ");
        return getDbMapper().scan(FailureCause.class, scan);
    }

    /**
     * Gets the unique categories of all FailureCauses.
     *
     * @return the list of categories.
     */
    @Override
    public List<String> getCategories() {
        DynamoDBScanExpression scan = new DynamoDBScanExpression();
        scan.setProjectionExpression("categories");
        scan.setFilterExpression(" attribute_exists(categories) ");
        List<FailureCause> causes = getDbMapper().scan(FailureCause.class, scan);
        Set<String> categories = new HashSet<>();
        for (FailureCause c:causes) {
            categories.addAll(c.getCategories());
        }
        return new ArrayList<>(categories);
    }

    /**
     * Called to see if the configuration has changed.
     *
     * @param oldKnowledgeBase the previous config.
     * @return true if it is the same.
     */
    @Override
    public boolean equals(KnowledgeBase oldKnowledgeBase) {
        if (getClass().isInstance(oldKnowledgeBase)) {
            DynamoDBKnowledgeBase oldDynamoDBKnowledgeBase = (DynamoDBKnowledgeBase)oldKnowledgeBase;
            return oldDynamoDBKnowledgeBase.getRegion().equals(region)
                    && oldDynamoDBKnowledgeBase.getCredentialsPath().equals(credentialsPath)
                    && oldDynamoDBKnowledgeBase.getCredentialsProfile().equals(credentialsProfile);
        } else {
            return false;
        }
    }

    /**
     * Overrides base Object equals.
     * @param other object to check
     * @return boolean if values are equal
     */
    @Override
    public boolean equals(Object other) {
        if (other instanceof KnowledgeBase) {
            return this.equals((KnowledgeBase)other);
        } else {
            return false;
        }
    }

    /**
     * Makes checkstyle happy.
     * @return hashcode of class
     */
    @Override
    public int hashCode() {
        //Making checkstyle happy.
        return getClass().getName().hashCode();
    }

    /**
     * Called when the KnowledgeBase should be up and running.
     */
    @Override
    public void start() {
        getDynamoDb();
    }

    /**
     * Called when it is time to clean up after the KnowledgeBase.
     */
    // TODO: 4/19/18 Implement this
    @Override
    public void stop() {

    }

    /**
     * If Statistics logging is enabled on this knowledge base or not.
     *
     * @return true if so. False if not or not implemented.
     */
    // TODO: 4/19/18 Implement this
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
    // TODO: 4/19/18 Implement this
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
    // TODO: 4/19/18 Implement this
    @Override
    public void saveStatistics(Statistics stat) throws Exception {

    }

    /**
     * Get an instance of {@link AmazonDynamoDB}. Connects to the defined region with the defined AWS
     * credentials file/profile. If this has been called before, it will return the cached version.
     * @return instance of AmazonDynamoDB
     */
    private AmazonDynamoDB getDynamoDb() {
        if (dynamoDB != null) {
            return dynamoDB;
        }

        ProfileCredentialsProvider credentialsProvider =
                new ProfileCredentialsProvider(credentialsPath, credentialsProfile);
        try {
            credentialsProvider.getCredentials();
        } catch (Exception e) {
            throw new AmazonClientException(
                    "Cannot load the credentials from the credential profiles file. "
                            + "Please make sure that your credentials file is at the correct "
                            + "location (~/.aws/credentials), and is in valid format.",
                    e);
        }

        dynamoDB = AmazonDynamoDBClientBuilder.standard()
                .withCredentials(credentialsProvider)
                .withRegion(region)
                .build();

        return dynamoDB;
    }

    /**
     * Get a cached or new instance of {@link DynamoDBMapper}.
     * @return dbMapper
     */
    private DynamoDBMapper getDbMapper() {
        if (dbMapper != null) {
            return dbMapper;
        }
        dbMapper = new DynamoDBMapper(getDynamoDb());
        createTable(dbMapper.generateCreateTableRequest(FailureCause.class));

        return dbMapper;
    }

    /**
     * Creates a DynamoDB table.
     * @param request {@link CreateTableRequest}
     */
    private void createTable(CreateTableRequest request) {
        try {
            String tableName = request.getTableName();
            AmazonDynamoDB db = getDynamoDb();
            request.setProvisionedThroughput(new ProvisionedThroughput()
                    .withReadCapacityUnits(1L).withWriteCapacityUnits(1L));
            TableUtils.createTableIfNotExists(db, request);
            TableUtils.waitUntilActive(db, tableName);

        } catch (Exception e) {
            throw new AmazonClientException(e);
        }
    }

    /**
     * Use Jenkins to get and instance of {@link DynamoDBKnowledgeBaseDescriptor}.
     * @return Descriptor
     */
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

        /**
         * Get a list of valid AWS regions for Jelly.
         * @return ListBoxModel containing AWS regions
         */
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
         * @param credentialsPath the filepath to credentials.
         * @param credentialProfile the credential profile to use.
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
