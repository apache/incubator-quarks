// d3.legend.js 
// (C) 2012 ziggy.jonsson.nyc@gmail.com
// MIT licence

(function() {
d3.legend = function(g, chartSvg, pItems, legendTitle) {
  g.each(function() {
    var g= d3.select(this);
    var items = {};
    var svg = !chartSvg ? d3.select(g.property("nearestViewportElement")) : chartSvg;
    var isTupleFlowLegend = false;
        
    var	legendPadding = g.attr("data-style-padding") || 5,
        lTitleItems = g.selectAll(".legend-title-items").data([true]),
        lb = g.selectAll(".legend-box").data([true]),
        li = g.selectAll(".legend-items").data([true])

    lTitleItems.enter().append("g").classed("legend-title-items", true)
    lb.enter().append("rect").classed("legend-box",true)
    liG = li.enter().append("g").classed("legend-items",true)

    if (pItems) {
    	pItems.forEach(function(p){
    		if (p.idx !== undefined) {
    			items[p.name] = {color: p.fill, idx: p.idx};
    			isTupleFlowLegend = true;
    		} else {
    			items[p.name] = {color: p.fill};
    			isTupleFlowLegend = false;
    		}
    	});
    } else {
	    svg.selectAll("[data-legend]").each(function() {
	        var self = d3.select(this);
	        items[self.attr("data-legend")] = {
	          pos : self.attr("data-legend-pos") || this.getBBox().y,
	          color : self.attr("data-legend-color") != undefined ? self.attr("data-legend-color") : self.style("fill") != 'none' ? self.style("fill") : self.style("stroke") 
	        }
	      });
    }

    if (isTupleFlowLegend)
	   items = d3.entries(items).sort(
			   function(a,b) {
				   if (a.value.idx < b.value.idx) {
					   return -1;
				   } else if (a.value.idx > b.value.idx) {
					   return 1;
				   } else {
					   return 0;
				   }
			   });
    else  {
    	
	    items = d3.entries(items).sort(
	    		function(a,b) {
	    			if (a.key < b.key) {
	    				return -1;
	    			} else if (a.key > b.key) {
	    				return 1;
	    			} else {
	    				return 0;
	    			}
	    });
    }
    
    li.selectAll("text")
        .data(items,function(d) { 
        	return d.key}
        )
        .call(function(d) { d.enter().append("text")})
        .call(function(d) { d.exit().remove()})
        .attr("y",function(d,i) { return i+"em"})
        .attr("x","1em")
        .text(function(d) {
        	return d.key;
        	})
    
    var legendOpacity = 0.7;
    if (legendTitle && legendTitle === "Stream tags") {
    	legendOpacity = 1.0;
	    li.selectAll("rect")
        .data(items,function(d) { 
        	return d.key}
        )
        .call(function(d) { d.enter().append("rect")})
        .call(function(d) { d.exit().remove()})
        .attr("y", function(d,i) { 
        	return i-0.75+ "em"}) 
        .attr("width", 8)                          
        .attr("height", 8)
        .style("fill",function(d) {
        	return d.value.color
        	})
        .style("stroke", "none")
        .style("fill-opacity", legendOpacity);
    } else if (legendTitle && legendTitle === "Oplet kind"){
    	
    	liG.selectAll("g")
    		.data(items, function(d) { 
    			return d.key;
    		})
    		.enter()
    		.append(function(d) {
    			if (d.key.toUpperCase().startsWith("COUNTEROP")) {
    	  			return document.createElementNS(d3.ns.prefix.svg, 'rect');
    	  		} else {
    	  			return document.createElementNS(d3.ns.prefix.svg, 'circle');
    	  		}
    		});

    	var count = 0;
    	li.selectAll("rect")
    	.attr("x", -3)
        .attr("y", function(d,i) {
        	count++;
        	return i-0.75+ "em"}) 
        .attr("width", 8)                          
        .attr("height", 8)
        .style("fill",function(d) {
        	return d.value.color
        	})
        .style("stroke", "none")
        .style("fill-opacity", legendOpacity);
    	
	    li.selectAll("circle")
        .attr("cy",function(d,i) {
        	return (i+count)-0.25+"em"})
        .attr("cx",0)
        .attr("r","0.4em")
        .style("fill",function(d) {
        	return d.value.color
        	})
         .style("fill-opacity", legendOpacity);
    	
    } else {
	    li.selectAll("circle")
	        .data(items,function(d) { 
	        	return d.key}
	        )
	        .call(function(d) { d.enter().append("circle")})
	        .call(function(d) { d.exit().remove()})
	        .attr("cy",function(d,i) { return i-0.25+"em"})
	        .attr("cx",0)
	        .attr("r","0.4em")
	        .style("fill",function(d) {
	        	return d.value.color
	        	})
	         .style("fill-opacity", legendOpacity);
    }
    // Reposition and resize the box
    var lbbox = li[0][0].getBBox();
    lb.attr("x",(lbbox.x-legendPadding))
        .attr("y",(lbbox.y-legendPadding))
        .attr("height",(lbbox.height+2*legendPadding))
        .attr("width",(lbbox.width+2*legendPadding));
    
    lTitleItems.attr("x", 0)
    	.attr("y", (lbbox.y - legendPadding - 15))
    	.attr("height",15)
        .attr("width",(lbbox.width+2*legendPadding));
        if (legendTitle) {
	         lTitleItems.selectAll("text")
	         .data([""],function(d) { 
	         	return legendTitle;
	          })
	         .call(function(d) { d.enter().append("text")})
	         .call(function(d) { d.exit().remove()})
	         .attr("y",function(d,i) { return "-2em"})
	         .attr("x",(lbbox.x-legendPadding))
	         .text(function(d) { 
	         	return legendTitle;
	         	});
        }
    	
  })
  return g
}
})()
