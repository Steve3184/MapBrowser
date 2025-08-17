const KEY_LAYOUT = [
    [
        { code: 'Backquote', key: '`', shift: '~' }, { code: 'Digit1', key: '1', shift: '!' }, { code: 'Digit2', key: '2', shift: '@' },
        { code: 'Digit3', key: '3', shift: '#' }, { code: 'Digit4', key: '4', shift: '$' }, { code: 'Digit5', key: '5', shift: '%' },
        { code: 'Digit6', key: '6', shift: '^' }, { code: 'Digit7', key: '7', shift: '&' }, { code: 'Digit8', key: '8', shift: '*' },
        { code: 'Digit9', key: '9', shift: '(' }, { code: 'Digit0', key: '0', shift: ')' }, { code: 'Minus', key: '-', shift: '_' },
        { code: 'Equal', key: '=', shift: '+' }, { code: 'Backspace', key: 'Backspace', isSpecial: true, className: 'key--backspace' }
    ],
    [
        { code: 'Tab', key: 'Tab', isSpecial: true, className: 'key--tab' }, { code: 'KeyQ', key: 'q', shift: 'Q' },
        { code: 'KeyW', key: 'w', shift: 'W' }, { code: 'KeyE', key: 'e', shift: 'E' }, { code: 'KeyR', key: 'r', shift: 'R' },
        { code: 'KeyT', key: 't', shift: 'T' }, { code: 'KeyY', key: 'y', shift: 'Y' }, { code: 'KeyU', key: 'u', shift: 'U' },
        { code: 'KeyI', key: 'i', shift: 'I' }, { code: 'KeyO', key: 'o', shift: 'O' }, { code: 'KeyP', key: 'p', shift: 'P' },
        { code: 'BracketLeft', key: '[', shift: '{' }, { code: 'BracketRight', key: ']', shift: '}' }, { code: 'Backslash', key: '\\', shift: '|' }
    ],
    [
        { code: 'CapsLock', key: 'CapsLock', isSpecial: true, className: 'key--caps' }, { code: 'KeyA', key: 'a', shift: 'A' },
        { code: 'KeyS', key: 's', shift: 'S' }, { code: 'KeyD', key: 'd', shift: 'D' }, { code: 'KeyF', key: 'f', shift: 'F' },
        { code: 'KeyG', key: 'g', shift: 'G' }, { code: 'KeyH', key: 'h', shift: 'H' }, { code: 'KeyJ', key: 'j', shift: 'J' },
        { code: 'KeyK', key: 'k', shift: 'K' }, { code: 'KeyL', key: 'l', shift: 'L' }, { code: 'Semicolon', key: ';', shift: ':' },
        { code: 'Quote', key: "'", shift: '"' }, { code: 'Enter', key: 'Enter', isSpecial: true, className: 'key--enter' }
    ],
    [
        { code: 'ShiftLeft', key: 'Shift', isSpecial: true, className: 'key--shift-left' }, { code: 'KeyZ', key: 'z', shift: 'Z' },
        { code: 'KeyX', key: 'x', shift: 'X' }, { code: 'KeyC', key: 'c', shift: 'C' }, { code: 'KeyV', key: 'v', shift: 'V' },
        { code: 'KeyB', key: 'b', shift: 'B' }, { code: 'KeyN', key: 'n', shift: 'N' }, { code: 'KeyM', key: 'm', shift: 'M' },
        { code: 'Comma', key: ',', shift: '<' }, { code: 'Period', key: '.', shift: '>' }, { code: 'Slash', key: '/', shift: '?' },
        { code: 'ShiftRight', key: 'Shift', isSpecial: true, className: 'key--shift-right' }
    ],
    [
        { code: 'ControlLeft', key: 'Ctrl', isSpecial: true, className: 'key--wide' },
        { code: 'AltLeft', key: 'Alt', isSpecial: true, className: 'key--wide' },
        { code: 'Space', key: ' ', className: 'key--space' },
        { code: 'ArrowLeft', key: '←', isSpecial: true },
        { code: 'ArrowUp', key: '↑', isSpecial: true },
        { code: 'ArrowDown', key: '↓', isSpecial: true },
        { code: 'ArrowRight', key: '→', isSpecial: true }
    ]
];

