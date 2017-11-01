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
streamsTags = {};
tagsArray = [];
selectedTags = [];
MULTIPLE_TAGS_COLOR = "#17becf";
MULTIPLE_TAGS_TEXT = "Multiple tags";
var showAllTags = d3.select("#showAllTags");
var selectTagButton = d3.select("#tagDialogBtn");

$( "#dialog" ).dialog({ 
	autoOpen: false,
	dialogClass: "no-close-dialog",
	modal: true,
	buttons: [ { text: "Done", 
		click: function(){
			$( this ).dialog( "close" );
		    clearInterval(run);
		    clearTableGraphs();

			d3.select("#graphLoading").style("display", "none");
			var selectedJob = d3.select("#jobs").node().value;
			getCounterMetricsForJob(renderGraph, selectedJob);
			startGraph(refreshInt);
		}
	}],
	})
	.css("font-size", "0.8em");

d3.select("#showTags")
.on("change", function() {
	// enable the showAllTags checkbox and the select individual tags button
	if (this.checked === true) {
		showAllTags.property("disabled", false);
		selectTagButton.property("disabled", false);
	} else {
		// uncheck the showAllTags if checked
		if (showAllTags.property("checked") === true) {
			showAllTags.property("checked", false);
		}
		
		showAllTags.property("disabled", true);
		selectTagButton.property("disabled", true);	
	}
    clearInterval(run);
    clearTableGraphs();

	d3.select("#graphLoading").style("display", "none");
	var selectedJob = d3.select("#jobs").node().value;
	getCounterMetricsForJob(renderGraph, selectedJob);
	startGraph(refreshInt);
});

showAllTags.on("change", function() {
	if (this.checked === true) {
		selectTagButton.property("disabled", true);
		// render the graph with all the tags shown
	} else {
		// enable it
		selectTagButton.property("disabled", false);
		// and select the first tag in the list?
		var values = $("#tags").val();
		if (!values) {
			// select the first one
			var firstOpt = $("#tags").find('option:eq(0)')
			firstOpt.prop("selected", true);
			var selectedValue = firstOpt.prop("value");
			$("#tags").val([selectedValue]);
		}
	}
    clearInterval(run);
    clearTableGraphs();

	d3.select("#graphLoading").style("display", "none");
	var selectedJob = d3.select("#jobs").node().value;
	getCounterMetricsForJob(renderGraph, selectedJob);
	startGraph(refreshInt);
});

selectTagButton.on("click", function() {
	$( "#dialog" ).dialog( "open")
});

showAllTagsSet = function() {
	return showAllTags.property("checked");
};

setAvailableTags = function(tags) {
	tags.forEach(function(t){
		if (!streamsTags[t]) {
			streamsTags[t] = t;
			tagsArray.push(streamsTags[t]);
		}
		
		if (!streamsTags[MULTIPLE_TAGS_TEXT]) {
			streamsTags[MULTIPLE_TAGS_TEXT] = MULTIPLE_TAGS_TEXT;
			tagsArray.push(streamsTags[MULTIPLE_TAGS_TEXT]);
		}
		
	});

	tagsArray.sort();
};

getSelectedTags = function() {
	// if showAll is clicked, return the entire tagsArray
	if (showAllTags.property("checked") === true) {
		return tagsArray;
	} else {
		// get the selected ones from the dialog
		var values = $("#tags").val();
		if (!values) {
			return [];
		}
		return values;	
	}
	
};

showTagDiv = function(bNewJob) {
	// check which layer is selected, if type is not tuple count, display tag info
	var layer = d3.select("#layers").property("value");
	var tagsDiv = d3.select("#tagsDiv");

	if (!tagsArray || tagsArray.length === 0) {
		tagsDiv.style("visibility", "hidden");
		return;
	}
	
	var showTagsChecked = $("#showTags").prop("checked");
	// check if the job has any tags
	if (tagsArray.length > 0 && showTagsChecked) {
		tagsDiv.style("visibility", "visible");
		
		var tagsSelect = d3.select("#tags");
		if (bNewJob) {
			tagsSelect.selectAll("option").remove();
			tagsArray.forEach(function(t){
				if (t !== MULTIPLE_TAGS_TEXT) {
			        tagsSelect
			        .append("option")
			        .text(t)
			        .attr("value", t);
				}
			});
			showAllTags.property("checked", true);
		} else {
			// make sure there are options, if not add them
			var bNoOptions = tagsSelect.selectAll("option").empty();
			tagsSelect.property("size", tagsArray.length);
			if (bNoOptions) {
				tagsArray.forEach(function(t){
					if (t !== MULTIPLE_TAGS_TEXT) {
				        tagsSelect
				        .append("option")
				        .text(t)
				        .attr("value", t);
					}
				});
			}
		}
		
	} 
	
};


