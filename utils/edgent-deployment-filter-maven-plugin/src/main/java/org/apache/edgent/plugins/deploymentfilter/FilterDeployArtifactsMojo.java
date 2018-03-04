/*
Licensed to the Apache Software Foundation (ASF) under one
or more contributor license agreements.  See the NOTICE file
distributed with this work for additional information
regarding copyright ownership.  The ASF licenses this file
to you under the Apache License, Version 2.0 (the
"License"); you may not use this file except in compliance
with the License.  You may obtain a copy of the License at

  http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing,
software distributed under the License is distributed on an
"AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
KIND, either express or implied.  See the License for the
specific language governing permissions and limitations
under the License.
*/
package org.apache.edgent.plugins.deploymentfilter;

import org.apache.edgent.plugins.deploymentfilter.model.FilterRule;
import org.apache.maven.artifact.Artifact;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;

import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;
import org.hamcrest.Matcher;
import org.hamcrest.beans.HasPropertyWithValue;
import org.hamcrest.core.AllOf;
import org.hamcrest.core.AnyOf;
import org.hamcrest.core.IsEqual;
import org.sonatype.aether.util.StringUtils;

import java.util.LinkedList;
import java.util.List;

/**
 * Goal which filters all 'test-jar' artifacts from installation and deployment.
 * The goal is added to the 'install' phase as this way it is executed after the install plugin,
 * but before the deploy, which is the phase we don't want the artifact to be handled.
 */
@Mojo( name = "filter-deploy-artifacts", defaultPhase = LifecyclePhase.INSTALL )
public class FilterDeployArtifactsMojo
    extends AbstractMojo
{

    @Parameter(defaultValue="${project}")
    private MavenProject project;

    @Parameter
    private List<FilterRule> filterRules;

    public void execute()
        throws MojoExecutionException
    {
        List<Matcher<? super Artifact>> filter = new LinkedList<Matcher<? super Artifact>>();
        for (FilterRule filterRule : filterRules) {
            List<Matcher<? super Artifact>> curFilter = new LinkedList<Matcher<? super Artifact>>();
            if(!StringUtils.isEmpty(filterRule.getType())) {
                curFilter.add(HasPropertyWithValue.hasProperty("type",
                    IsEqual.equalTo(filterRule.getType())));
            }
            if(!StringUtils.isEmpty(filterRule.getClassifier())) {
                curFilter.add(HasPropertyWithValue.hasProperty("classifier",
                    IsEqual.equalTo(filterRule.getClassifier())));
            }
            if(!StringUtils.isEmpty(filterRule.getGroupId())) {
                curFilter.add(HasPropertyWithValue.hasProperty("groupId",
                    IsEqual.equalTo(filterRule.getGroupId())));
            }
            if(!StringUtils.isEmpty(filterRule.getArtifactId())) {
                curFilter.add(HasPropertyWithValue.hasProperty("artifactId",
                    IsEqual.equalTo(filterRule.getArtifactId())));
            }
            if(!StringUtils.isEmpty(filterRule.getVersion())) {
                curFilter.add(HasPropertyWithValue.hasProperty("version",
                    IsEqual.equalTo(filterRule.getVersion())));
            }
            if(!StringUtils.isEmpty(filterRule.getScope())) {
                curFilter.add(HasPropertyWithValue.hasProperty("scope",
                    IsEqual.equalTo(filterRule.getScope())));
            }
            if(!curFilter.isEmpty()) {
                filter.add(new AllOf<Artifact>(curFilter));
            }
        }
        AnyOf<Artifact> matcher = new AnyOf<Artifact>(filter);

        // Find all 'test-jar' artifacts.
        // (This has to be done in separate loops in order to prevent
        // concurrent modification exceptions.
        List<Artifact> toBeRemovedArtifacts = new LinkedList<Artifact>();
        for(Artifact artifact : project.getAttachedArtifacts()) {
            if(matcher.matches(artifact)) {
                toBeRemovedArtifacts.add(artifact);
            }
        }

        // Remove all of them from the list of attached artifacts.
        if(!toBeRemovedArtifacts.isEmpty()) {
            for (Artifact toBeRemovedArtifact : toBeRemovedArtifacts) {
                getLog().info(" - Excluding artifact " + toBeRemovedArtifact.getArtifactId() +
                    " from deployment.");
                project.getAttachedArtifacts().remove(toBeRemovedArtifact);
            }
        }
    }
}