const VKEYBOARD_CSS = `
.virtual-keyboard {
font-size: 14px;
position: fixed;
bottom: 5%;
left: 50%;
transform: translateX(-50%);
width: auto;
max-width: 98%;
background-color: #3a3a3a;
border-radius: 8px;
box-shadow: 0 5px 20px rgba(0, 0, 0, 0.5);
user-select: none;
z-index: 9999;
transition: opacity 0.2s, transform 0.2s;
}
.virtual-keyboard.hidden {
opacity: 0;
pointer-events: none;
transform: translateX(-50%) translateY(20px);
}
.keyboard__header {
padding: 5px 8px;
background-color: #4f4f4f;
cursor: move;
color: white;
text-align: center;
font-size: 0.8em;
border-top-left-radius: 8px;
border-top-right-radius: 8px;
position: relative;
}
.keyboard__close-btn {
position: absolute;
top: 4px;
right: 6px;
transform: none;
background: #666;
border: none;
color: white;
width: 20px;
height: 20px;
border-radius: 50%;
cursor: pointer;
font-size: 1.2em;
line-height: 20px;
text-align: center;
padding: 0;
}
.keyboard__close-btn:hover {
background: #888;
}
.keyboard__keys {
padding: 5px;
display: flex;
flex-direction: column;
gap: 4px;
}
.keyboard__row {
display: flex;
justify-content: center;
gap: 4px;
}
.keyboard__key {
display: flex;
justify-content: center;
align-items: center;
height: 2.8em;
padding: 0 0.8em;
background-color: #555;
color: white;
border: none;
border-radius: 5px;
cursor: pointer;
transition: background-color 0.15s;
flex-grow: 1;
min-width: 2.8em;
}
.keyboard__key:hover {
background-color: #777;
}
.keyboard__key:active,
.keyboard__key--active {
background-color: #007bff;
color: white;
}
.key--backspace { flex-grow: 2; }
.key--tab { flex-grow: 1.5; }
.key--caps { flex-grow: 1.8; }
.key--enter { flex-grow: 2.2; }
.key--shift-left { flex-grow: 2.5; }
.key--shift-right { flex-grow: 2.5; }
.key--space { flex-grow: 10; }
.key--wide { flex-grow: 1.5; }
`;
class VirtualKeyboard {
    constructor(options = {}) {
        this.options = {
            targetContainer: document.body,
            inputSelector: '.auto-keyboard-input',
            autoEnable: true,
            layout: KEY_LAYOUT,
            ensureOnTop: true,
            ...options
        };

        this.elements = { container: null, keysContainer: null, keys: new Map() };
        this.state = {
            capsLock: false,
            shift: false,
            ctrl: false,
            alt: false,
            isVisible: false,
            focusedElement: null,
            isDragging: false,
            offsetX: 0,
            offsetY: 0,
            onTopInterval: null
        };

        this._handleFocusIn = this._handleFocusIn.bind(this);
        this.init();
    }

    _injectCSS() {
        if (document.getElementById('vkeyboard-styles')) return;
        const styleElement = document.createElement('style');
        styleElement.id = 'vkeyboard-styles';
        styleElement.innerHTML = VKEYBOARD_CSS;
        document.head.appendChild(styleElement);
    }

    init() {
        this._injectCSS();
        this.elements.container = document.createElement('div');
        this.elements.container.classList.add('virtual-keyboard', 'hidden');
        const header = this._createHeader();
        this.elements.keysContainer = this._createKeys();
        this.elements.container.append(header, this.elements.keysContainer);
        this.options.targetContainer.appendChild(this.elements.container);
        this._makeDraggable(header);
        if (this.options.autoEnable) document.addEventListener('focusin', this._handleFocusIn);
    }

