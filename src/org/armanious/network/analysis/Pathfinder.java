package org.armanious.network.analysis;

import org.armanious.graph.Path;

public interface Pathfinder<T> {
	
	Path<T> findPath(T src, T dst);

}
