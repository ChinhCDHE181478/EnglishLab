import { useEffect, useRef, useState } from 'react';
import { Link2, List, ListOrdered, Redo2, RemoveFormatting, Undo2, Unlink2 } from 'lucide-react';
import { sanitizeLessonHtml } from '../../utils/lessonRichText';
import { useAppDialog } from '../ui/AppDialog';

const TOOL_BUTTON_CLASS = 'inline-flex h-9 min-w-9 items-center justify-center rounded-lg border px-2 text-xs font-extrabold transition focus:outline-none focus:ring-2 focus:ring-[#730014]/20';
const TOOL_BUTTON_IDLE_CLASS = 'border-transparent text-[#4b0009] hover:border-[#dfbfbd] hover:bg-white';
const TOOL_BUTTON_ACTIVE_CLASS = 'border-[#c99599] bg-[#730014] text-white shadow-sm';

const EMPTY_TOOLBAR_STATE = {
  block: 'p',
  bold: false,
  italic: false,
  underline: false,
  strikeThrough: false,
  insertUnorderedList: false,
  insertOrderedList: false,
  justifyLeft: false,
  justifyCenter: false,
  justifyRight: false,
};

const BLOCK_TAGS = new Set(['p', 'h2', 'h3', 'blockquote']);

const selectionBelongsToEditor = (selection, editor) => {
  if (!selection?.rangeCount || !editor) return false;
  const range = selection.getRangeAt(0);
  return editor.contains(range.commonAncestorContainer);
};

const findSelectionBlock = (selection, editor) => {
  if (!selectionBelongsToEditor(selection, editor)) return 'p';
  let node = selection.anchorNode;
  if (node?.nodeType === window.Node.TEXT_NODE) node = node.parentElement;
  while (node && node !== editor) {
    const tagName = node.tagName?.toLowerCase();
    if (BLOCK_TAGS.has(tagName)) return tagName;
    node = node.parentElement;
  }
  return 'p';
};

const selectionHasAncestor = (selection, editor, tags) => {
  if (!selectionBelongsToEditor(selection, editor)) return false;
  let node = selection.anchorNode;
  if (node?.nodeType === window.Node.TEXT_NODE) node = node.parentElement;
  const match = node?.closest?.([...tags].join(','));
  return Boolean(match && editor.contains(match));
};

const commandIsActive = (command) => {
  try {
    return Boolean(document.queryCommandState?.(command));
  } catch {
    return false;
  }
};

const SIZE_CLASS = {
  compact: 'min-h-[120px]',
  form: 'min-h-[180px]',
  lesson: 'min-h-[420px]',
};

/**
 * Rich text editor dùng chung cho Content Manager / Manager / Staff.
 * @param {'compact'|'form'|'lesson'} [size='form']
 * @param {(html: string) => void} onChange
 */