    open(targetInput = null) {
        if (targetInput) this.state.focusedElement = targetInput;
        if (!this.state.isVisible) {
            this.state.isVisible = true;
            this.elements.container.classList.remove('hidden');
            if (this.options.ensureOnTop && !this.state.onTopInterval) {
                this.state.onTopInterval = setInterval(() => this._bringToFront(), 1000);
            }
        }
    }

    close() {
        if (this.state.isVisible) {
            this.state.isVisible = false;
            this.elements.container.classList.add('hidden');
            this.state.focusedElement = null;
            if (this.state.onTopInterval) {
                clearInterval(this.state.onTopInterval);
                this.state.onTopInterval = null;
            }
        }
    }

    _bringToFront() {
        let maxZ = 0;
        document.querySelectorAll('body *').forEach(el => {
            if (el === this.elements.container || this.elements.container.contains(el)) return;
            const zIndex = parseInt(window.getComputedStyle(el).zIndex, 10);
            if (!isNaN(zIndex) && zIndex > maxZ) maxZ = zIndex;
        });
        const currentZ = parseInt(this.elements.container.style.zIndex, 10) || 0;
        if (currentZ <= maxZ) this.elements.container.style.zIndex = maxZ + 1;
    }

    _dispatchEvent(type, keyInfo) {
        if (!this.state.focusedElement) return true;

        let key = keyInfo.isSpecial ? keyInfo.code : this._getChar(keyInfo);

        const event = new KeyboardEvent(type, {
            key: key,
            code: keyInfo.code,
            shiftKey: this.state.shift,
            ctrlKey: this.state.ctrl,
            altKey: this.state.alt,
            bubbles: true,
            cancelable: true
        });

        return this.state.focusedElement.dispatchEvent(event);
    }

    _handleKeyPress(keyInfo) {
        if (!this.state.focusedElement) this.open(document.querySelector('textarea, input[type=text]'));
        if (!this.state.focusedElement) return;
        this.state.focusedElement.focus();

        const wasKeyDownPrevented = !this._dispatchEvent('keydown', keyInfo);

        if (!wasKeyDownPrevented) {
            switch (keyInfo.code) {
                case 'Backspace': this._handleBackspace(); break;
                case 'CapsLock': this._toggleCapsLock(); break;
                case 'ShiftLeft': case 'ShiftRight': this._toggleShift(); break;
                case 'ControlLeft': this._toggleCtrl(); break;
                case 'AltLeft': this._toggleAlt(); break;
                case 'Enter':
                    if (this.state.focusedElement.tagName.toLowerCase() === 'textarea') {
                        this._insertText('\n');
                    } else if (this.state.focusedElement.form) {
                        this.state.focusedElement.form.requestSubmit?.();
                    }
                    break;
                case 'Tab': this._insertText('\t'); break;
                case 'ArrowLeft': case 'ArrowRight': case 'ArrowUp': case 'ArrowDown':
                    break;
                default:
                    const char = this._getChar(keyInfo);
                    this._insertText(char);
                    if (this.state.shift) this._toggleShift(false);
                    if (this.state.ctrl) this._toggleCtrl(false);
                    if (this.state.alt) this._toggleAlt(false);
                    break;
            }
        }

        this._dispatchEvent('keyup', keyInfo);
    }

    _getChar(keyInfo) {
        if (keyInfo.isSpecial) return '';
        let char = this.state.shift ? keyInfo.shift : keyInfo.key;
        if (this.state.capsLock && !this.state.shift && char.match(/^[a-zA-Z]$/)) {
            char = char.toUpperCase();
        }
        return char;
    }

