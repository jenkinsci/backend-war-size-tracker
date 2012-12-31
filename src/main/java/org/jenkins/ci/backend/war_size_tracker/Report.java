/*
 * The MIT License
 * 
 * Copyright (c) 2011, Jesse Farinacci
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

package org.jenkins.ci.backend.war_size_tracker;

import java.io.IOException;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.apache.commons.httpclient.Header;
import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.methods.HeadMethod;
import org.apache.commons.lang.StringUtils;
import org.apache.velocity.VelocityContext;

/**
 * @author <a href="mailto:jieryn@gmail.com">Jesse Farinacci</a>
 * @since 1.0
 */
public final class Report {
    private static final Logger LOG = Logger.getLogger(Report.class.getName());

    protected static String generateReport() throws Exception {
        final List<Jenkins> sizes = new LinkedList<Jenkins>();

        for (int version = 60; version <= 600; version++) {
            try {
                final Jenkins jenkins = getJenkins("1." + version);
                if (jenkins != null) {
                    sizes.add(jenkins);
                }
            }

            catch (final IOException e) {
                LOG.log(Level.WARNING, "Error processing version " + version, e);
            }
        }

        return generateReport(sizes);
    }

    protected static String generateReport(final List<Jenkins> wars)
            throws Exception {
        final VelocityContext context = new VelocityContext();
        context.put("now", new Date());
        context.put("wars", wars);

        return VelocityUtils.interpolate(
                VelocityUtils.getVelocityTemplate("report.vm"), context);
    }

    protected static Jenkins getJenkins(final String version)
            throws IOException {
        final HttpClient client = new HttpClient();
        final HeadMethod head = new HeadMethod(
                "http://mirrors.jenkins-ci.org/war/" + version + "/"
                        + getJenkinsWarName(version));

        final int status = client.executeMethod(head);

        if (status < 400) {
            final Header contentLength = head
                    .getResponseHeader("Content-Length");

            if (contentLength != null) {
                return new Jenkins(version, Long.valueOf(contentLength
                        .getValue()));
            }
        }

        return null;
    }

    protected static String getJenkinsWarName(final String version) {
        // handle sanity check
        if (StringUtils.isEmpty(version)) {
            return "hudson.war";
        }

        // handle really old versions
        if (version.length() == 4) {
            return "hudson.war";
        }

        // handle jenkins rename: 1.396 and up are jenkins.war
        final int rc = "1.396".compareTo(version);

        if (rc <= 0) {
            return "jenkins.war";
        }

        return "hudson.war";
    }
}
