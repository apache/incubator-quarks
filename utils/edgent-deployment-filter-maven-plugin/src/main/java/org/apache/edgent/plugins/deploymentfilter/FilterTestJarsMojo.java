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

import org.apache.maven.artifact.Artifact;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;

import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;

import java.util.LinkedList;
import java.util.List;

/**
 * Goal which filters all 'test-jar' artifacts from installation and deployment.
 * The goal is added to the 'install' phase as this way it is executed after the install plugin,
 * but before the deploy, which is the phase we don't want the artifact to be handled.
 */
@Mojo( name = "filter-test-jars", defaultPhase = LifecyclePhase.INSTALL )
public class FilterTestJarsMojo
    extends AbstractMojo
{

    @Parameter(defaultValue="${project}")
    private MavenProject project;

    public void execute()
        throws MojoExecutionException
    {
        // Find all 'test-jar' artifacts.
        // (This has to be done in separate loops in order to prevent
        // concurrent modification exceptions.
        List<Artifact> toBeRemovedArtifacts = new LinkedList<Artifact>();
        for(Artifact artifact : project.getAttachedArtifacts()) {
            if("test-jar".equals(artifact.getType())) {
                toBeRemovedArtifacts.add(artifact);
            }
        }

        // Remove all of them from the list of attached artifacts.
        if(!toBeRemovedArtifacts.isEmpty()) {
            for (Artifact toBeRemovedArtifact : toBeRemovedArtifacts) {
                getLog().info(" - Excluding test-jar artifact " + toBeRemovedArtifact.getArtifactId() +
                    " from deployment.");
                project.getAttachedArtifacts().remove(toBeRemovedArtifact);
            }
        }
    }
}