export default function RichTextEditor({
  label,
  onChange,
  value = '',
  placeholder = 'Soạn nội dung tại đây...',
  helperText = 'Nội dung được lưu kèm định dạng (đậm, danh sách, liên kết...).',
  size = 'form',
  className = '',
}) {
  const { alert: alertDialog, prompt: promptDialog } = useAppDialog();
  const editorRef = useRef(null);
  const lastEmittedHtmlRef = useRef('');
  const savedRangeRef = useRef(null);
  const syncToolbarStateRef = useRef(null);
  const [toolbarState, setToolbarState] = useState(EMPTY_TOOLBAR_STATE);

  const syncToolbarState = () => {
    const editor = editorRef.current;
    const selection = window.getSelection?.();
    if (!selectionBelongsToEditor(selection, editor)) return;

    savedRangeRef.current = selection.getRangeAt(0).cloneRange();
    const nextState = {
      block: findSelectionBlock(selection, editor),
      bold: selectionHasAncestor(selection, editor, new Set(['b', 'strong'])) || commandIsActive('bold'),
      italic: selectionHasAncestor(selection, editor, new Set(['em', 'i'])) || commandIsActive('italic'),
      underline: selectionHasAncestor(selection, editor, new Set(['u'])) || commandIsActive('underline'),
      strikeThrough: selectionHasAncestor(selection, editor, new Set(['s', 'strike'])) || commandIsActive('strikeThrough'),
      insertUnorderedList: selectionHasAncestor(selection, editor, new Set(['ul'])) || commandIsActive('insertUnorderedList'),
      insertOrderedList: selectionHasAncestor(selection, editor, new Set(['ol'])) || commandIsActive('insertOrderedList'),
      justifyLeft: commandIsActive('justifyLeft'),
      justifyCenter: commandIsActive('justifyCenter'),
      justifyRight: commandIsActive('justifyRight'),
    };
    setToolbarState((current) => (
      Object.keys(nextState).every((key) => current[key] === nextState[key]) ? current : nextState
    ));
  };
  syncToolbarStateRef.current = syncToolbarState;

  useEffect(() => {
    const editor = editorRef.current;
    if (!editor) return;
    const nextValue = sanitizeLessonHtml(value);
    if (nextValue === lastEmittedHtmlRef.current) return;
    if (editor.innerHTML !== nextValue) {
      editor.innerHTML = nextValue;
      savedRangeRef.current = null;
    }
    lastEmittedHtmlRef.current = nextValue;
  }, [value]);

  useEffect(() => {
    const handleSelectionChange = () => syncToolbarStateRef.current?.();
    document.addEventListener('selectionchange', handleSelectionChange);
    return () => document.removeEventListener('selectionchange', handleSelectionChange);
  }, []);

  const emitChange = () => {
    const editor = editorRef.current;
    if (!editor) return;
    const cleanHtml = sanitizeLessonHtml(editor.innerHTML);
    if (cleanHtml !== editor.innerHTML) editor.innerHTML = cleanHtml;
    lastEmittedHtmlRef.current = cleanHtml;
    onChange(cleanHtml);
  };

  const runCommand = (command, commandValue = null) => {
    const editor = editorRef.current;
    if (!editor) return;
    editor.focus({ preventScroll: true });
    if (savedRangeRef.current && editor.contains(savedRangeRef.current.commonAncestorContainer)) {
      const selection = window.getSelection?.();
      selection?.removeAllRanges();
      selection?.addRange(savedRangeRef.current);
    }
    document.execCommand(command, false, commandValue);
    emitChange();
    syncToolbarState();
  };

  const formatBlock = (tagName) => {
    runCommand('formatBlock', tagName);
  };

  const addLink = async () => {
    const selection = window.getSelection?.();
    if (!selection?.toString().trim()) {
      await alertDialog('Hãy bôi đen đoạn văn bản cần gắn liên kết.', {
        title: 'Chưa chọn văn bản',
      });
      return;
    }
    const savedRange = selection.rangeCount ? selection.getRangeAt(0).cloneRange() : null;
    const href = await promptDialog('Nhập địa chỉ liên kết muốn gắn vào đoạn văn bản đã chọn.', 'https://', {
      title: 'Gắn liên kết',
      inputLabel: 'Địa chỉ liên kết',
      inputType: 'url',
      placeholder: 'https://example.com',
      confirmLabel: 'Gắn liên kết',
      required: true,
    });
    if (!href) return;
    if (savedRange) {
      const currentSelection = window.getSelection?.();
      currentSelection?.removeAllRanges();
      currentSelection?.addRange(savedRange);
    }
    runCommand('createLink', href.trim());
  };

  const uppercaseSelection = () => {
    const selection = window.getSelection?.();
    if (!selection?.rangeCount || !selection.toString()) return;
    const range = selection.getRangeAt(0);
    if (!editorRef.current?.contains(range.commonAncestorContainer)) return;
    const textNode = document.createTextNode(selection.toString().toUpperCase());
    range.deleteContents();
    range.insertNode(textNode);
    range.setStartAfter(textNode);
    range.collapse(true);
    selection.removeAllRanges();
    selection.addRange(range);
    emitChange();
  };

  const heightClass = SIZE_CLASS[size] || SIZE_CLASS.form;

  return (
    <div className={`block space-y-2 ${className}`}>
      {label ? <span className="text-xs font-bold uppercase tracking-[0.14em] text-[#6b7d99]">{label}</span> : null}
      <div className="overflow-hidden rounded-2xl border border-[#d8e0eb] bg-[#f8faff] focus-within:border-[#730014] focus-within:ring-2 focus-within:ring-[#730014]/10">
        <div className="flex flex-wrap items-center gap-1 border-b border-[#d8e0eb] bg-[#fffafb] p-2" role="toolbar" aria-label="Công cụ định dạng nội dung">
          <ToolbarButton active={toolbarState.block === 'p'} label="Đoạn văn" onClick={() => formatBlock('p')}>P</ToolbarButton>
          <ToolbarButton active={toolbarState.block === 'h2'} label="Tiêu đề lớn" onClick={() => formatBlock('h2')}>H2</ToolbarButton>
          <ToolbarButton active={toolbarState.block === 'h3'} label="Tiêu đề nhỏ" onClick={() => formatBlock('h3')}>H3</ToolbarButton>
          <span className="mx-1 h-6 w-px bg-[#dfbfbd]" />
          <ToolbarButton active={toolbarState.bold} label="In đậm" onClick={() => runCommand('bold')}><strong>B</strong></ToolbarButton>
          <ToolbarButton active={toolbarState.italic} label="In nghiêng" onClick={() => runCommand('italic')}><em>I</em></ToolbarButton>
          <ToolbarButton active={toolbarState.underline} label="Gạch chân" onClick={() => runCommand('underline')}><u>U</u></ToolbarButton>
          <ToolbarButton active={toolbarState.strikeThrough} label="Gạch ngang" onClick={() => runCommand('strikeThrough')}><s>S</s></ToolbarButton>
          <ToolbarButton label="Chuyển thành chữ in hoa" onClick={uppercaseSelection}>AA</ToolbarButton>
          <span className="mx-1 h-6 w-px bg-[#dfbfbd]" />
          <ToolbarButton active={toolbarState.insertUnorderedList} label="Danh sách dấu đầu dòng" onClick={() => runCommand('insertUnorderedList')}><List className="h-4 w-4" /></ToolbarButton>
          <ToolbarButton active={toolbarState.insertOrderedList} label="Danh sách đánh số" onClick={() => runCommand('insertOrderedList')}><ListOrdered className="h-4 w-4" /></ToolbarButton>
          <ToolbarButton active={toolbarState.block === 'blockquote'} label="Trích dẫn" onClick={() => formatBlock('blockquote')}>❝</ToolbarButton>
          <ToolbarButton active={toolbarState.justifyLeft} label="Căn trái" onClick={() => runCommand('justifyLeft')}>≡</ToolbarButton>
          <ToolbarButton active={toolbarState.justifyCenter} label="Căn giữa" onClick={() => runCommand('justifyCenter')}>≣</ToolbarButton>
          <ToolbarButton active={toolbarState.justifyRight} label="Căn phải" onClick={() => runCommand('justifyRight')}>≡</ToolbarButton>
          <span className="mx-1 h-6 w-px bg-[#dfbfbd]" />
          <ToolbarButton label="Gắn liên kết" onClick={addLink}><Link2 className="h-4 w-4" /></ToolbarButton>
          <ToolbarButton label="Bỏ liên kết" onClick={() => runCommand('unlink')}><Unlink2 className="h-4 w-4" /></ToolbarButton>
          <ToolbarButton label="Xóa định dạng" onClick={() => runCommand('removeFormat')}><RemoveFormatting className="h-4 w-4" /></ToolbarButton>
          <ToolbarButton label="Hoàn tác" onClick={() => runCommand('undo')}><Undo2 className="h-4 w-4" /></ToolbarButton>
          <ToolbarButton label="Làm lại" onClick={() => runCommand('redo')}><Redo2 className="h-4 w-4" /></ToolbarButton>
        </div>
        <div
          ref={editorRef}
          className={`${heightClass} overflow-y-auto bg-white px-5 py-4 text-sm leading-7 text-[#2b2828] outline-none empty:before:pointer-events-none empty:before:text-slate-400 empty:before:content-[attr(data-placeholder)] [&_a]:font-semibold [&_a]:text-[#730014] [&_a]:underline [&_blockquote]:my-4 [&_blockquote]:border-l-4 [&_blockquote]:border-[#dfbfbd] [&_blockquote]:bg-[#fffafb] [&_blockquote]:px-4 [&_blockquote]:py-2 [&_h2]:mb-3 [&_h2]:mt-5 [&_h2]:text-2xl [&_h2]:font-extrabold [&_h3]:mb-2 [&_h3]:mt-4 [&_h3]:text-lg [&_h3]:font-extrabold [&_ol]:list-decimal [&_ol]:pl-6 [&_p]:my-2 [&_ul]:list-disc [&_ul]:pl-6`}
          contentEditable
          data-placeholder={placeholder}
          onBlur={emitChange}
          onFocus={syncToolbarState}
          onInput={emitChange}
          onKeyUp={syncToolbarState}
          onMouseUp={syncToolbarState}
          suppressContentEditableWarning
        />
      </div>
      {helperText ? <p className="text-xs leading-5 text-[#8b706e]">{helperText}</p> : null}
    </div>
  );
}

function ToolbarButton({ active = false, children, label, onClick }) {
  return (
    <button
      aria-label={label}
      aria-pressed={active}
      className={`${TOOL_BUTTON_CLASS} ${active ? TOOL_BUTTON_ACTIVE_CLASS : TOOL_BUTTON_IDLE_CLASS}`}
      onClick={onClick}
      onMouseDown={(event) => event.preventDefault()}
      title={label}
      type="button"
    >
      {children}
    </button>
  );
}
