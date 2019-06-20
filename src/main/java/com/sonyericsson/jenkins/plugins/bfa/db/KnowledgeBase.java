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

import com.sonyericsson.jenkins.plugins.bfa.graphs.FailureCauseTimeInterval;
import com.sonyericsson.jenkins.plugins.bfa.graphs.GraphFilterBuilder;
import com.sonyericsson.jenkins.plugins.bfa.model.FailureCause;
import com.sonyericsson.jenkins.plugins.bfa.statistics.Statistics;
import com.sonyericsson.jenkins.plugins.bfa.utils.ObjectCountPair;
import hudson.ExtensionList;
import hudson.Util;
import hudson.model.AbstractBuild;
import hudson.model.Describable;
import hudson.model.Descriptor;
import hudson.model.Run;
import jenkins.model.Jenkins;
import org.jfree.data.time.TimePeriod;

import java.io.Serializable;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Map;

/**
 * Base class for storage implementations of {@link FailureCause}s. Extend this class and put <code>@Extension</code> on
 * the descriptor to provide your own.
 *
 * @author Robert Sandell &lt;robert.sandell@sonyericsson.com&gt;
 */
public abstract class KnowledgeBase implements Describable<KnowledgeBase>, Serializable {

    private static final long DEFAULT_NBR_OF_NULL_FAILURE_CAUSES = 0;

    /**
     * Get the list of {@link FailureCause}s. It is intended to be used in the scanning phase hence it should be
     * returned as quickly as possible, so the list could be cached.
     *
     * @return the full list of causes.
     * @throws Exception if something in the KnowledgeBase handling goes wrong.
     */
    public abstract Collection<FailureCause> getCauses() throws Exception;

    /**
     * Get the list of the {@link FailureCause}'s names and ids. The list should be the latest possible from the DB as
     * they will be used for editing. The objects returned should contain at least the id and the name of the cause.
     *
     * @return the full list of the names and ids of the causes.
     * @throws Exception if something in the KnowledgeBase handling goes wrong.
     */
    public abstract Collection<FailureCause> getCauseNames() throws Exception;

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
    public abstract Collection<FailureCause> getShallowCauses() throws Exception;

    /**
     * Get the cause with the given id. The cause returned is intended to be edited right away, so it should be as fresh
     * from the db as possible.
     *
     * @param id the id of the cause.
     * @return the cause or null if a cause with that id could not be found.
     * @throws Exception if something in the KnowledgeBase handling goes wrong.
     */
    public abstract FailureCause getCause(String id) throws Exception;

    /**
     * Saves a new cause to the db and generates a new id for the cause.
     *
     * @param cause the cause to add.
     * @return the same cause but with a new id.
     * @throws Exception if something in the KnowledgeBase handling goes wrong.
     */
    public abstract FailureCause addCause(FailureCause cause) throws Exception;

    /**
     * Removes the cause from the knowledge base.
     *
     * @param id the id of the cause to remove.
     * @throws Exception if so.
     * @return the removed FailureCause.
     */
    public abstract FailureCause removeCause(String id) throws Exception;

    /**
     * Saves a cause to the db. Assumes that the id is kept from when it was fetched. Can also be an existing cause in
     * another {@link KnowledgeBase} implementation with a preexisting id that is being converted via {@link
     * #convertFrom(KnowledgeBase)}.
     *
     * @param cause the cause to add.
     * @return the same cause but with a new id.
     * @throws Exception if something in the KnowledgeBase handling goes wrong.
     */
    public abstract FailureCause saveCause(FailureCause cause) throws Exception;

    /**
     * Converts the existing old knowledge base into this one. Will be called after the creation of a new object when
     * then Jenkins config is saved, So it could just be that the old one is exactly the same as this one.
     *
     * @param oldKnowledgeBase the old one.
     * @throws Exception if something in the KnowledgeBase handling goes wrong.
     */
    public abstract void convertFrom(KnowledgeBase oldKnowledgeBase) throws Exception;

    /**
     * Gets the unique categories of all FailureCauses.
     * @return the list of categories.
     * @throws Exception if something in the KnowledgeBase handling goes wrong.
     */
    public abstract List<String> getCategories() throws Exception;

    /**
     * Does a full copy of the data in the old one to this one. Using the public api, can be used by implementations of
     * {@link #convertFrom(KnowledgeBase)} to handle conversion from an unknown source type.
     *
     * @param oldKnowledgeBase the old one.
     * @see #convertFrom(KnowledgeBase)
     * @throws Exception if anything goes wrong in the KnowledgeBase handling.
     */
    protected void convertFromAbstract(KnowledgeBase oldKnowledgeBase) throws Exception {
        for (FailureCause cause : oldKnowledgeBase.getCauseNames()) {
            saveCause(oldKnowledgeBase.getCause(cause.getId()));
        }
    }

