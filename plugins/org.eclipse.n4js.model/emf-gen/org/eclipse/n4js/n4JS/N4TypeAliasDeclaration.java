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
package org.eclipse.n4js.n4JS;

import org.eclipse.n4js.ts.types.TypeAlias;

/**
 * <!-- begin-user-doc -->
 * A representation of the model object '<em><b>N4 Type Alias Declaration</b></em>'.
 * <!-- end-user-doc -->
 *
 * <p>
 * The following features are supported:
 * </p>
 * <ul>
 *   <li>{@link org.eclipse.n4js.n4JS.N4TypeAliasDeclaration#isCyclic <em>Cyclic</em>}</li>
 * </ul>
 *
 * @see org.eclipse.n4js.n4JS.N4JSPackage#getN4TypeAliasDeclaration()
 * @model
 * @generated
 */
public interface N4TypeAliasDeclaration extends N4TypeDeclaration, GenericDeclaration, TypedElement {
	/**
	 * Returns the value of the '<em><b>Cyclic</b></em>' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * <!-- begin-model-doc -->
	 *  Set in {@code TypeRefProcessor} and used in {@code N4JSTypeAliasValidator}.
	 * <!-- end-model-doc -->
	 * @return the value of the '<em>Cyclic</em>' attribute.
	 * @see #setCyclic(boolean)
	 * @see org.eclipse.n4js.n4JS.N4JSPackage#getN4TypeAliasDeclaration_Cyclic()
	 * @model unique="false" transient="true"
	 * @generated
	 */
	boolean isCyclic();

	/**
	 * Sets the value of the '{@link org.eclipse.n4js.n4JS.N4TypeAliasDeclaration#isCyclic <em>Cyclic</em>}' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @param value the new value of the '<em>Cyclic</em>' attribute.
	 * @see #isCyclic()
	 * @generated
	 */
	void setCyclic(boolean value);

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @model kind="operation" unique="false"
	 * @generated
	 */
	TypeAlias getDefinedTypeAsTypeAlias();

} // N4TypeAliasDeclaration
