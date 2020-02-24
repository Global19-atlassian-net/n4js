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
package org.eclipse.n4js.ide.server.codeActions;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.emf.common.util.URI;
import org.eclipse.lsp4j.CodeAction;
import org.eclipse.lsp4j.CodeActionContext;
import org.eclipse.lsp4j.CodeActionParams;
import org.eclipse.lsp4j.Command;
import org.eclipse.lsp4j.Diagnostic;
import org.eclipse.lsp4j.TextDocumentIdentifier;
import org.eclipse.lsp4j.TextEdit;
import org.eclipse.lsp4j.WorkspaceEdit;
import org.eclipse.lsp4j.jsonrpc.messages.Either;
import org.eclipse.n4js.ide.server.commands.N4JSCommandService;
import org.eclipse.n4js.ide.xtext.server.DiagnosticIssueConverter;
import org.eclipse.n4js.ide.xtext.server.XLanguageServerImpl;
import org.eclipse.n4js.ide.xtext.server.XProjectManager;
import org.eclipse.n4js.ide.xtext.server.XWorkspaceManager;
import org.eclipse.xtext.ide.server.UriExtensions;
import org.eclipse.xtext.ide.server.codeActions.ICodeActionService2;
import org.eclipse.xtext.service.OperationCanceledManager;
import org.eclipse.xtext.validation.Issue;

import com.google.common.base.Strings;
import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;
import com.google.inject.Inject;
import com.google.inject.Injector;
import com.google.inject.Singleton;

/**
 * Infrastructure for LSP code actions
 */
@SuppressWarnings("restriction")
@Singleton
public class N4JSCodeActionService implements ICodeActionService2 {

	final Class<?>[] quickfixProviders = { N4JSQuickfixProvider.class };

	private static class TextEditCollector implements ICodeActionAcceptor {
		private final Map<String, List<TextEdit>> allEdits;

		private TextEditCollector() {
			this.allEdits = new HashMap<>();
		}

		@Override
		public void acceptQuickfixCommand(QuickfixContext context, String title, String commandID,
				Object... arguments) {

			throw new UnsupportedOperationException(
					"TODO implement composite post-text-edit-apply actions if necessary");
		}

		@Override
		public void acceptQuickfixCodeAction(QuickfixContext context, String title, List<TextEdit> textEdits) {
			String uriString = context.options.getCodeActionParams().getTextDocument().getUri();
			allEdits.computeIfAbsent(uriString, ignore -> new ArrayList<>()).addAll(textEdits);
		}
	}

	private static class QuickFixImplementation {
		final Object instance;
		final Method method;
		final String id;
		final boolean multiFix;

		private QuickFixImplementation(Object instance, final Method method, boolean multiFix) {
			this.instance = instance;
			this.method = method;
			this.id = method.getName();
			this.multiFix = multiFix;
		}

		void compute(String code, Options options, ICodeActionAcceptor acceptor) {
			QuickfixContext context = new QuickfixContext(code, options);

			try {
				method.invoke(instance, context, acceptor);
			} catch (Exception e) {
				e.printStackTrace();
			}
		}

	}

	private static class MultiQuickfixAcceptor implements ICodeActionAcceptor {
		final String quickfixId;
		final CodeActionAcceptor acceptor;

		private MultiQuickfixAcceptor(String quickfixId, CodeActionAcceptor acceptor) {
			this.quickfixId = quickfixId;
			this.acceptor = acceptor;
		}

		@Override
		public void acceptQuickfixCodeAction(QuickfixContext context, String title, List<TextEdit> textEdits) {
			acceptor.acceptQuickfixCodeAction(context, title, textEdits);
			acceptor.acceptQuickfixCommand(context, title + " (entire file)",
					N4JSCommandService.COMPOSITE_FIX_FILE,
					title,
					context.issueCode,
					quickfixId,
					context.options.getCodeActionParams());
			acceptor.acceptQuickfixCommand(context, title + " (entire project)",
					N4JSCommandService.COMPOSITE_FIX_PROJECT,
					title,
					context.issueCode,
					quickfixId,
					context.options.getCodeActionParams());
		}

		@Override
		public void acceptQuickfixCommand(QuickfixContext context, String title, String commandID,
				Object... arguments) {

			acceptor.acceptQuickfixCommand(context, title, commandID, arguments);
		}
	}

	@Inject
	private XLanguageServerImpl languageServer;

	@Inject
	private XWorkspaceManager workspaceManager;

	@Inject
	private DiagnosticIssueConverter diagnosticIssueConverter;

	@Inject
	private UriExtensions uriExtensions;

	@Inject
	private OperationCanceledManager cancelManager;

	private final Multimap<String, QuickFixImplementation> quickfixMap = HashMultimap.create();

	/**
	 * Using reflection, all classes of {@link #quickfixProviders} are inspected to find all methods that are annotated
	 * with {@link Fix}.
	 */
	@Inject
	public void init(Injector injector) {
		for (Class<?> quickFixProvider : quickfixProviders) {
			Object qpInstance = injector.getInstance(quickFixProvider);
			Method[] methods = quickFixProvider.getMethods();
			for (Method method : methods) {
				int modifiers = method.getModifiers();
				if (Modifier.isPublic(modifiers) && !Modifier.isStatic(modifiers)) {
					Fixes fixes = method.getAnnotation(Fixes.class);
					if (fixes != null) {
						for (Fix fix : fixes.value()) {
							acceptFix(fix, qpInstance, method);
						}
					}
					Fix fix = method.getAnnotation(Fix.class);
					if (fix != null) {
						acceptFix(fix, qpInstance, method);
					}
				}
			}
		}
	}