    /**
     * Called to see if the configuration has changed.
     *
     * @param oldKnowledgeBase the previous config.
     * @return true if it is the same.
     */
    public abstract boolean equals(KnowledgeBase oldKnowledgeBase);

    /**
     * Called when the KnowledgeBase should be up and running.
     * @throws  Exception if anything goes wrong during the startup.
     */
    public abstract void start() throws Exception;

    /**
     * Called when it is time to clean up after the KnowledgeBase.
     */
    public abstract void stop();

    /**
     * If Statistics logging is enabled on this knowledge base or not.
     *
     * @return true if so. False if not or not implemented.
     */
    public abstract boolean isStatisticsEnabled();

    /**
     * If all builds should be added to statistics logging, not just unsuccessful builds.
     * Only relevant if {@link #isStatisticsEnabled()} is true.
     *
     * @return true if set, false otherwise or if not implemented
     */
    public abstract boolean isSuccessfulLoggingEnabled();

    /**
     * Saves the Statistics.
     * @param stat the Statistics.
     * @throws Exception if something in the KnowledgeBase handling goes wrong.
     */
    public abstract void saveStatistics(Statistics stat) throws Exception;

    /**
     * Gets Statistics data. This method needs to be implemented in subclass for graph support.
     * @param filter the filter to use when fetching data
     * @param limit number of statistics items to fetch, set to nonpositive value to fetch all
     * @return the list of statistics.
     * @throws Exception if something in the KnowledgeBase handling goes wrong.
     */
    public List<Statistics> getStatistics(GraphFilterBuilder filter, int limit) throws Exception {
        return Collections.<Statistics>emptyList();
    }

    /**
     * Gets a list of {@link ObjectCountPair} where each pair contains a unique {@link FailureCause} as key
     * and the number of times that failure cause was triggered as count.
     * The list is sorted by counts, meaning that the FailureCause that has been triggered the most comes first.
     * This method needs to be implemented in subclass for graph support.
     *
     * @param filter the filter to use when fetching data
     * @return list of ObjectCountPairs.
     */
    public List<ObjectCountPair<FailureCause>> getNbrOfFailureCauses(GraphFilterBuilder filter) {
        return Collections.<ObjectCountPair<FailureCause>>emptyList();
    }

    /**
     * Gets the quota of unknown failure causes mapped by time periods.
     * This method needs to be implemented in subclass for graph support.
     * @param intervalSize the interval sizes in which the data is grouped.
     * Should be set to Calendar.MONTH, Calendar.DATE or Calendar.HOUR_OF_DAY.
     * @param filter The filter to use when fetching the data
     * @return failure cause quotas
     */
    public Map<TimePeriod, Double> getUnknownFailureCauseQuotaPerTime(int intervalSize, GraphFilterBuilder filter) {
        return Collections.<TimePeriod, Double>emptyMap();
    }

    /**
     * Gets a list of {@link ObjectCountPair}s where each pair contains a unique {@link FailureCause}-name as key
     * and the number of times that failure cause was triggered as count.
     * This list is sorted by counts, meaning that the FailureCause that has been triggered the most comes first.
     * This method needs to be implemented in subclass for graph support.
     *
     * @param filter The filter to use when fetching the data
     * @return List of ObjectCountPairs that consist of a name and count
     */
    public List<ObjectCountPair<String>> getFailureCauseNames(GraphFilterBuilder filter) {
        return Collections.<ObjectCountPair<String>>emptyList();
    }

    /**
     * Counts how many statistics posts there are without FailureCause (null) for a given filter.
     * This method needs to be implemented in subclass for graph support.
     * @param filter the filter to use when fetching data
     * @return number of statistics posts without FailureCause
     */
    public long getNbrOfNullFailureCauses(GraphFilterBuilder filter) {
        return DEFAULT_NBR_OF_NULL_FAILURE_CAUSES;
    }

    /**
     * Gets a list of {@link ObjectCountPair} where each pair contains a unique failure category string as key
     * and the number of times that the failure cause category was triggered as count.
     * The list is sorted by counts, meaning that the Category that has been triggered the most comes first.
     * This method needs to be implemented in subclass for graph support.
     *
     * @param filter the filter to use when fetching data
     * @param limit the number of categories to fetch, set to nonpositive value to fetch all
     * @return list of ObjectCountPairs
     */
    public List<ObjectCountPair<String>> getNbrOfFailureCategoriesPerName(GraphFilterBuilder filter, int limit) {
        return Collections.<ObjectCountPair<String>>emptyList();
    }