    _updateKeyAppearance() {
        for (const [code, keyElement] of this.elements.keys.entries()) {
            const keyInfo = this.options.layout.flat().find(k => k.code === code);
            if (!keyInfo) continue;

            let displayText = keyInfo.isSpecial ? keyInfo.key : this._getChar(keyInfo);
            if (keyInfo.code === 'Backspace') displayText = '⌫';
            if (keyInfo.code === 'CapsLock') displayText = 'Caps';

            keyElement.textContent = displayText;
        }
        const capsKey = this.elements.keys.get('CapsLock');
        if (capsKey) {
            capsKey.classList.toggle('keyboard__key--active', this.state.capsLock);
            capsKey.textContent = this.state.capsLock ? 'CAPS' : 'Caps';
        }

        const shiftLeftKey = this.elements.keys.get('ShiftLeft');
        const shiftRightKey = this.elements.keys.get('ShiftRight');
        if (shiftLeftKey) shiftLeftKey.classList.toggle('keyboard__key--active', this.state.shift);
        if (shiftRightKey) shiftRightKey.classList.toggle('keyboard__key--active', this.state.shift);

        const ctrlKey = this.elements.keys.get('ControlLeft');
        if (ctrlKey) ctrlKey.classList.toggle('keyboard__key--active', this.state.ctrl);

        const altKey = this.elements.keys.get('AltLeft');
        if (altKey) altKey.classList.toggle('keyboard__key--active', this.state.alt);
    }

    _toggleCapsLock() {
        this.state.capsLock = !this.state.capsLock;
        this._updateKeyAppearance();
    }

    _toggleShift(forceState) {
        this.state.shift = typeof forceState === 'boolean' ? forceState : !this.state.shift;
        this._updateKeyAppearance();
    }

    _toggleCtrl(forceState) {
        this.state.ctrl = typeof forceState === 'boolean' ? forceState : !this.state.ctrl;
        this._updateKeyAppearance();
    }

    _toggleAlt(forceState) {
        this.state.alt = typeof forceState === 'boolean' ? forceState : !this.state.alt;
        this._updateKeyAppearance();
    }

    _insertText(text) {
        if (!this.state.focusedElement || text === '') return;
        const el = this.state.focusedElement;
        const { selectionStart, selectionEnd } = el;
        el.setRangeText(text, selectionStart, selectionEnd, 'end');
        el.dispatchEvent(new Event('input', { bubbles: true, cancelable: true }));
    }

    _handleBackspace() {
        if (!this.state.focusedElement) return;
        const el = this.state.focusedElement;
        const { selectionStart, selectionEnd } = el;
        if (selectionStart === selectionEnd && selectionStart > 0) {
            el.setRangeText('', selectionStart - 1, selectionEnd, 'end');
        } else if (selectionStart !== selectionEnd) {
            el.setRangeText('', selectionStart, selectionEnd, 'end');
        }
        el.dispatchEvent(new Event('input', { bubbles: true, cancelable: true }));
    }

    _createHeader() {
        const header = document.createElement('div');
        header.className = 'keyboard__header';
        header.textContent = 'Virtual Keyboard';

        const closeBtn = document.createElement('button');
        closeBtn.className = 'keyboard__close-btn';
        closeBtn.innerHTML = '×';
        closeBtn.addEventListener('click', () => this.close());

        header.appendChild(closeBtn);
        return header;
    }

    _createKeys() {
        const keysContainer = document.createElement('div');
        keysContainer.className = 'keyboard__keys';

        this.options.layout.forEach(row => {
            const rowElement = document.createElement('div');
            rowElement.className = 'keyboard__row';

            row.forEach(keyInfo => {
                const keyElement = document.createElement('button');
                keyElement.setAttribute('type', 'button');
                keyElement.className = 'keyboard__key';

                if (keyInfo.className) {
                    keyElement.classList.add(...keyInfo.className.split(' '));
                }

                let displayText = keyInfo.key;
                if (keyInfo.code === 'Backspace') {
                    displayText = '⌫';
                } else if (keyInfo.code === 'CapsLock') {
                    displayText = 'Caps';
                }

                keyElement.textContent = displayText;
                keyElement.dataset.code = keyInfo.code;
                keyElement.addEventListener('click', () => this._handleKeyPress(keyInfo));

                rowElement.appendChild(keyElement);
                this.elements.keys.set(keyInfo.code, keyElement);
            });

            keysContainer.appendChild(rowElement);
        });

        return keysContainer;
    }

