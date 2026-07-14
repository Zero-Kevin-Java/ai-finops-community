import sys
from pathlib import Path


TYPE_NAMES_TO_REMOVE = [
    # Public model / pricing (L1/L2)
    'PublicModel',
    'PublicModelSearchParams',
    'PublicModelOperateParams',
    'PublicModelStatusOperateParams',
    'PublicModelList',
    'PublicModelOption',
    'ImportPublicModelParams',
    'ImportPublicModelResult',
    'ModelPriceTier',
    'ModelPrice',
    'ModelPriceSearchParams',
    'ModelPriceOperateParams',
    'ModelPriceList',
    'PublicModelPriceTier',
    'PublicModelPrice',
    'PublicModelPriceSearchParams',
    'PublicModelPriceOperateParams',
    'PublicModelPriceList',
    # Billing (L2)
    'BillingRecord',
    'BillingRecordSearchParams',
    'BillingRecordList',
    'BillingSummary',
    'BillingSummarySearchParams',
    'BillingSummaryList',
    # Quota (L2)
    'QuotaAccountType',
    'QuotaObjectType',
    'QuotaUnit',
    'QuotaResetCycle',
    'QuotaLimitAction',
    'QuotaStatus',
    'QuotaAccount',
    'QuotaAccountSearchParams',
    'QuotaAccountOperateParams',
    'QuotaAccountStatusOperateParams',
    'QuotaAccountList',
    'QuotaOverviewSummary',
    'QuotaObjectBalanceRatio',
    'QuotaOverview',
    'QuotaObjectOption',
    'QuotaObjectOptionSearchParams',
    'QuotaAllocationParams',
    'QuotaAdjustParams',
    'QuotaBizType',
    'QuotaChangeType',
    'QuotaLedger',
    'QuotaLedgerSearchParams',
    'QuotaLedgerList',
    # Cost detail (L2)
    'CostDetailOverview',
    'CostDetailOverviewSearchParams',
    'CostDetailOverviewDetail',
    # Usage / request logs (L2)
    'UsageRecord',
    'UsageRecordSearchParams',
    'UsageRecordList',
    'UsageRecordSummary',
    'RequestStatus',
    'RequestLog',
    'RequestLogSearchParams',
    'RequestLogList',
    # Audit (L2)
    'AuditEvent',
    'AuditEventSearchParams',
    'AuditEventList',
    # Bill compare (L2)
    'BillCompare',
    'BillCompareSearchParams',
    'BillCompareList',
    'BillCompareSummary',
    'BillComparePreview',
]


def remove_type_definition(text: str, type_name: str) -> str:
    needle = f'type {type_name}'
    start = text.find(needle)
    if start == -1:
        print(f"  WARNING: type not found: {type_name}", file=sys.stderr)
        return text

    # Include leading whitespace / newline to keep file tidy
    line_start = text.rfind('\n', 0, start)
    if line_start == -1:
        line_start = 0

    pos = start
    depth = 0
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
            elif char == ';' and depth == 0:
                break
        pos += 1

    end = pos + 1  # include semicolon
    return text[:line_start] + text[end:]


def main():
    base = Path(sys.argv[1]) if len(sys.argv) > 1 else Path('D:/project/WL/ai-finops-community/app-l0')
    file_path = base / 'src' / 'typings' / 'api' / 'llm.api.d.ts'
    text = file_path.read_text(encoding='utf-8')
    original_len = len(text)
    for type_name in TYPE_NAMES_TO_REMOVE:
        text = remove_type_definition(text, type_name)
    if len(text) != original_len:
        file_path.write_text(text, encoding='utf-8')
        print(f"Updated {file_path}")
    else:
        print(f"No changes for {file_path}")


if __name__ == '__main__':
    main()