    /**
     * Gets a map where a lists of failure causes are mapped
     * by the build number for which they were triggered.
     * This method needs to be implemented in subclass for graph support.
     *
     * @param filter the filter to use when fetching data
     * @return map of failure causes
     */
    public Map<Integer, List<FailureCause>> getFailureCausesPerBuild(GraphFilterBuilder filter) {
        return Collections.<Integer, List<FailureCause>>emptyMap();
    }

    /**
     * Fetches failure causes grouped in time intervals. The returned list does not have to be sorted,
     * and one list element is created for each FailureCause for each time interval there exist data.
     * This method needs to be implemented in subclass for graph support.
     *
     * @param intervalSize the interval sizes in which the data is grouped.
     * Should be set to Calendar.MONTH, Calendar.DATE or Calendar.HOUR_OF_DAY.
     * @param filter the filter to use when fetching data
     * @param byCategories set to true in order to group failure causes by their categories
     * @return list of FailureCauseTimeIntervals
     */
    public List<FailureCauseTimeInterval> getFailureCausesPerTime(int intervalSize, GraphFilterBuilder filter,
            boolean byCategories) {
        return Collections.<FailureCauseTimeInterval>emptyList();
    }

    /**
     * Gets a list of {@link ObjectCountPair} where each pair contains a unique FailureCause id as key
     * and a belonging count value for how many times that FailureCause was triggered.
     * This method needs to be implemented in subclass for graph support.
     *
     * @param filter the filter to use when fetching data
     * @param limit the number of items to fetch, set to nonpositive value to fetch all
     * @return list of ObjectCountPairs
     */
    public List<ObjectCountPair<String>> getNbrOfFailureCausesPerId(GraphFilterBuilder filter, int limit) {
        return Collections.<ObjectCountPair<String>>emptyList();
    }

    /**
     * Find the time at which the latest Failure occurred which matches the provided FailureCause.
     * This method needs to be implemented in subclass if last seen-functionality is to be supported.
     *
     * @param id     the FailureCause to match.
     * @return the time at which the latest Failure occurred.
     */
    public Date getLatestFailureForCause(String id) {
        return null;
    }

    /**
     * Set the time at which FailureCauses identified by ids last occurred.
     * This method needs to be implemented in subclass if last seen-functionality is to be supported.
     *
     * @param ids   the ids of FailureCauses which occurred.
     * @param seen  the time at which the FailureCauses occurred.
     */
    public void updateLastSeen(List<String> ids, Date seen) {
        return;
    }

    /**
     * Find out when the FailureCause identified by id was first created.
     * This method needs to be implemented in subclass if last modified-functionality is to work correctly
     * when upgrading from a version without this functionality.
     *
     * @param id the id of the FailureCause which info to retrieve.
     * @return the Date at which the cause was created, or unix epoch if unknown.
     */
    public Date getCreationDateForCause(String id) {
        return new Date(0);
    }

    /**
     * Removes the build failure cause of particular build.
     * @param build the build.
     * @throws Exception if something in the KnowledgeBase handling goes wrong.
     * @deprecated use {@link #removeBuildfailurecause(hudson.model.Run)}
     */
    @Deprecated
    public void removeBuildfailurecause(AbstractBuild build) throws Exception {
        if (Util.isOverridden(KnowledgeBase.class, getClass(), "removeBuildfailurecause", Run.class)) {
            removeBuildfailurecause((Run)build);
        }
    }

    /**
     * Removes the build failure cause of particular build.
     * @param build the build.
     * @throws Exception if something in the KnowledgeBase handling goes wrong.
     */
    public void removeBuildfailurecause(Run build) throws Exception {
        if (Util.isOverridden(KnowledgeBase.class, getClass(), "removeBuildfailurecause", AbstractBuild.class)) {
            removeBuildfailurecause((AbstractBuild)build);
        }
    }



    /**
     * Descriptor for {@link KnowledgeBase}s.
     */
    public abstract static class KnowledgeBaseDescriptor extends Descriptor<KnowledgeBase> {

        /**
         * All registered {@link KnowledgeBaseDescriptor}s.
         *
         * @return the extension list.
         */
        public static ExtensionList<KnowledgeBaseDescriptor> all() {
            return Jenkins.getInstance().getExtensionList(KnowledgeBaseDescriptor.class);
        }
    }
}
