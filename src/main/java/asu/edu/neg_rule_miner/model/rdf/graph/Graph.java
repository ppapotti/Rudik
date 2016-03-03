/*
Copyright (c) 2014, DIADEM Team (http://diadem.cs.ox.ac.uk)

All rights reserved.

Redistribution and use in source and binary forms, with or without
modification, are permitted provided that the following conditions are met: 

1. Redistributions of source code must retain the above copyright notice, this
   list of conditions and the following disclaimer. 
2. Redistributions in binary form must reproduce the above copyright notice,
   this list of conditions and the following disclaimer in the documentation
   and/or other materials provided with the distribution. 

THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND
ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE FOR
ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
(INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
(INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.

The views and conclusions contained in the software and documentation are those
of the authors and should not be interpreted as representing official policies, 
either expressed or implied, of the FreeBSD Project. 
 */

package asu.edu.neg_rule_miner.model.rdf.graph;

import java.util.Map;
import java.util.Set;

import org.apache.jena.ext.com.google.common.collect.Maps;
import org.apache.jena.ext.com.google.common.collect.Sets;

/**
 * 
 * 
 * @author Stefano Ortona (stefano dot ortona at cs dot ox dot ac dot uk) -
 *         Department of Computer Science - University of Oxford
 * 
 *         Utility class to model a RDF Graph.
 *         Each Node contains a label of type T
 */

public class Graph<T>{

	public Graph(){
		this.nodes=Sets.newConcurrentHashSet();
		this.neighbours=Maps.newConcurrentMap();
		this.edge2artificial = Maps.newConcurrentMap();
	}

	public Set<T> nodes;

	public Map<T,Set<Edge<T>>> neighbours;

	public Map<Edge<T>,Boolean> edge2artificial;

	public void addNode(T label){
		if(this.nodes.contains(label))
			return;
		this.nodes.add(label);
		Set<Edge<T>> neighbors = Sets.newHashSet();
		this.neighbours.put(label, neighbors);

	}

	public Set<T> getNodes(){
		return this.nodes;
	}

	public void addEdge(T source, T end, String label, boolean bidirectional){
		Edge<T> edge = new Edge<T>(source, end, label);
		this.addEdge(edge, bidirectional);
	}

	/**
	 * Return true if the operation completed succesfully
	 * @param edge
	 * @param bidirectional
	 * @return
	 */
	public boolean addEdge(Edge<T> edge,boolean bidirectional){
		//cannot add an edge if neither node source nor source edge are in the graph
		if(!this.nodes.contains(edge.getNodeSource())||!this.nodes.contains(edge.getNodeEnd())){
			return false;
		}

		T source = edge.getNodeSource();
		Set<Edge<T>> neighbours = this.neighbours.get(source);

		neighbours.add(edge);
		this.edge2artificial.put(edge, false);

		if(bidirectional){
			T end = edge.getNodeEnd();
			Edge<T> inverseEdge = new Edge<T>(end, source, edge.getLabel());
			neighbours = this.neighbours.get(end);
			//do not add if inverse edge has been already added
			if(!neighbours.contains(inverseEdge)){
				neighbours.add(inverseEdge);
				this.edge2artificial.put(inverseEdge, true);
			}
		}

		return true;
	}

	public Set<Edge<T>> getNeighbours(T node){
		return this.neighbours.get(node);
	}


	public boolean isArtifical(Edge<T> e){
		Boolean isArtifical = this.edge2artificial.get(e);
		return isArtifical!=null ? isArtifical : false;

	}



}