	private void acceptFix(Fix fix, Object instance, Method method) {
		String issueCode = fix.value();
		if (!Strings.isNullOrEmpty(issueCode)) {
			QuickFixImplementation impl = new QuickFixImplementation(instance, method, fix.multiFix());
			quickfixMap.put(issueCode, impl);
		}
	}

	@Override
	public List<Either<Command, CodeAction>> getCodeActions(Options options) {
		CodeActionAcceptor acceptor = new CodeActionAcceptor();

		List<Diagnostic> diagnostics = null;
		if (options.getCodeActionParams() != null && options.getCodeActionParams().getContext() != null) {
			diagnostics = options.getCodeActionParams().getContext().getDiagnostics();
		}
		if (diagnostics == null) {
			diagnostics = Collections.emptyList();
		}

		for (Diagnostic diag : diagnostics) {
			cancelManager.checkCanceled(options.getCancelIndicator());
			findQuickfixes(diag.getCode(), options, acceptor);
		}

		cancelManager.checkCanceled(options.getCancelIndicator());
		return acceptor.getList();
	}

	/** Finds quick-fixes for the given issue code and adds these quick-fixes to the acceptor iff available. */
	public void findQuickfixes(String code, Options options, CodeActionAcceptor acceptor) {
		for (QuickFixImplementation qfix : quickfixMap.get(code)) {
			ICodeActionAcceptor accTmp = qfix.multiFix ? new MultiQuickfixAcceptor(qfix.id, acceptor) : acceptor;
			qfix.compute(code, options, accTmp);
		}
	}

	/**
	 * Applies all fixes of the same kind to the current file.
	 */
	public WorkspaceEdit applyToFile(String code, String id, Options options) {
		WorkspaceEdit result = new WorkspaceEdit();
		QuickFixImplementation quickfix = findOriginatingQuickfix(code, id);
		if (quickfix == null) {
			return result;
		}
		TextEditCollector collector = new TextEditCollector();
		XProjectManager projectManager = getCurrentProject(options);
		String uriString = options.getCodeActionParams().getTextDocument().getUri();
		URI uri = uriExtensions.toUri(uriString);
		Collection<Issue> issues = projectManager.getProjectStateHolder().getValidationIssues().get(uri);
		for (Issue issue : issues) {
			if (code.equals(issue.getCode())) {
				Options newOptions = copyOptions(options, options.getCodeActionParams().getTextDocument(), issue);
				quickfix.compute(code, newOptions, collector);
			}
		}
		result.setChanges(collector.allEdits);
		return result;
	}

	/**
	 * Applies all fixes of the same kind to the current project.
	 */
	public WorkspaceEdit applyToProject(String code, String id, Options options) {
		WorkspaceEdit result = new WorkspaceEdit();
		QuickFixImplementation quickfix = findOriginatingQuickfix(code, id);
		if (quickfix == null) {
			return result;
		}
		TextEditCollector collector = new TextEditCollector();
		XProjectManager projectManager = getCurrentProject(options);
		Map<URI, Collection<Issue>> validationIssues = projectManager.getProjectStateHolder().getValidationIssues();

		for (Map.Entry<URI, Collection<Issue>> entry : validationIssues.entrySet()) {
			URI uri = entry.getKey();
			Collection<Issue> issues = entry.getValue();
			TextDocumentIdentifier docIdentifier = new TextDocumentIdentifier(uriExtensions.toUriString(uri));
			for (Issue issue : issues) {
				if (code.equals(issue.getCode())) {
					Options newOptions = copyOptions(options, docIdentifier, issue);
					quickfix.compute(code, newOptions, collector);
				}
			}
		}
		result.setChanges(collector.allEdits);
		return result;
	}

	private Options copyOptions(Options options, TextDocumentIdentifier docIdentifier, Issue issue) {
		Diagnostic diagnostic = diagnosticIssueConverter.toDiagnostic(issue, workspaceManager);
		CodeActionContext context = new CodeActionContext(Collections.singletonList(diagnostic));
		CodeActionParams codeActionParams = new CodeActionParams(docIdentifier, diagnostic.getRange(), context);
		Options newOptions = languageServer.toOptions(codeActionParams, options.getCancelIndicator());
		return newOptions;
	}

	private XProjectManager getCurrentProject(Options options) {
		String uriString = options.getCodeActionParams().getTextDocument().getUri();
		URI uri = uriExtensions.toUri(uriString);
		XProjectManager projectManager = workspaceManager.getProjectManager(uri);
		return projectManager;
	}

	private QuickFixImplementation findOriginatingQuickfix(String code, String id) {
		for (QuickFixImplementation quickfix : quickfixMap.get(code)) {
			if (quickfix.id.equals(id)) {
				return quickfix;
			}
		}
		return null;
	}

}