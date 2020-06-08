/**
 * Copyright (c) 2020 NumberFour AG.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *   NumberFour AG - Initial API and implementation
 */
package org.eclipse.n4js.xtext.workspace;

import java.util.function.Function;

import org.eclipse.emf.common.util.URI;
import org.eclipse.xtext.resource.impl.ProjectDescription;
import org.eclipse.xtext.workspace.IWorkspaceConfig;

/**
 * Extension of {@link IWorkspaceConfig} to modify workspace during operation
 */
@SuppressWarnings("restriction")
public interface XIWorkspaceConfig extends IWorkspaceConfig {

	/** @return base directory of workspace */
	URI getPath();

	/** Updates internal data based on changes of the given resource */
	WorkspaceChanges update(URI changedResource, Function<String, ProjectDescription> pdProvider);

}