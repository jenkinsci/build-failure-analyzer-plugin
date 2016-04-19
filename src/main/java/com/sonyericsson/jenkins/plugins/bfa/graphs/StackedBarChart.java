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

import hudson.model.Job;
import org.jfree.chart.ChartFactory;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.axis.CategoryAxis;
import org.jfree.chart.axis.CategoryLabelPositions;
import org.jfree.chart.axis.NumberAxis;
import org.jfree.chart.plot.CategoryPlot;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.data.category.DefaultCategoryDataset;


/**
 * Bar chart displaying failure causes.
 * Since it's a stacked bar chart, different failure causes for the same x-value
 * will get stacked in different colors on top of each other.
 *
 * @author Fredrik Persson &lt;fredrik6.persson@sonymobile.com&gt;
 *
 */
public abstract class StackedBarChart extends BFAGraph {

    /**
     * String used for creating "empty" x-values without y-values (if there are no failure causes).
     * This string is typically hidden from the legend.
     */
    protected static final String NO_FAILURE = "No failure for this build";

    /**
     * Default constructor.
     *
     * @param timestamp timestamp for this project graph, used for HTTP caching. Set to -1 if timestamp is not needed.
     * @param defaultW width of the graph in pixels
     * @param defaultH height of the graph in pixels
     * @param project the parent project of this graph
     * @param filter the filter used when fetching data for this graph
     * @param graphTitle The title of the graph
     */
    public StackedBarChart(long timestamp, int defaultW, int defaultH, Job project,
            GraphFilterBuilder filter, String graphTitle) {
        super(timestamp, defaultW, defaultH, project, filter, graphTitle);
    }

    @Override
    protected JFreeChart createGraph() {
        DefaultCategoryDataset dataset = createDataset();

        JFreeChart chart = ChartFactory.createStackedBarChart(getTitle(), getXAxisTitle(), getYAxisTitle(), dataset,
                PlotOrientation.VERTICAL, true, true, false);

        final CategoryPlot plot = chart.getCategoryPlot();
        int index = dataset.getRowIndex(NO_FAILURE);
        if (index >= 0) {
            plot.getRenderer().setSeriesVisibleInLegend(index, false);
        }
        CategoryAxis domainAxis = plot.getDomainAxis();

        domainAxis.setCategoryLabelPositions(CategoryLabelPositions.UP_90);
        domainAxis.setCategoryMargin(0);

        final NumberAxis rangeAxis = (NumberAxis)plot.getRangeAxis();
        rangeAxis.setStandardTickUnits(NumberAxis.createIntegerTickUnits());

        return chart;
    }

    /**
     * Creates the dataset used for plotting the graph.
     * @return created dataset
     */
    protected abstract DefaultCategoryDataset createDataset();

    /**
     * Getter for the graph title.
     * @return graph title
     */
    protected abstract String getTitle();

    /**
     * Getter for the Y-axis title.
     * @return title for the Y-axis
     */
    protected abstract String getYAxisTitle();

    /**
     * Getter for the X-axis title.
     * @return title for the X-axis
     */
    protected abstract String getXAxisTitle();



}
