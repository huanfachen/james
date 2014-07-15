//  Copyright 2014 Herman De Beukelaer
//
//  Licensed under the Apache License, Version 2.0 (the "License");
//  you may not use this file except in compliance with the License.
//  You may obtain a copy of the License at
//
//      http://www.apache.org/licenses/LICENSE-2.0
//
//  Unless required by applicable law or agreed to in writing, software
//  distributed under the License is distributed on an "AS IS" BASIS,
//  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
//  See the License for the specific language governing permissions and
//  limitations under the License.

package org.jamesframework.core.util;

import org.jamesframework.core.problems.Problem;
import org.jamesframework.core.problems.solutions.Solution;
import org.jamesframework.core.search.LocalSearch;

/**
 * Interface of a local search factory used to create any local search.
 * 
 * @param <SolutionType> solution type of created local searches, required to extend {@link Solution}
 * @author <a href="mailto:herman.debeukelaer@ugent.be">Herman De Beukelaer</a>
 */
public interface LocalSearchFactory<SolutionType extends Solution> {
    
    /**
     * Create a local search, given the problem to solve.
     * 
     * @param problem problem to solve
     * @return neighbourhood search
     */
    public LocalSearch<SolutionType> create(Problem<SolutionType> problem);

}