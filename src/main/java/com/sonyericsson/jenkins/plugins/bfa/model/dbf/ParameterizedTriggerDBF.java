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

package com.sonyericsson.jenkins.plugins.bfa.model.dbf;

import hudson.Extension;
import hudson.model.Action;
import hudson.model.Run;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Get downstream builds for the Parameterized Trigger Plugin.
 * <p/>
 * We want to avoid having dependencies to other plugins thus using reflection.
 *
 * @author Jan-Olof Sivtoft
 */
@Extension
public class ParameterizedTriggerDBF extends DownstreamBuildFinder {

    private static final Logger logger = Logger.
            getLogger(ParameterizedTriggerDBF.class.getName());

    /**
     * Return a list of all downstream builds originating from provided build.
     * Getting build originating from the usage of the parameterized trigger
     * plugin.
     *
     * @param build get the downstream build(s) relative this build
     * @return a list with downstream builds
     */
    @Override
    public List<Run<?, ?>> getDownstreamBuilds(
            final Run build) {

        if (build == null) {
            return EMPTY;
        }

        // The action class to examine for downstream builds
        String className =
                "hudson.plugins.parameterizedtrigger.BuildInfoExporterAction";
        // The method returning triggered builds
        String methodName = "getTriggeredBuilds";

        try {
            // The BuildInfoExporterAction class
            Class clazz = Class.forName(className);
            //  The BuildInfoExporterAction instance
            Action action = build.getAction(clazz);

            // Check triggered builds
            if (action != null) {
                Method method = clazz.getMethod(methodName);
                return (List)method.invoke(action);
            } else {
                return EMPTY;
            }
            // All exceptions below are due to using reflection, thus silently
            // return an empty list as it doesn't exists.
        } catch (ClassNotFoundException e) {
            // Either the plugin doesn't exist, silently exit.
            // Or className is wrong - treated as it doesn't exist
            logger.log(Level.FINER, "Class " + className + " not installed. "
                    + "Can't get downstream builds");
            return EMPTY;
        } catch (NoSuchMethodException e) {
            logger.log(Level.FINER, "Method " + methodName
                    + " doesn't exists in " + "class " + className + ".");
            return EMPTY;
        } catch (InvocationTargetException e) {
            return EMPTY;
        } catch (IllegalAccessException e) {
            return EMPTY;
        }
    }
}
