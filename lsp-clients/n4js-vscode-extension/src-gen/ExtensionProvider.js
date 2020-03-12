// Generated by N4JS transpiler; for copyright see original N4JS source file.

import 'n4js-runtime'
import * as n4jscli from 'n4js-cli'
import * as jreProvider from 'n4js-cli/src-gen/JreProvider'
import * as net from 'net'

const LSP_SYNC_MESSAGE = 'Listening for LSP clients';
const CHANNEL_NAME = 'N4JS Language Server';
const PORT = 5007;
const TIMEOUT = 1000;
let n4jscProcess;
let outputAppender;
function getOutputAppender(outputChannel) {
	if (!outputAppender) {
		outputAppender = (text)=>outputChannel.append(text.toString());
	}
	return outputAppender;
}
export function getActivate(vscode, vscodeLC) {
	return (context)=>{
		let outputChannel = vscode.window.createOutputChannel(CHANNEL_NAME);
		let serverOptions = async()=>{
			outputChannel.appendLine('Start LSP extension');
			let writer;
			let reader;
			const socket = await connectToRunningN4jsLspServer(PORT, outputChannel);
			if (socket) {
				writer = socket;
				reader = socket;
			} else {
				outputChannel.appendLine('Start new N4JS LSP server.');
				await startN4jsLspServerAndConnect(PORT, vscode, outputChannel);
				writer = n4jscProcess.stdin;
				reader = n4jscProcess.stdout;
			}
			let result = {
				writer: writer,
				reader: reader,
				process: n4jscProcess,
				detached: true
			};
			return Promise.resolve(result);
		};
		let clientOptions = {
			documentSelector: [
				{
					scheme: 'file',
					language: 'n4js'
				},
				{
					scheme: 'file',
					language: 'n4js.json'
				},
				{
					scheme: 'n4scheme',
					language: 'n4js'
				},
				{
					scheme: 'untitled',
					language: 'n4js'
				}
			],
			synchronize: {
				fileEvents: vscode.workspace.createFileSystemWatcher('{/**/*.+(n4js|n4jsd|n4jsx|n4idl),/**/package.json}')
			},
			outputChannel: outputChannel
		};
		let lc = new vscodeLC.LanguageClient(CHANNEL_NAME, serverOptions, clientOptions, true);
		lc.onReady().then(()=>{
			const requestType = new vscodeLC.RequestType('n4/documentContents');
			const textDocumentContentProvider = {
				provideTextDocumentContent: (uri, token)=>{
					return lc.sendRequest(requestType, {
						uri: uri.toString()
					}, token).then((v)=>{
						return v || '';
					});
				}
			};
			context.subscriptions.push(vscode.workspace.registerTextDocumentContentProvider('n4scheme', textDocumentContentProvider));
		});
		let disposableLangClient = lc.start();
		context.subscriptions.push(vscode.workspace.onDidChangeConfiguration((e)=>{
			if (e.affectsConfiguration('n4js.traceOutput', {
				languageId: 'n4js'
			})) {
				setOutputAppenders(vscode, outputChannel);
			}
		}));
		context.subscriptions.push(disposableLangClient);
	};
}
export function getDeactivate(vscode, vscodeLC) {
	return ()=>{
		if (!n4jscProcess) {
			return undefined;
		}
		n4jscProcess.kill();
		return new Promise((resolve, reject)=>{
			n4jscProcess.on('exit', ()=>{
				resolve();
			});
		});
	};
}
function setOutputAppenders(vscode, outputChannel) {
	const outputAppender = getOutputAppender(outputChannel);
	const traceOutput = vscode.workspace.getConfiguration('').get('n4js.traceOutput');
	switch(traceOutput) {
		case 'off':
			n4jscProcess.stdout.removeListener('data', outputAppender);
			n4jscProcess.stderr.removeListener('data', outputAppender);
			break;
		case 'verbose':
			n4jscProcess.stdout.on('data', outputAppender);
			n4jscProcess.stderr.on('data', outputAppender);
			break;
	}
}
function getJavaVMXmxSetting(vscode) {
	const vmXmx = vscode.workspace.getConfiguration('').get('n4js.jreVmXmx');
	switch(vmXmx) {
		case 'n4jsc default':
			return undefined;
		case 'jre default':
			return null;
		default:
			return vmXmx;
	}
}
async function startN4jsLspServerAndConnect(port, vscode, outputChannel) {
	let logFn = (text)=>outputChannel.appendLine(text);
	await jreProvider.ensureJRE(logFn);
	const env = Object.assign({
		NODEJS_PATH: process.argv[0]
	}, process.env);
	const spawnOptions = {
		env: env
	};
	const n4jscOptions = {
		stdio: true
	};
	const vmOptions = {
		xmx: getJavaVMXmxSetting(vscode)
	};
	n4jscProcess = n4jscli.n4jscProcess('lsp', undefined, n4jscOptions, vmOptions, spawnOptions, logFn);
	n4jscProcess.on('message', (data)=>outputChannel.appendLine(data.toString()));
	n4jscProcess.on('error', (err)=>outputChannel.appendLine(err.toString()));
	n4jscProcess.on('exit', (code)=>outputChannel.appendLine('exit ' + code));
	n4jscProcess.on('close', (code)=>outputChannel.appendLine('close ' + code));
	n4jscProcess.on('disconnect', ()=>outputChannel.appendLine('disconnected'));
	setOutputAppenders(vscode, outputChannel);
	let serverReady = new Promise((resolve, reject)=>{
		let waitForListenMsg = (data)=>{
			let $opt;
			let receivedServerOutput = data.toString();
			if (($opt = receivedServerOutput) == null ? void 0 : $opt.startsWith(LSP_SYNC_MESSAGE)) {
				n4jscProcess.stdout.removeListener('data', waitForListenMsg);
				resolve();
			}
		};
		n4jscProcess.stdout.on('data', waitForListenMsg);
	});
	await serverReady;
	outputChannel.appendLine('Connected to LSP server');
}
async function connectToRunningN4jsLspServer(port, outputChannel) {
	let connectionPromise = new Promise((resolve, reject)=>{
		try {
			let timer = setTimeout(()=>{
				clientSocket.end();
				resolve(null);
			}, TIMEOUT);
			let clientSocket = net.createConnection({
				port: port
			});
			clientSocket.on('connect', ()=>{
				outputChannel.appendLine('Connected to a already running LSP server (port=' + PORT + ')');
				clearTimeout(timer);
				resolve(clientSocket);
			});
			clientSocket.on('error', (err)=>{
				clearTimeout(timer);
				clientSocket.destroy();
				resolve(null);
			});
			clientSocket.on('disconnect', ()=>{
				resolve(null);
			});
		} catch(err) {}
		return null;
	});
	let result = await connectionPromise;
	return result;
}
async function sleep(ms) {
	return new Promise((resolve)=>{
		setTimeout(resolve, ms);
	});
}
//# sourceMappingURL=ExtensionProvider.map