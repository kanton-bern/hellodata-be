'use strict';

var obsidian = require('obsidian');

const modifiers = /^(CommandOrControl|CmdOrCtrl|Command|Cmd|Control|Ctrl|AltGr|Option|Alt|Shift|Super)/i;
const keyCodes = /^(Plus|Space|Tab|Backspace|Delete|Insert|Return|Enter|Up|Down|Left|Right|Home|End|PageUp|PageDown|Escape|Esc|VolumeUp|VolumeDown|VolumeMute|MediaNextTrack|MediaPreviousTrack|MediaStop|MediaPlayPause|PrintScreen|F24|F23|F22|F21|F20|F19|F18|F17|F16|F15|F14|F13|F12|F11|F10|F9|F8|F7|F6|F5|F4|F3|F2|F1|[0-9A-Z)!@#$%^&*(:+<_>?~{|}";=,\-./`[\\\]'])/i;
const UNSUPPORTED = {};

function _command(accelerator, event, modifier) {
	if (process.platform !== 'darwin') {
		return UNSUPPORTED;
	}

	if (event.metaKey) {
		throw new Error('Double `Command` modifier specified.');
	}

	return {
		event: Object.assign({}, event, {metaKey: true}),
		accelerator: accelerator.slice(modifier.length)
	};
}

function _super(accelerator, event, modifier) {
	if (event.metaKey) {
		throw new Error('Double `Super` modifier specified.');
	}

	return {
		event: Object.assign({}, event, {metaKey: true}),
		accelerator: accelerator.slice(modifier.length)
	};
}

function _commandorcontrol(accelerator, event, modifier) {
	if (process.platform === 'darwin') {
		if (event.metaKey) {
			throw new Error('Double `Command` modifier specified.');
		}

		return {
			event: Object.assign({}, event, {metaKey: true}),
			accelerator: accelerator.slice(modifier.length)
		};
	}

	if (event.ctrlKey) {
		throw new Error('Double `Control` modifier specified.');
	}

	return {
		event: Object.assign({}, event, {ctrlKey: true}),
		accelerator: accelerator.slice(modifier.length)
	};
}

function _alt(accelerator, event, modifier) {
	if (modifier === 'option' && process.platform !== 'darwin') {
		return UNSUPPORTED;
	}

	if (event.altKey) {
		throw new Error('Double `Alt` modifier specified.');
	}

	return {
		event: Object.assign({}, event, {altKey: true}),
		accelerator: accelerator.slice(modifier.length)
	};
}

function _shift(accelerator, event, modifier) {
	if (event.shiftKey) {
		throw new Error('Double `Shift` modifier specified.');
	}

	return {
		event: Object.assign({}, event, {shiftKey: true}),
		accelerator: accelerator.slice(modifier.length)
	};
}

function _control(accelerator, event, modifier) {
	if (event.ctrlKey) {
		throw new Error('Double `Control` modifier specified.');
	}

	return {
		event: Object.assign({}, event, {ctrlKey: true}),
		accelerator: accelerator.slice(modifier.length)
	};
}

function reduceModifier({accelerator, event}, modifier) {
	switch (modifier) {
		case 'command':
		case 'cmd': {
			return _command(accelerator, event, modifier);
		}

		case 'super': {
			return _super(accelerator, event, modifier);
		}

		case 'control':
		case 'ctrl': {
			return _control(accelerator, event, modifier);
		}

		case 'commandorcontrol':
		case 'cmdorctrl': {
			return _commandorcontrol(accelerator, event, modifier);
		}

		case 'option':
		case 'altgr':
		case 'alt': {
			return _alt(accelerator, event, modifier);
		}

		case 'shift': {
			return _shift(accelerator, event, modifier);
		}

		default:
			console.error(modifier);
	}
}

function reducePlus({accelerator, event}) {
	return {
		event,
		accelerator: accelerator.trim().slice(1)
	};
}

const virtualKeyCodes = {
	0: 'Digit0',
	1: 'Digit1',
	2: 'Digit2',
	3: 'Digit3',
	4: 'Digit4',
	5: 'Digit5',
	6: 'Digit6',
	7: 'Digit7',
	8: 'Digit8',
	9: 'Digit9',
	'-': 'Minus',
	'=': 'Equal',
	Q: 'KeyQ',
	W: 'KeyW',
	E: 'KeyE',
	R: 'KeyR',
	T: 'KeyT',
	Y: 'KeyY',
	U: 'KeyU',
	I: 'KeyI',
	O: 'KeyO',
	P: 'KeyP',
	'[': 'BracketLeft',
	']': 'BracketRight',
	A: 'KeyA',
	S: 'KeyS',
	D: 'KeyD',
	F: 'KeyF',
	G: 'KeyG',
	H: 'KeyH',
	J: 'KeyJ',
	K: 'KeyK',
	L: 'KeyL',
	';': 'Semicolon',
	'\'': 'Quote',
	'`': 'Backquote',
	'/': 'Backslash',
	Z: 'KeyZ',
	X: 'KeyX',
	C: 'KeyC',
	V: 'KeyV',
	B: 'KeyB',
	N: 'KeyN',
	M: 'KeyM',
	',': 'Comma',
	'.': 'Period',
	'\\': 'Slash',
	' ': 'Space'
};

function reduceKey({accelerator, event}, key) {
	if (key.length > 1 || event.key) {
		throw new Error(`Unvalid keycode \`${key}\`.`);
	}

	const code =
		key.toUpperCase() in virtualKeyCodes ?
			virtualKeyCodes[key.toUpperCase()] :
			null;

	return {
		event: Object.assign({}, event, {key}, code ? {code} : null),
		accelerator: accelerator.trim().slice(key.length)
	};
}

const domKeys = Object.assign(Object.create(null), {
	plus: 'Add',
	space: 'Space',
	tab: 'Tab',
	backspace: 'Backspace',
	delete: 'Delete',
	insert: 'Insert',
	return: 'Return',
	enter: 'Return',
	up: 'ArrowUp',
	down: 'ArrowDown',
	left: 'ArrowLeft',
	right: 'ArrowRight',
	home: 'Home',
	end: 'End',
	pageup: 'PageUp',
	pagedown: 'PageDown',
	escape: 'Escape',
	esc: 'Escape',
	volumeup: 'AudioVolumeUp',
	volumedown: 'AudioVolumeDown',
	volumemute: 'AudioVolumeMute',
	medianexttrack: 'MediaTrackNext',
	mediaprevioustrack: 'MediaTrackPrevious',
	mediastop: 'MediaStop',
	mediaplaypause: 'MediaPlayPause',
	printscreen: 'PrintScreen'
});

// Add function keys
for (let i = 1; i <= 24; i++) {
	domKeys[`f${i}`] = `F${i}`;
}

function reduceCode({accelerator, event}, {code, key}) {
	if (event.code) {
		throw new Error(`Duplicated keycode \`${key}\`.`);
	}

	return {
		event: Object.assign({}, event, {key}, code ? {code} : null),
		accelerator: accelerator.trim().slice((key && key.length) || 0)
	};
}

/**
 * This function transform an Electron Accelerator string into
 * a DOM KeyboardEvent object.
 *
 * @param  {string} accelerator an Electron Accelerator string, e.g. `Ctrl+C` or `Shift+Space`.
 * @return {object} a DOM KeyboardEvent object derivate from the `accelerator` argument.
 */
function toKeyEvent(accelerator) {
	let state = {accelerator, event: {}};
	while (state.accelerator !== '') {
		const modifierMatch = state.accelerator.match(modifiers);
		if (modifierMatch) {
			const modifier = modifierMatch[0].toLowerCase();
			state = reduceModifier(state, modifier);
			if (state === UNSUPPORTED) {
				return {unsupportedKeyForPlatform: true};
			}
		} else if (state.accelerator.trim()[0] === '+') {
			state = reducePlus(state);
		} else {
			const codeMatch = state.accelerator.match(keyCodes);
			if (codeMatch) {
				const code = codeMatch[0].toLowerCase();
				if (code in domKeys) {
					state = reduceCode(state, {
						code: domKeys[code],
						key: code
					});
				} else {
					state = reduceKey(state, code);
				}
			} else {
				throw new Error(`Unvalid accelerator: "${state.accelerator}"`);
			}
		}
	}

	return state.event;
}

var keyboardeventFromElectronAccelerator = {
	UNSUPPORTED,
	reduceModifier,
	reducePlus,
	reduceKey,
	reduceCode,
	toKeyEvent
};

/**
 * Follows the link under the cursor, temporarily moving the cursor if necessary for follow-link to
 * work (i.e. if the cursor is on a starting square bracket).
 */
const followLinkUnderCursor = (vimrcPlugin) => {
    const obsidianEditor = vimrcPlugin.getActiveObsidianEditor();
    const { line, ch } = obsidianEditor.getCursor();
    const firstTwoChars = obsidianEditor.getRange({ line, ch }, { line, ch: ch + 2 });
    let numCharsMoved = 0;
    for (const char of firstTwoChars) {
        if (char === "[") {
            obsidianEditor.exec("goRight");
            numCharsMoved++;
        }
    }
    vimrcPlugin.executeObsidianCommand("editor:follow-link");
    // Move the cursor back to where it was
    for (let i = 0; i < numCharsMoved; i++) {
        obsidianEditor.exec("goLeft");
    }
};

/**
 * Moves the cursor down `repeat` lines, skipping over folded sections.
 */
const moveDownSkippingFolds = (vimrcPlugin, cm, { repeat }) => {
    moveSkippingFolds(vimrcPlugin, repeat, "down");
};
/**
 * Moves the cursor up `repeat` lines, skipping over folded sections.
 */
const moveUpSkippingFolds = (vimrcPlugin, cm, { repeat }) => {
    moveSkippingFolds(vimrcPlugin, repeat, "up");
};
function moveSkippingFolds(vimrcPlugin, repeat, direction) {
    const obsidianEditor = vimrcPlugin.getActiveObsidianEditor();
    let { line: oldLine, ch: oldCh } = obsidianEditor.getCursor();
    const commandName = direction === "up" ? "goUp" : "goDown";
    for (let i = 0; i < repeat; i++) {
        obsidianEditor.exec(commandName);
        const { line: newLine, ch: newCh } = obsidianEditor.getCursor();
        if (newLine === oldLine && newCh === oldCh) {
            // Going in the specified direction doesn't do anything anymore, stop now
            return;
        }
        [oldLine, oldCh] = [newLine, newCh];
    }
}

/**
 * Returns the position of the repeat-th instance of a pattern from a given cursor position, in the
 * given direction; looping to the other end of the document when reaching one end. Returns the
 * original cursor position if no match is found.
 *
 * Under the hood, to avoid repeated loops of the document: we get all matches at once, order them
 * according to `direction` and `cursorPosition`, and use modulo arithmetic to return the
 * appropriate match.
 *
 * @param cm The CodeMirror editor instance.
 * @param cursorPosition The current cursor position.
 * @param repeat The number of times to repeat the jump (e.g. 1 to jump to the very next match). Is
 * modulo-ed for efficiency.
 * @param regex The regex pattern to jump to.
 * @param filterMatch Optional filter function to run on the regex matches. Return false to ignore
 * a given match.
 * @param direction The direction to jump in.
 */
function jumpToPattern({ cm, cursorPosition, repeat, regex, filterMatch = () => true, direction, }) {
    const content = cm.getValue();
    const cursorIdx = cm.indexFromPos(cursorPosition);
    const orderedMatches = getOrderedMatches({ content, regex, cursorIdx, filterMatch, direction });
    const effectiveRepeat = (repeat % orderedMatches.length) || orderedMatches.length;
    const matchIdx = orderedMatches[effectiveRepeat - 1]?.index;
    if (matchIdx === undefined) {
        return cursorPosition;
    }
    const newCursorPosition = cm.posFromIndex(matchIdx);
    return newCursorPosition;
}
/**
 * Returns an ordered array of all matches of a regex in a string in the given direction from the
 * cursor index (looping around to the other end of the document when reaching one end).
 */
function getOrderedMatches({ content, regex, cursorIdx, filterMatch, direction, }) {
    const { previousMatches, currentMatches, nextMatches } = getAndGroupMatches(content, regex, cursorIdx, filterMatch);
    if (direction === "next") {
        return [...nextMatches, ...previousMatches, ...currentMatches];
    }
    return [
        ...previousMatches.reverse(),
        ...nextMatches.reverse(),
        ...currentMatches.reverse(),
    ];
}
/**
 * Finds all matches of a regex in a string and groups them by their positions relative to the
 * cursor.
 */
function getAndGroupMatches(content, regex, cursorIdx, filterMatch) {
    const globalRegex = makeGlobalRegex(regex);
    const allMatches = [...content.matchAll(globalRegex)].filter(filterMatch);
    const previousMatches = allMatches.filter((match) => match.index < cursorIdx && !isWithinMatch(match, cursorIdx));
    const currentMatches = allMatches.filter((match) => isWithinMatch(match, cursorIdx));
    const nextMatches = allMatches.filter((match) => match.index > cursorIdx);
    return { previousMatches, currentMatches, nextMatches };
}
function makeGlobalRegex(regex) {
    const globalFlags = getGlobalFlags(regex);
    return new RegExp(regex.source, globalFlags);
}
function getGlobalFlags(regex) {
    const { flags } = regex;
    return flags.includes("g") ? flags : `${flags}g`;
}
function isWithinMatch(match, index) {
    return match.index <= index && index < match.index + match[0].length;
}

/** Naive Regex for a Markdown heading (H1 through H6). "Naive" because it does not account for
 * whether the match is within a codeblock (e.g. it could be a Python comment, not a heading).
 */
const NAIVE_HEADING_REGEX = /^#{1,6} /gm;
/** Regex for a Markdown fenced codeblock, which begins with some number >=3 of backticks at the
 * start of a line. It either ends on the nearest future line that starts with at least as many
 * backticks (\1 back-reference), or extends to the end of the string if no such future line exists.
 */
const FENCED_CODEBLOCK_REGEX = /(^```+)(.*?^\1|.*)/gms;
/**
 * Jumps to the repeat-th next heading.
 */
const jumpToNextHeading = (cm, cursorPosition, { repeat }) => {
    return jumpToHeading({ cm, cursorPosition, repeat, direction: "next" });
};
/**
 * Jumps to the repeat-th previous heading.
 */
const jumpToPreviousHeading = (cm, cursorPosition, { repeat }) => {
    return jumpToHeading({ cm, cursorPosition, repeat, direction: "previous" });
};
/**
 * Jumps to the repeat-th heading in the given direction.
 *
 * Under the hood, we use the naive heading regex to find all headings, and then filter out those
 * that are within codeblocks. `codeblockMatches` is passed in a closure to avoid repeated
 * computation.
 */
function jumpToHeading({ cm, cursorPosition, repeat, direction, }) {
    const codeblockMatches = findAllCodeblocks(cm);
    const filterMatch = (match) => !isMatchWithinCodeblock(match, codeblockMatches);
    return jumpToPattern({
        cm,
        cursorPosition,
        repeat,
        regex: NAIVE_HEADING_REGEX,
        filterMatch,
        direction,
    });
}
function findAllCodeblocks(cm) {
    const content = cm.getValue();
    return [...content.matchAll(FENCED_CODEBLOCK_REGEX)];
}
function isMatchWithinCodeblock(match, codeblockMatches) {
    return codeblockMatches.some((codeblockMatch) => isWithinMatch(codeblockMatch, match.index));
}

const WIKILINK_REGEX_STRING = "\\[\\[.*?\\]\\]";
const MARKDOWN_LINK_REGEX_STRING = "\\[.*?\\]\\(.*?\\)";
const URL_REGEX_STRING = "\\w+://\\S+";
/**
 * Regex for a link (which can be a wikilink, a markdown link, or a standalone URL).
 */
const LINK_REGEX_STRING = `${WIKILINK_REGEX_STRING}|${MARKDOWN_LINK_REGEX_STRING}|${URL_REGEX_STRING}`;
const LINK_REGEX = new RegExp(LINK_REGEX_STRING, "g");
/**
 * Jumps to the repeat-th next link.
 *
 * Note that `jumpToPattern` uses `String.matchAll`, which internally updates `lastIndex` after each
 * match; and that `LINK_REGEX` matches wikilinks / markdown links first. So, this won't catch
 * non-standalone URLs (e.g. the URL in a markdown link). This should be a good thing in most cases;
 * otherwise it could be tedious (as a user) for each markdown link to contain two jumpable spots.
*/
const jumpToNextLink = (cm, cursorPosition, { repeat }) => {
    return jumpToPattern({
        cm,
        cursorPosition,
        repeat,
        regex: LINK_REGEX,
        direction: "next",
    });
};
/**
 * Jumps to the repeat-th previous link.
 */
const jumpToPreviousLink = (cm, cursorPosition, { repeat }) => {
    return jumpToPattern({
        cm,
        cursorPosition,
        repeat,
        regex: LINK_REGEX,
        direction: "previous",
    });
};

/**
 * Utility types and functions for defining Obsidian-specific Vim commands.
 */
function defineAndMapObsidianVimMotion(vimObject, motionFn, mapping) {
    vimObject.defineMotion(motionFn.name, motionFn);
    vimObject.mapCommand(mapping, "motion", motionFn.name, undefined, {});
}
function defineAndMapObsidianVimAction(vimObject, vimrcPlugin, obsidianActionFn, mapping) {
    vimObject.defineAction(obsidianActionFn.name, (cm, actionArgs) => {
        obsidianActionFn(vimrcPlugin, cm, actionArgs);
    });
    vimObject.mapCommand(mapping, "action", obsidianActionFn.name, undefined, {});
}

const DEFAULT_SETTINGS = {
    vimrcFileName: ".obsidian.vimrc",
    displayChord: false,
    displayVimMode: false,
    fixedNormalModeLayout: false,
    capturedKeyboardMap: {},
    supportJsCommands: false,
    vimStatusPromptMap: {
        normal: '🟢',
        insert: '🟠',
        visual: '🟡',
        replace: '🔴',
    },
};
const vimStatusPromptClass = "vimrc-support-vim-mode";
// NOTE: to future maintainers, please make sure all mapping commands are included in this array.
const mappingCommands = [
    "map",
    "nmap",
    "noremap",
    "iunmap",
    "nunmap",
    "vunmap",
];
function sleep(ms) {
    return new Promise(resolve => setTimeout(resolve, ms));
}
class VimrcPlugin extends obsidian.Plugin {
    constructor() {
        super(...arguments);
        this.codeMirrorVimObject = null;
        this.initialized = false;
        this.lastYankBuffer = [""];
        this.lastSystemClipboard = "";
        this.yankToSystemClipboard = false;
        this.currentKeyChord = [];
        this.vimChordStatusBar = null;
        this.vimStatusBar = null;
        this.currentVimStatus = "normal" /* vimStatus.normal */;
        this.customVimKeybinds = {};
        this.currentSelection = null;
        this.isInsertMode = false;
        this.logVimModeChange = async (cm) => {
            if (!cm)
                return;
            this.isInsertMode = cm.mode === 'insert';
            switch (cm.mode) {
                case "insert":
                    this.currentVimStatus = "insert" /* vimStatus.insert */;
                    break;
                case "normal":
                    this.currentVimStatus = "normal" /* vimStatus.normal */;
                    break;
                case "visual":
                    this.currentVimStatus = "visual" /* vimStatus.visual */;
                    break;
                case "replace":
                    this.currentVimStatus = "replace" /* vimStatus.replace */;
                    break;
            }
            if (this.settings.displayVimMode)
                this.updateVimStatusBar();
        };
        this.onVimKeypress = async (vimKey) => {
            if (vimKey != "<Esc>") { // TODO figure out what to actually look for to exit commands.
                this.currentKeyChord.push(vimKey);
                if (this.customVimKeybinds[this.currentKeyChord.join("")] != undefined) { // Custom key chord exists.
                    this.currentKeyChord = [];
                }
            }
            else {
                this.currentKeyChord = [];
            }
            // Build keychord text
            let tempS = "";
            for (const s of this.currentKeyChord) {
                tempS += " " + s;
            }
            if (tempS != "") {
                tempS += "-";
            }
            this.vimChordStatusBar?.setText(tempS);
        };
        this.onVimCommandDone = async (reason) => {
            this.vimChordStatusBar?.setText("");
            this.currentKeyChord = [];
        };
        this.onKeydown = (ev) => {
            if (this.settings.fixedNormalModeLayout) {
                const keyMap = this.settings.capturedKeyboardMap;
                if (!this.isInsertMode && !ev.shiftKey &&
                    ev.code in keyMap && ev.key != keyMap[ev.code]) {
                    let view = this.getActiveView();
                    if (view) {
                        const cmEditor = this.getCodeMirror(view);
                        if (cmEditor) {
                            this.codeMirrorVimObject.handleKey(cmEditor, keyMap[ev.code], 'mapping');
                        }
                    }
                    ev.preventDefault();
                    return false;
                }
            }
        };
    }
    updateVimStatusBar() {
        this.vimStatusBar.setText(this.settings.vimStatusPromptMap[this.currentVimStatus]);
        this.vimStatusBar.dataset.vimMode = this.currentVimStatus;
    }
    async captureKeyboardLayout() {
        // This is experimental API and it might break at some point:
        // https://developer.mozilla.org/en-US/docs/Web/API/KeyboardLayoutMap
        let keyMap = {};
        let layout = await navigator.keyboard.getLayoutMap();
        let doneIterating = new Promise((resolve, reject) => {
            let counted = 0;
            layout.forEach((value, index) => {
                keyMap[index] = value;
                counted += 1;
                if (counted === layout.size)
                    resolve();
            });
        });
        await doneIterating;
        new obsidian.Notice('Keyboard layout captured');
        return keyMap;
    }
    async initialize() {
        if (this.initialized)
            return;
        this.codeMirrorVimObject = window.CodeMirrorAdapter?.Vim;
        this.registerYankEvents(activeWindow);
        this.app.workspace.on("window-open", (workspaceWindow, w) => {
            this.registerYankEvents(w);
        });
        this.prepareChordDisplay();
        this.prepareVimModeDisplay();
        // Two events cos
        // this don't trigger on loading/reloading obsidian with note opened
        this.app.workspace.on("active-leaf-change", async () => {
            this.updateSelectionEvent();
            this.updateVimEvents();
        });
        // and this don't trigger on opening same file in new pane
        this.app.workspace.on("file-open", async () => {
            this.updateSelectionEvent();
            this.updateVimEvents();
        });
        this.initialized = true;
    }
    registerYankEvents(win) {
        this.registerDomEvent(win.document, 'click', () => {
            this.captureYankBuffer(win);
        });
        this.registerDomEvent(win.document, 'keyup', () => {
            this.captureYankBuffer(win);
        });
        this.registerDomEvent(win.document, 'focusin', () => {
            this.captureYankBuffer(win);
        });
    }
    async updateSelectionEvent() {
        const view = this.getActiveView();
        if (!view)
            return;
        let cm = this.getCodeMirror(view);
        if (!cm)
            return;
        if (this.getCursorActivityHandlers(cm).some((e) => e.name === "updateSelection"))
            return;
        cm.on("cursorActivity", async (cm) => this.updateSelection(cm));
    }
    async updateSelection(cm) {
        this.currentSelection = cm.listSelections();
    }
    getCursorActivityHandlers(cm) {
        return cm._handlers.cursorActivity;
    }
    async updateVimEvents() {
        if (!this.app.isVimEnabled())
            return;
        let view = this.getActiveView();
        if (view) {
            const cmEditor = this.getCodeMirror(view);
            // See https://codemirror.net/doc/manual.html#vimapi_events for events.
            this.isInsertMode = false;
            this.currentVimStatus = "normal" /* vimStatus.normal */;
            if (this.settings.displayVimMode)
                this.updateVimStatusBar();
            if (!cmEditor)
                return;
            cmEditor.off('vim-mode-change', this.logVimModeChange);
            cmEditor.on('vim-mode-change', this.logVimModeChange);
            this.currentKeyChord = [];
            cmEditor.off('vim-keypress', this.onVimKeypress);
            cmEditor.on('vim-keypress', this.onVimKeypress);
            cmEditor.off('vim-command-done', this.onVimCommandDone);
            cmEditor.on('vim-command-done', this.onVimCommandDone);
            CodeMirror.off(cmEditor.getInputField(), 'keydown', this.onKeydown);
            CodeMirror.on(cmEditor.getInputField(), 'keydown', this.onKeydown);
        }
    }
    async onload() {
        await this.loadSettings();
        this.addSettingTab(new SettingsTab(this.app, this));
        console.log('loading Vimrc plugin');
        this.app.workspace.on('active-leaf-change', async () => {
            if (!this.initialized)
                await this.initialize();
            if (this.codeMirrorVimObject.loadedVimrc)
                return;
            let fileName = this.settings.vimrcFileName;
            if (!fileName || fileName.trim().length === 0) {
                fileName = DEFAULT_SETTINGS.vimrcFileName;
                console.log('Configured Vimrc file name is illegal, falling-back to default');
            }
            let vimrcContent = '';
            try {
                vimrcContent = await this.app.vault.adapter.read(fileName);
            }
            catch (e) {
                console.log('Error loading vimrc file', fileName, 'from the vault root', e.message);
            }
            this.readVimInit(vimrcContent);
        });
    }
    async loadSettings() {
        const data = await this.loadData();
        this.settings = Object.assign({}, DEFAULT_SETTINGS, data);
    }
    async saveSettings() {
        await this.saveData(this.settings);
    }
    onunload() {
        console.log('unloading Vimrc plugin (but Vim commands that were already loaded will still work)');
    }
    getActiveView() {
        return this.app.workspace.getActiveViewOfType(obsidian.MarkdownView);
    }
    getActiveObsidianEditor() {
        return this.getActiveView().editor;
    }
    getCodeMirror(view) {
        return view.editMode?.editor?.cm?.cm;
    }
    readVimInit(vimCommands) {
        let view = this.getActiveView();
        if (view) {
            var cmEditor = this.getCodeMirror(view);
            if (cmEditor && !this.codeMirrorVimObject.loadedVimrc) {
                this.defineBasicCommands(this.codeMirrorVimObject);
                this.defineAndMapObsidianVimCommands(this.codeMirrorVimObject);
                this.defineSendKeys(this.codeMirrorVimObject);
                this.defineObCommand(this.codeMirrorVimObject);
                this.defineSurround(this.codeMirrorVimObject);
                this.defineJsCommand(this.codeMirrorVimObject);
                this.defineJsFile(this.codeMirrorVimObject);
                this.defineSource(this.codeMirrorVimObject);
                this.loadVimCommands(vimCommands);
                // Make sure that we load it just once per CodeMirror instance.
                // This is supposed to work because the Vim state is kept at the keymap level, hopefully
                // there will not be bugs caused by operations that are kept at the object level instead
                this.codeMirrorVimObject.loadedVimrc = true;
            }
            if (cmEditor) {
                cmEditor.off('vim-mode-change', this.logVimModeChange);
                cmEditor.on('vim-mode-change', this.logVimModeChange);
                CodeMirror.off(cmEditor.getInputField(), 'keydown', this.onKeydown);
                CodeMirror.on(cmEditor.getInputField(), 'keydown', this.onKeydown);
            }
        }
    }
    loadVimCommands(vimCommands) {
        let view = this.getActiveView();
        if (view) {
            var cmEditor = this.getCodeMirror(view);
            vimCommands.split("\n").forEach(function (line, index, arr) {
                if (line.trim().length > 0 && line.trim()[0] != '"') {
                    let split = line.split(" ");
                    if (mappingCommands.includes(split[0])) {
                        // Have to do this because "vim-command-done" event doesn't actually work properly, or something.
                        this.customVimKeybinds[split[1]] = true;
                    }
                    this.codeMirrorVimObject.handleEx(cmEditor, line);
                }
            }.bind(this) // Faster than an arrow function. https://stackoverflow.com/questions/50375440/binding-vs-arrow-function-for-react-onclick-event
            );
        }
    }
    defineBasicCommands(vimObject) {
        vimObject.defineOption('clipboard', '', 'string', ['clip'], (value, cm) => {
            if (value) {
                if (value.trim() == 'unnamed' || value.trim() == 'unnamedplus') {
                    if (!this.yankToSystemClipboard) {
                        this.yankToSystemClipboard = true;
                        console.log("Vim is now set to yank to system clipboard.");
                    }
                }
                else {
                    throw new Error("Unrecognized clipboard option, supported are 'unnamed' and 'unnamedplus' (and they do the same)");
                }
            }
        });
        vimObject.defineOption('tabstop', 4, 'number', [], (value, cm) => {
            if (value && cm) {
                cm.setOption('tabSize', value);
            }
        });
        vimObject.defineEx('iunmap', '', (cm, params) => {
            if (params.argString.trim()) {
                this.codeMirrorVimObject.unmap(params.argString.trim(), 'insert');
            }
        });
        vimObject.defineEx('nunmap', '', (cm, params) => {
            if (params.argString.trim()) {
                this.codeMirrorVimObject.unmap(params.argString.trim(), 'normal');
            }
        });
        vimObject.defineEx('vunmap', '', (cm, params) => {
            if (params.argString.trim()) {
                this.codeMirrorVimObject.unmap(params.argString.trim(), 'visual');
            }
        });
        vimObject.defineEx('noremap', '', (cm, params) => {
            if (!params?.args?.length) {
                throw new Error('Invalid mapping: noremap');
            }
            if (params.argString.trim()) {
                this.codeMirrorVimObject.noremap.apply(this.codeMirrorVimObject, params.args);
            }
        });
        // Allow the user to register an Ex command
        vimObject.defineEx('exmap', '', (cm, params) => {
            if (params?.args?.length && params.args.length < 2) {
                throw new Error(`exmap requires at least 2 parameters: [name] [actions...]`);
            }
            let commandName = params.args[0];
            params.args.shift();
            let commandContent = params.args.join(' ');
            // The content of the user's Ex command is just the remaining parameters of the exmap command
            this.codeMirrorVimObject.defineEx(commandName, '', (cm, params) => {
                this.codeMirrorVimObject.handleEx(cm, commandContent);
            });
        });
    }
    defineAndMapObsidianVimCommands(vimObject) {
        defineAndMapObsidianVimMotion(vimObject, jumpToNextHeading, ']]');
        defineAndMapObsidianVimMotion(vimObject, jumpToPreviousHeading, '[[');
        defineAndMapObsidianVimMotion(vimObject, jumpToNextLink, 'gl');
        defineAndMapObsidianVimMotion(vimObject, jumpToPreviousLink, 'gL');
        defineAndMapObsidianVimAction(vimObject, this, moveDownSkippingFolds, 'zj');
        defineAndMapObsidianVimAction(vimObject, this, moveUpSkippingFolds, 'zk');
        defineAndMapObsidianVimAction(vimObject, this, followLinkUnderCursor, 'gf');
    }
    defineSendKeys(vimObject) {
        vimObject.defineEx('sendkeys', '', async (cm, params) => {
            if (!params?.args?.length) {
                console.log(params);
                throw new Error(`The sendkeys command requires a list of keys, e.g. sendKeys Ctrl+p a b Enter`);
            }
            let allGood = true;
            let events = [];
            for (const key of params.args) {
                if (key.startsWith('wait')) {
                    const delay = key.slice(4);
                    await sleep(delay * 1000);
                }
                else {
                    let keyEvent = null;
                    try {
                        keyEvent = new KeyboardEvent('keydown', keyboardeventFromElectronAccelerator.toKeyEvent(key));
                        events.push(keyEvent);
                    }
                    catch (e) {
                        allGood = false;
                        throw new Error(`Key '${key}' couldn't be read as an Electron Accelerator`);
                    }
                    if (allGood) {
                        for (keyEvent of events)
                            window.postMessage(JSON.parse(JSON.stringify(keyEvent)), '*');
                        // view.containerEl.dispatchEvent(keyEvent);
                    }
                }
            }
        });
    }
    executeObsidianCommand(commandName) {
        const availableCommands = this.app.commands.commands;
        if (!(commandName in availableCommands)) {
            throw new Error(`Command ${commandName} was not found, try 'obcommand' with no params to see in the developer console what's available`);
        }
        const view = this.getActiveView();
        const editor = view.editor;
        const command = availableCommands[commandName];
        const { callback, checkCallback, editorCallback, editorCheckCallback } = command;
        if (editorCheckCallback)
            editorCheckCallback(false, editor, view);
        else if (editorCallback)
            editorCallback(editor, view);
        else if (checkCallback)
            checkCallback(false);
        else if (callback)
            callback();
        else
            throw new Error(`Command ${commandName} doesn't have an Obsidian callback`);
    }
    defineObCommand(vimObject) {
        vimObject.defineEx('obcommand', '', async (cm, params) => {
            if (!params?.args?.length || params.args.length != 1) {
                const availableCommands = this.app.commands.commands;
                console.log(`Available commands: ${Object.keys(availableCommands).join('\n')}`);
                throw new Error(`obcommand requires exactly 1 parameter`);
            }
            const commandName = params.args[0];
            this.executeObsidianCommand(commandName);
        });
    }
    defineSurround(vimObject) {
        // Function to surround selected text or highlighted word.
        var surroundFunc = (params) => {
            var editor = this.getActiveView().editor;
            if (!params?.length) {
                throw new Error("surround requires exactly 2 parameters: prefix and postfix text.");
            }
            let newArgs = params.join(" ").match(/(\\.|[^\s\\\\]+)+/g);
            if (newArgs.length != 2) {
                throw new Error("surround requires exactly 2 parameters: prefix and postfix text.");
            }
            let beginning = newArgs[0].replace("\\\\", "\\").replace("\\ ", " "); // Get the beginning surround text
            let ending = newArgs[1].replace("\\\\", "\\").replace("\\ ", " "); // Get the ending surround text
            let currentSelections = this.currentSelection;
            var chosenSelection = currentSelections?.[0] ? currentSelections[0] : { anchor: editor.getCursor(), head: editor.getCursor() };
            if (currentSelections?.length > 1) {
                console.log("WARNING: Multiple selections in surround. Attempt to select matching cursor. (obsidian-vimrc-support)");
                const cursorPos = editor.getCursor();
                for (const selection of currentSelections) {
                    if (selection.head.line == cursorPos.line && selection.head.ch == cursorPos.ch) {
                        console.log("RESOLVED: Selection matching cursor found. (obsidian-vimrc-support)");
                        chosenSelection = selection;
                        break;
                    }
                }
            }
            if (editor.posToOffset(chosenSelection.anchor) === editor.posToOffset(chosenSelection.head)) {
                // No range of selected text, so select word.
                let wordAt = editor.wordAt(chosenSelection.head);
                if (wordAt) {
                    chosenSelection = { anchor: wordAt.from, head: wordAt.to };
                }
            }
            if (editor.posToOffset(chosenSelection.anchor) > editor.posToOffset(chosenSelection.head)) {
                [chosenSelection.anchor, chosenSelection.head] = [chosenSelection.head, chosenSelection.anchor];
            }
            let currText = editor.getRange(chosenSelection.anchor, chosenSelection.head);
            editor.replaceRange(beginning + currText + ending, chosenSelection.anchor, chosenSelection.head);
            // If no selection, place cursor between beginning and ending
            if (editor.posToOffset(chosenSelection.anchor) === editor.posToOffset(chosenSelection.head)) {
                chosenSelection.head.ch += beginning.length;
                editor.setCursor(chosenSelection.head);
            }
        };
        vimObject.defineEx("surround", "", (cm, params) => { surroundFunc(params.args); });
        vimObject.defineEx("pasteinto", "", (cm, params) => {
            // Using the register for when this.yankToSystemClipboard == false
            surroundFunc(['[',
                '](' + vimObject.getRegisterController().getRegister('yank').keyBuffer + ")"]);
        });
        this.getActiveView().editor;
        // Handle the surround dialog input
        var surroundDialogCallback = (value) => {
            if ((/^\[+$/).test(value)) { // check for 1-inf [ and match them with ]
                surroundFunc([value, "]".repeat(value.length)]);
            }
            else if ((/^\(+$/).test(value)) { // check for 1-inf ( and match them with )
                surroundFunc([value, ")".repeat(value.length)]);
            }
            else if ((/^\{+$/).test(value)) { // check for 1-inf { and match them with }
                surroundFunc([value, "}".repeat(value.length)]);
            }
            else { // Else, just put it before and after.
                surroundFunc([value, value]);
            }
        };
        vimObject.defineOperator("surroundOperator", () => {
            let p = "<span>Surround with: <input type='text'></span>";
            CodeMirror.openDialog(p, surroundDialogCallback, { bottom: true, selectValueOnOpen: false });
        });
        vimObject.mapCommand("<A-y>s", "operator", "surroundOperator");
    }
    async captureYankBuffer(win) {
        if (!this.yankToSystemClipboard) {
            return;
        }
        const yankRegister = this.codeMirrorVimObject.getRegisterController().getRegister('yank');
        const currentYankBuffer = yankRegister.keyBuffer;
        // yank -> clipboard
        const buf = currentYankBuffer[0];
        if (buf !== this.lastYankBuffer[0]) {
            await win.navigator.clipboard.writeText(buf);
            this.lastYankBuffer = currentYankBuffer;
            this.lastSystemClipboard = await win.navigator.clipboard.readText();
            return;
        }
        // clipboard -> yank
        try {
            const currentClipboardText = await win.navigator.clipboard.readText();
            if (currentClipboardText !== this.lastSystemClipboard) {
                yankRegister.setText(currentClipboardText);
                this.lastYankBuffer = yankRegister.keyBuffer;
                this.lastSystemClipboard = currentClipboardText;
            }
        }
        catch (e) {
            // XXX: Avoid "Uncaught (in promise) DOMException: Document is not focused."
            // XXX: It is not good but easy workaround
        }
    }
    prepareChordDisplay() {
        if (this.settings.displayChord) {
            // Add status bar item
            this.vimChordStatusBar = this.addStatusBarItem();
            // Move vimChordStatusBar to the leftmost position and center it.
            let parent = this.vimChordStatusBar.parentElement;
            this.vimChordStatusBar.parentElement.insertBefore(this.vimChordStatusBar, parent.firstChild);
            this.vimChordStatusBar.style.marginRight = "auto";
            const view = this.getActiveView();
            if (!view)
                return;
            let cmEditor = this.getCodeMirror(view);
            // See https://codemirror.net/doc/manual.html#vimapi_events for events.
            cmEditor.off('vim-keypress', this.onVimKeypress);
            cmEditor.on('vim-keypress', this.onVimKeypress);
            cmEditor.off('vim-command-done', this.onVimCommandDone);
            cmEditor.on('vim-command-done', this.onVimCommandDone);
        }
    }
    prepareVimModeDisplay() {
        if (this.settings.displayVimMode) {
            this.vimStatusBar = this.addStatusBarItem(); // Add status bar item
            this.vimStatusBar.setText(this.settings.vimStatusPromptMap["normal" /* vimStatus.normal */]); // Init the vimStatusBar with normal mode
            this.vimStatusBar.addClass(vimStatusPromptClass);
            this.vimStatusBar.dataset.vimMode = this.currentVimStatus;
        }
    }
    defineJsCommand(vimObject) {
        vimObject.defineEx('jscommand', '', (cm, params) => {
            if (!this.settings.supportJsCommands)
                throw new Error("JS commands are turned off; enable them via the Vimrc plugin configuration if you're sure you know what you're doing");
            const jsCode = params.argString.trim();
            if (jsCode[0] != '{' || jsCode[jsCode.length - 1] != '}')
                throw new Error("Expected an argument which is JS code surrounded by curly brackets: {...}");
            let currentSelections = this.currentSelection;
            var chosenSelection = currentSelections && currentSelections.length > 0 ? currentSelections[0] : null;
            const command = Function('editor', 'view', 'selection', jsCode);
            const view = this.getActiveView();
            command(view.editor, view, chosenSelection);
        });
    }
    defineJsFile(vimObject) {
        vimObject.defineEx('jsfile', '', async (cm, params) => {
            if (!this.settings.supportJsCommands)
                throw new Error("JS commands are turned off; enable them via the Vimrc plugin configuration if you're sure you know what you're doing");
            if (params?.args?.length < 1)
                throw new Error("Expected format: fileName {extraCode}");
            let extraCode = '';
            const fileName = params.args[0];
            if (params.args.length > 1) {
                params.args.shift();
                extraCode = params.args.join(' ').trim();
                if (extraCode[0] != '{' || extraCode[extraCode.length - 1] != '}')
                    throw new Error("Expected an extra code argument which is JS code surrounded by curly brackets: {...}");
            }
            let currentSelections = this.currentSelection;
            var chosenSelection = currentSelections && currentSelections.length > 0 ? currentSelections[0] : null;
            let content = '';
            try {
                content = await this.app.vault.adapter.read(fileName);
            }
            catch (e) {
                throw new Error(`Cannot read file ${params.args[0]} from vault root: ${e.message}`);
            }
            const command = Function('editor', 'view', 'selection', content + extraCode);
            const view = this.getActiveView();
            command(view.editor, view, chosenSelection);
        });
    }
    defineSource(vimObject) {
        vimObject.defineEx('source', '', async (cm, params) => {
            if (params?.args?.length > 1)
                throw new Error("Expected format: source [fileName]");
            const fileName = params.argString.trim();
            try {
                this.app.vault.adapter.read(fileName).then(vimrcContent => {
                    this.loadVimCommands(vimrcContent);
                });
            }
            catch (e) {
                console.log('Error loading vimrc file', fileName, 'from the vault root', e.message);
            }
        });
    }
}
class SettingsTab extends obsidian.PluginSettingTab {
    constructor(app, plugin) {
        super(app, plugin);
        this.plugin = plugin;
    }
    display() {
        let { containerEl } = this;
        containerEl.empty();
        containerEl.createEl('h2', { text: 'Vimrc Settings' });
        new obsidian.Setting(containerEl)
            .setName('Vimrc file name')
            .setDesc('Relative to vault directory (requires restart)')
            .addText((text) => {
            text.setPlaceholder(DEFAULT_SETTINGS.vimrcFileName);
            text.setValue(this.plugin.settings.vimrcFileName || DEFAULT_SETTINGS.vimrcFileName);
            text.onChange(value => {
                this.plugin.settings.vimrcFileName = value;
                this.plugin.saveSettings();
            });
        });
        new obsidian.Setting(containerEl)
            .setName('Vim chord display')
            .setDesc('Displays the current chord until completion. Ex: "<Space> f-" (requires restart)')
            .addToggle((toggle) => {
            toggle.setValue(this.plugin.settings.displayChord || DEFAULT_SETTINGS.displayChord);
            toggle.onChange(value => {
                this.plugin.settings.displayChord = value;
                this.plugin.saveSettings();
            });
        });
        new obsidian.Setting(containerEl)
            .setName('Vim mode display')
            .setDesc('Displays the current vim mode (requires restart)')
            .addToggle((toggle) => {
            toggle.setValue(this.plugin.settings.displayVimMode || DEFAULT_SETTINGS.displayVimMode);
            toggle.onChange(value => {
                this.plugin.settings.displayVimMode = value;
                this.plugin.saveSettings();
            });
        });
        new obsidian.Setting(containerEl)
            .setName('Use a fixed keyboard layout for Normal mode')
            .setDesc('Define a keyboard layout to always use when in Normal mode, regardless of the input language (experimental).')
            .addButton(async (button) => {
            button.setButtonText('Capture current layout');
            button.onClick(async () => {
                this.plugin.settings.capturedKeyboardMap = await this.plugin.captureKeyboardLayout();
                this.plugin.saveSettings();
            });
        })
            .addToggle((toggle) => {
            toggle.setValue(this.plugin.settings.fixedNormalModeLayout || DEFAULT_SETTINGS.fixedNormalModeLayout);
            toggle.onChange(async (value) => {
                this.plugin.settings.fixedNormalModeLayout = value;
                if (value && Object.keys(this.plugin.settings.capturedKeyboardMap).length === 0)
                    this.plugin.settings.capturedKeyboardMap = await this.plugin.captureKeyboardLayout();
                this.plugin.saveSettings();
            });
        });
        new obsidian.Setting(containerEl)
            .setName('Support JS commands (beware!)')
            .setDesc("Support the 'jscommand' and 'jsfile' commands, which allow defining Ex commands using Javascript. WARNING! Review the README to understand why this may be dangerous before enabling.")
            .addToggle(toggle => {
            toggle.setValue(this.plugin.settings.supportJsCommands ?? DEFAULT_SETTINGS.supportJsCommands);
            toggle.onChange(value => {
                this.plugin.settings.supportJsCommands = value;
                this.plugin.saveSettings();
            });
        });
        containerEl.createEl('h2', { text: 'Vim Mode Display Prompt' });
        new obsidian.Setting(containerEl)
            .setName('Normal mode prompt')
            .setDesc('Set the status prompt text for normal mode.')
            .addText((text) => {
            text.setPlaceholder('Default: 🟢');
            text.setValue(this.plugin.settings.vimStatusPromptMap.normal ||
                DEFAULT_SETTINGS.vimStatusPromptMap.normal);
            text.onChange((value) => {
                this.plugin.settings.vimStatusPromptMap.normal = value ||
                    DEFAULT_SETTINGS.vimStatusPromptMap.normal;
                this.plugin.saveSettings();
            });
        });
        new obsidian.Setting(containerEl)
            .setName('Insert mode prompt')
            .setDesc('Set the status prompt text for insert mode.')
            .addText((text) => {
            text.setPlaceholder('Default: 🟠');
            text.setValue(this.plugin.settings.vimStatusPromptMap.insert ||
                DEFAULT_SETTINGS.vimStatusPromptMap.insert);
            text.onChange((value) => {
                this.plugin.settings.vimStatusPromptMap.insert = value ||
                    DEFAULT_SETTINGS.vimStatusPromptMap.insert;
                console.log(this.plugin.settings.vimStatusPromptMap);
                this.plugin.saveSettings();
            });
        });
        new obsidian.Setting(containerEl)
            .setName('Visual mode prompt')
            .setDesc('Set the status prompt text for visual mode.')
            .addText((text) => {
            text.setPlaceholder('Default: 🟡');
            text.setValue(this.plugin.settings.vimStatusPromptMap.visual ||
                DEFAULT_SETTINGS.vimStatusPromptMap.visual);
            text.onChange((value) => {
                this.plugin.settings.vimStatusPromptMap.visual = value ||
                    DEFAULT_SETTINGS.vimStatusPromptMap.visual;
                this.plugin.saveSettings();
            });
        });
        new obsidian.Setting(containerEl)
            .setName('Replace mode prompt')
            .setDesc('Set the status prompt text for replace mode.')
            .addText((text) => {
            text.setPlaceholder('Default: 🔴');
            text.setValue(this.plugin.settings.vimStatusPromptMap.replace ||
                DEFAULT_SETTINGS.vimStatusPromptMap.replace);
            text.onChange((value) => {
                this.plugin.settings.vimStatusPromptMap.replace = value ||
                    DEFAULT_SETTINGS.vimStatusPromptMap.replace;
                this.plugin.saveSettings();
            });
        });
    }
}

module.exports = VimrcPlugin;


/* nosourcemap */