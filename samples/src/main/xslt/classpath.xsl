<?xml version="1.0"?>
<!--

  Licensed to the Apache Software Foundation (ASF) under one or more
  contributor license agreements.  See the NOTICE file distributed with
  this work for additional information regarding copyright ownership.
  The ASF licenses this file to You under the Apache License, Version 2.0
  (the "License"); you may not use this file except in compliance with
  the License.  You may obtain a copy of the License at

      http://www.apache.org/licenses/LICENSE-2.0

  Unless required by applicable law or agreed to in writing, software
  distributed under the License is distributed on an "AS IS" BASIS,
  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
  See the License for the specific language governing permissions and
  limitations under the License.

-->
<xsl:stylesheet xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
                xmlns:grphml="http://graphml.graphdrawing.org/xmlns"
                xmlns:yFiles="http://www.yworks.com/xml/graphml"
                exclude-result-prefixes="grphml yFiles"
                version="1.0">

  <xsl:param name="groupId"/>
  <xsl:param name="artifactId"/>
  <xsl:param name="version"/>

  <xsl:output media-type="text" omit-xml-declaration="yes"/>
  <!--xsl:output indent="yes"/-->

  <xsl:template match="/grphml:graphml">
    <!-- Calculate the name of the current module. This will help find the starting point -->
    <xsl:variable name="nodeName" select="concat($groupId,':', $artifactId, ':jar:', $version)"/>
    <!-- Start outputting the classpath starting with the current modules node -->
    <xsl:variable name="rootNode" select="grphml:graph/grphml:node[grphml:data/yFiles:ShapeNode/yFiles:NodeLabel = $nodeName]"/>

    <xsl:call-template name="processNode">
      <xsl:with-param name="currentNode" select="$rootNode"/>
      <xsl:with-param name="addedNodeIds"/>
    </xsl:call-template>
  </xsl:template>

  <xsl:template name="processNode">
    <xsl:param name="currentNode"/>
    <xsl:param name="addedNodeIds"/>
    <xsl:variable name="currentNodeId" select="$currentNode/@id"/>
    <xsl:if test="not(contains($addedNodeIds, concat('|', $currentNodeId, '|')))">
      <xsl:variable name="newAddedNodeIds"><xsl:if test="string-length($addedNodeIds) = 0">|</xsl:if><xsl:value-of select="$addedNodeIds"/><xsl:value-of select="$currentNodeId"/>|</xsl:variable>
      <xsl:variable name="outgoingCompileDependencyIds" select="//grphml:edge[(@source = $currentNodeId) and ((grphml:data/yFiles:PolyLineEdge/yFiles:EdgeLabel = 'compile') or (grphml:data/yFiles:PolyLineEdge/yFiles:EdgeLabel = 'provided'))]/@target"/>
      <xsl:variable name="outgoingCompileDependencyName" select="$currentNode/grphml:data/yFiles:ShapeNode/yFiles:NodeLabel/text()"/>
      <xsl:variable name="outgoingCompileDependencyGroupId" select="substring-before($outgoingCompileDependencyName, ':')"/>
      <xsl:variable name="outgoingCompileDependencyArtifactId" select="substring-before(substring-after($outgoingCompileDependencyName, ':'), ':')"/>
      <xsl:variable name="outgoingCompileDependencyVersion" select="substring-before(substring-after(substring-after(substring-after($outgoingCompileDependencyName, ':'), ':'), ':'), ':')"/>
      <xsl:if test="not(starts-with($outgoingCompileDependencyGroupId, 'org.apache.edgent.'))">ext/</xsl:if><xsl:value-of select="concat($outgoingCompileDependencyArtifactId, '-', $outgoingCompileDependencyVersion, '.jar')"/>,<xsl:for-each select="$outgoingCompileDependencyIds">
        <xsl:variable name="currentDependentNodeId" select="."/>
        <xsl:variable name="currentDependentNode" select="//grphml:node[@id = $currentDependentNodeId]"/>
        <xsl:call-template name="processNode">
          <xsl:with-param name="currentNode" select="$currentDependentNode"/>
          <xsl:with-param name="addedNodeIds" select="$newAddedNodeIds"/>
        </xsl:call-template>
      </xsl:for-each>
    </xsl:if>
  </xsl:template>

</xsl:stylesheet>