    destroy() {
        if (this.state.onTopInterval) {
            clearInterval(this.state.onTopInterval);
        }
        document.removeEventListener('focusin', this._handleFocusIn);
        this.elements.container.remove();
        this.elements.keys.clear();
    }

    _handleFocusIn(e) {
        if (document.getElementsByClassName('virtual-keyboard').length == 0) {
            this.init();
            this.isVisible = false;
        }
        if (e.target.matches(this.options.inputSelector)) {
            this.open(e.target);
        }
    }

    _makeDraggable(handle) {
        const container = this.elements.container;

        const onDragStart = (e) => {
            if (e.target.closest('.keyboard__close-btn')) return;

            e.preventDefault();
            this.state.isDragging = true;
            container.style.transition = 'none';

            const clientX = e.type === 'touchstart' ? e.touches[0].clientX : e.clientX;
            const clientY = e.type === 'touchstart' ? e.touches[0].clientY : e.clientY;

            this.state.offsetX = clientX - container.offsetLeft;
            this.state.offsetY = clientY - container.offsetTop;

            document.addEventListener('mousemove', onDragMove);
            document.addEventListener('touchmove', onDragMove, { passive: false });
            document.addEventListener('mouseup', onDragEnd);
            document.addEventListener('touchend', onDragEnd);
        };

        const onDragMove = (e) => {
            if (!this.state.isDragging) return;
            if (e.type === 'touchmove') e.preventDefault();

            const clientX = e.type === 'touchmove' ? e.touches[0].clientX : e.clientX;
            const clientY = e.type === 'touchmove' ? e.touches[0].clientY : e.clientY;

            let newX = clientX - this.state.offsetX;
            let newY = clientY - this.state.offsetY;

            const bounds = {
                w: window.innerWidth,
                h: window.innerHeight,
                kw: container.offsetWidth,
                kh: container.offsetHeight
            };

            newX = Math.max(0, Math.min(newX, bounds.w - bounds.kw));
            newY = Math.max(0, Math.min(newY, bounds.h - bounds.kh));

            container.style.left = `${newX}px`;
            container.style.top = `${newY}px`;
            container.style.transform = 'none';
            container.style.bottom = 'auto';
            container.style.right = 'auto';
        };

        const onDragEnd = () => {
            this.state.isDragging = false;
            container.style.transition = 'opacity 0.2s, transform 0.2s';

            document.removeEventListener('mousemove', onDragMove);
            document.removeEventListener('touchmove', onDragMove);
            document.removeEventListener('mouseup', onDragEnd);
            document.removeEventListener('touchend', onDragEnd);
        };

        handle.addEventListener('mousedown', onDragStart);
        handle.addEventListener('touchstart', onDragStart, { passive: false });
    }
}

(function() {
    let keyboardInstance = null;
    function toggleVKB(forceState) {
        const shouldBeEnabled = forceState === undefined ? !keyboardInstance : !!forceState;
        if (shouldBeEnabled) {
            if (!keyboardInstance) {
                const finalOptions = {
                    inputSelector: 'input, textarea, [contenteditable="true"]',
                    autoEnable: true
                };
                keyboardInstance = new VirtualKeyboard(finalOptions);
            }
        } else {
            if (keyboardInstance) {
                keyboardInstance.destroy();
                keyboardInstance = null;
            }
        }
    }
    window.toggleVKB = toggleVKB;
    setTimeout(()=>{toggleVKB(true);},10);
})();

