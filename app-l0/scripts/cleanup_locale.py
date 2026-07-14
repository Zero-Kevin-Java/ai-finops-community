import json5
import re
import sys
from pathlib import Path

def find_key_block_range(text: str, key: str) -> tuple[int, int] | None:
    """Find the range of a key: { ... } block in a JS/TS object literal, including one comma."""
    search = key + ':'
    start = text.find(search)
    if start == -1:
        return None

    # Find opening brace after the key
    brace_start = text.find('{', start)
    if brace_start == -1:
        return None

    depth = 1
    pos = brace_start + 1
    in_string = False
    string_char = None
    while pos < len(text):
        char = text[pos]
        if in_string:
            if char == '\\':
                pos += 2
                continue
            if char == string_char:
                in_string = False
        else:
            if char in ('"', "'", '`'):
                in_string = True
                string_char = char
            elif char == '{':
                depth += 1
            elif char == '}':
                depth -= 1
                if depth == 0:
                    break
        pos += 1

    end = pos + 1  # after closing brace

    # Try to consume trailing comma
    next_pos = end
    while next_pos < len(text) and text[next_pos].isspace():
        next_pos += 1
    if next_pos < len(text) and text[next_pos] == ',':
        end = next_pos + 1
    else:
        # consume leading comma instead
        prev = start - 1
        while prev >= 0 and text[prev].isspace():
            prev -= 1
        if prev >= 0 and text[prev] == ',':
            start = prev

    return (start, end)


def remove_key_path(text: str, key_path: str) -> str:
    """Remove a dotted key path from a JS object literal by finding the last segment block."""
    segments = key_path.split('.')
    # The block we need to remove is the last segment inside its parent object.
    range_info = find_key_block_range(text, segments[-1])
    if range_info is None:
        print(f"  WARNING: key not found: {key_path}", file=sys.stderr)
        return text
    start, end = range_info
    return text[:start] + text[end:]


def remove_keys_from_locale(file_path: Path, key_paths: list[str]) -> None:
    text = file_path.read_text(encoding='utf-8')
    original_len = len(text)
    for key_path in key_paths:
        text = remove_key_path(text, key_path)
    if len(text) != original_len:
        file_path.write_text(text, encoding='utf-8')
        print(f"Updated {file_path}")
    else:
        print(f"No changes for {file_path}")


LOCALE_KEY_PATHS = [
    # dict
    "dict.llm_billing_status",
    "dict.llm_summary_status",
    "dict.llm_audit_level",
    "dict.llm_request_status",
    "dict.llm_quota_status",
    "dict.llm_quota_account_type",
    "dict.llm_quota_reset_cycle",
    "dict.llm_quota_biz_type",
    "dict.llm_quota_change_type",
    # page
    "page.llm.model.importPublic",
    "page.llm.model.publicModel",
    "page.llm.model.publicModelPrice",
    "page.llm.model.modelPrice",
    "page.llm.billingRecord",
    "page.llm.billingSummary",
    "page.llm.yuanKeyBill",
    "page.llm.billCompare",
    "page.llm.auditEvent",
    "page.llm.costDetailOverview",
    "page.llm.roi",
    "page.llm.roiBaseline",
    "page.llm.quotaOverview",
    "page.llm.quotaAccount",
    "page.llm.quotaLedger",
    "page.llm.usageRecord",
    "page.llm.requestLog",
    "page.gateway.trial",
    "page.gateway.scoringAnalytics",
    "page.system.digitalEmployee",
]


def main():
    base = Path(sys.argv[1]) if len(sys.argv) > 1 else Path('D:/project/WL/ai-finops-community/app-l0')
    for lang in ('zh-cn', 'en-us'):
        file_path = base / 'src' / 'locales' / 'langs' / f'{lang}.ts'
        if file_path.exists():
            remove_keys_from_locale(file_path, LOCALE_KEY_PATHS)
        else:
            print(f"File not found: {file_path}", file=sys.stderr)

    app_d_path = base / 'src' / 'typings' / 'app.d.ts'
    if app_d_path.exists():
        text = app_d_path.read_text(encoding='utf-8')
        original_len = len(text)
        for key_path in LOCALE_KEY_PATHS:
            text = remove_key_path(text, key_path)
        if len(text) != original_len:
            app_d_path.write_text(text, encoding='utf-8')
            print(f"Updated {app_d_path}")
        else:
            print(f"No changes for {app_d_path}")


if __name__ == '__main__':
    main()
