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

import hudson.model.ModelObject;
import hudson.model.AbstractProject;
import hudson.util.Graph;

import java.util.Date;
import java.util.Map;
import java.util.regex.Pattern;

import com.sonyericsson.jenkins.plugins.bfa.BfaGraphAction;
import com.sonyericsson.jenkins.plugins.bfa.utils.BfaUtils;

/**
 * Action class for displaying graphs on the project page.
 *
 * @author Fredrik Persson &lt;fredrik6.persson@sonymobile.com&gt;
 */
public class ProjectGraphAction extends BfaGraphAction {
    private static final int GRAPH_WIDTH_SMALL = 500;
    private static final int GRAPH_HEIGHT_SMALL = 200;
    private static final int NBR_OF_BUILDS = 25;

    private static final String URL_NAME = "bfa-proj-graphs";
    private static final String PAGE_TITLE = "Statistics for project";
    private static final String GRAPH_TITLE_CAUSES = "Failure causes for this project";
    private static final String GRAPH_TITLE_CAUSES_SMALL = "Failure causes for this project last 30 days";
    private static final String GRAPH_TITLE_CATEGORIES = "Failures grouped by categories for this project";
    private static final String BUILD_NBR_TITLE = "Failure causes per build for this project";

    private AbstractProject project;

    /**
     * Standard constructor.
     * @param project the parent project of this action
     */
    public ProjectGraphAction(AbstractProject project) {
        this.project = project;
    }

    @Override
    public String getDisplayName() {
        return null;
    }

    @Override
    public String getIconFileName() {
        return null;
    }

    @Override
    public String getUrlName() {
        return URL_NAME;
    }

    @Override
    public ModelObject getOwner() {
        return project;
    }

    @Override
    public GraphType[] getGraphTypes() {
        return new GraphType[] {GraphType.BAR_CHART_CAUSES, GraphType.PIE_CHART_CAUSES,
                GraphType.BAR_CHART_CATEGORIES, GraphType.PIE_CHART_CATEGORIES,
                GraphType.BAR_CHART_BUILD_NBRS, };
    }

    @Override
    public String getGraphsPageTitle() {
        return PAGE_TITLE;
    }

    @Override
    protected Graph getGraph(GraphType which, Date timePeriod,
            boolean hideManAborted, boolean forAllMasters,
            Map<String, String> rawReqParams) {
        GraphFilterBuilder filter = getDefaultBuilder(hideManAborted,
                timePeriod);
        switch (which) {
        case BAR_CHART_CAUSES_SMALL:
            return new BarChart(-1, GRAPH_WIDTH_SMALL, GRAPH_HEIGHT_SMALL,
                    project, filter, GRAPH_TITLE_CAUSES_SMALL, false);

        case BAR_CHART_CAUSES:
            return new BarChart(-1, DEFAULT_GRAPH_WIDTH, DEFAULT_GRAPH_HEIGHT,
                    project, filter, GRAPH_TITLE_CAUSES, false);

        case BAR_CHART_CATEGORIES:
            return new BarChart(-1, DEFAULT_GRAPH_WIDTH, DEFAULT_GRAPH_HEIGHT,
                    project, filter, GRAPH_TITLE_CATEGORIES, true);

        case BAR_CHART_BUILD_NBRS:
            return new BuildNbrStackedBarChart(-1, DEFAULT_GRAPH_WIDTH,
                    DEFAULT_GRAPH_HEIGHT, project, filter, NBR_OF_BUILDS,
                    BUILD_NBR_TITLE);

        case PIE_CHART_CAUSES:
            return new PieChart(-1, DEFAULT_GRAPH_WIDTH, DEFAULT_GRAPH_HEIGHT,
                    project, filter, GRAPH_TITLE_CAUSES, false);

        case PIE_CHART_CATEGORIES:
            return new PieChart(-1, DEFAULT_GRAPH_WIDTH, DEFAULT_GRAPH_HEIGHT,
                    project, filter, GRAPH_TITLE_CATEGORIES, true);

        default:
            break;
        }
        return null;
    }

    /**
     * Get a GraphFilterBuilder corresponding to the specified arguments, and the
     * project name set.
     * @param hideAborted Hide manually aborted
     * @param period The time period
     * @return A GraphFilterBuilder
     */
    private GraphFilterBuilder getDefaultBuilder(boolean hideAborted, Date period) {
        GraphFilterBuilder filter = new GraphFilterBuilder();
        filter.setProjectName(getProjectName());
        if (hideAborted) {
            filter.setExcludeResult(EXCLUDE_ABORTED);
        }
        filter.setSince(period);
        filter.setMasterName(BfaUtils.getMasterName());
        return filter;
    }

    @Override
    protected String getGraphCacheId(GraphType whichGraph, String reqTimePeriod,
            boolean hideAborted, boolean forAllMasters) {
        String id = null;
        if (whichGraph == GraphType.BAR_CHART_BUILD_NBRS) {
            id = getCacheIdForBuildNbrs(getProjectName());
        } else {
            id = getClass().getSimpleName() + ID_SEPARATOR
                    + whichGraph.getValue() + ID_SEPARATOR
                    + getProjectName() + ID_SEPARATOR
                    + reqTimePeriod + ID_SEPARATOR
                    + String.valueOf(hideAborted);
        }
        return id;
    }

    /**
     * Get the cache-id for a {@link BfaGraphAction#BAR_CHART_BUILD_NBRS} for the project name.
     * @param projectName The name of the project
     * @return The id
     */
    private static String getCacheIdForBuildNbrs(String projectName) {
        return ProjectGraphAction.class.getSimpleName() + ID_SEPARATOR
                + projectName + ID_SEPARATOR
                + GraphType.BAR_CHART_BUILD_NBRS.getValue();
    }

    /**
     * Get the name of the belonging project
     * @return The project name, or an empty string if the project is null
     */
    private String getProjectName() {
        if (project == null) {
            return "";
        }
        return project.getFullName();
    }

    /**
     * Invalidate the cache for the build number graph for the specified project.
     * @param project The project whose build number graph to invalidate
     */
    public static void invalidateBuildNbrGraphCache(AbstractProject project) {
        GraphCache.getInstance().invalidate(ProjectGraphAction.getCacheIdForBuildNbrs(project.getFullName()));
    }

    /**
     * Invalidate all graph caches for the specified project.
     * @param project The project whose graphs to invalidate
     */
    public static void invalidateProjectGraphCache(AbstractProject project) {
        Pattern projectPattern = Pattern.compile("^.*" + ID_SEPARATOR
                + project.getFullName() + ID_SEPARATOR + ".*$");
        GraphCache.getInstance().invalidateMatching(projectPattern);
    }
}
