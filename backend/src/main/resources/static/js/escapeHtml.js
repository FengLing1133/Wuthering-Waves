/**
 * HTML 转义工具 — 防止 XSS 注入
 * 所有拼接用户/服务端数据到 innerHTML 前必须经过此函数转义
 */
function escapeHtml(str) {
    if (str == null) return '';
    return String(str)
        .replace(/&/g, '&amp;')
        .replace(/</g, '&lt;')
        .replace(/>/g, '&gt;')
        .replace(/"/g, '&quot;')
        .replace(/'/g, '&#39;');
}
