// @vitest-environment jsdom

import { describe, expect, it } from 'vitest';
import { looksLikeRichTextHtml, sanitizeLessonHtml, stripRichTextToPlain } from './lessonRichText';

describe('sanitizeLessonHtml', () => {
  it('khôi phục rich text cũ bị escape thay vì hiển thị thẻ HTML thô', () => {
    const escaped = '&lt;h2&gt;Tiêu đề&lt;/h2&gt;&lt;p&gt;Nội dung&lt;/p&gt;';

    expect(looksLikeRichTextHtml(escaped)).toBe(true);
    expect(sanitizeLessonHtml(escaped)).toBe('<h2>Tiêu đề</h2><p>Nội dung</p>');
    expect(stripRichTextToPlain(escaped)).toBe('Tiêu đề\nNội dung');
  });

  it('lọc lại phần tử con sau khi loại bỏ thẻ không được phép', () => {
    const sanitized = sanitizeLessonHtml('<abc><img src=x onerror="alert(1)"></abc><p>Nội dung an toàn</p>');

    expect(sanitized).not.toContain('<img');
    expect(sanitized).not.toContain('onerror');
    expect(sanitized).toContain('<p>Nội dung an toàn</p>');
  });

  it('loại bỏ event handler và liên kết có giao thức nguy hiểm', () => {
    const sanitized = sanitizeLessonHtml(
      '<p onclick="alert(1)">Đề bài</p><a href="javascript:alert(2)" onmouseover="alert(3)">Mở</a>'
    );

    expect(sanitized).not.toContain('onclick');
    expect(sanitized).not.toContain('onmouseover');
    expect(sanitized).not.toContain('javascript:');
    expect(sanitized).toContain('<p>Đề bài</p>');
  });

  it('chỉ giữ căn lề an toàn và gia cố liên kết hợp lệ', () => {
    const sanitized = sanitizeLessonHtml(
      '<p style="text-align:center;background:url(javascript:alert(1))">Giữa</p><a href="https://example.com">Nguồn</a>'
    );

    expect(sanitized).toContain('text-align: center');
    expect(sanitized).not.toContain('background');
    expect(sanitized).toContain('href="https://example.com"');
    expect(sanitized).toContain('rel="noopener noreferrer"');
  });
});
