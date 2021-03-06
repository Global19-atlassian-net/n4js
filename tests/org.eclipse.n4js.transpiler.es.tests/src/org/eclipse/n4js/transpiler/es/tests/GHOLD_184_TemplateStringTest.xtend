/**
 * Copyright (c) 2016 NumberFour AG.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *   NumberFour AG - Initial API and implementation
 */
package org.eclipse.n4js.transpiler.es.tests

import org.eclipse.emf.common.util.URI
import org.eclipse.n4js.N4JSInjectorProviderWithMockProject
import org.eclipse.n4js.n4JS.Script
import org.eclipse.xtext.testing.InjectWith
import org.eclipse.xtext.testing.XtextRunner
import org.junit.FixMethodOrder
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.MethodSorters

/**
 */
@RunWith(XtextRunner)
@InjectWith(N4JSInjectorProviderWithMockProject)
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
class GHOLD_184_TemplateStringTest extends AbstractTranspilerTest {

	@Test
	def void test_Compile() throws Throwable{

		val script = '''
			 var foo = `\n party`;
			 var bar = "\n party";
			 console.log(foo === bar); //should be true but is false

			 var foo2 = `"${bar}"`
			 var bar2 = '"\n party"';
		''';

		val moduleWrapped = '''
			// Generated by N4JS transpiler; for copyright see original N4JS source file.
			
			import 'n4js-runtime'
			
			var foo = "\n party";
			var bar = "\n party";
			console.log(foo === bar);
			var foo2 = ("\"" + bar + "\"");
			var bar2 = '"\n party"';
		''';

	 	// Prepare ResourceSet to contain exportedScript:
		val resSet = installExportedScript;

   		val Script scriptNode = script.parse(URI.createURI("src/A.n4js"),resSet);
		scriptNode.resolveLazyRefs;

		assertCompileResult(scriptNode,moduleWrapped);
	}

}
