#!/usr/bin/env python3
"""
Up Bank (https://up.com.au) Developer API helper for agents.

Fetches transactions so an agent can analyse spending and suggest cuts.
Standard library only (Python 3.9+), no extra installs needed.

Setup
-----
1. Get a personal access token at https://api.up.com.au/getting_started
2. export UP_API_TOKEN="up:yeah:xxxxxxxx"

Python usage
------------
    from up_api import get_current_month_transactions, get_transactions_for_month

    this_month = get_current_month_transactions()
    march_2026 = get_transactions_for_month(2026, 3)
    summary = summarize_spending(march_2026)

CLI usage
---------
    python up_api.py current               # this month's transactions (JSON)
    python up_api.py month 2026 3          # March 2026 transactions (JSON)
    python up_api.py month 2026 3 --summary   # spending summary instead
"""

from __future__ import annotations

import argparse
import json
import os
import sys
import urllib.error
import urllib.parse
import urllib.request
from collections import defaultdict
from datetime import datetime
from typing import Any, Optional
from zoneinfo import ZoneInfo

API_BASE = "https://api.up.com.au/api/v1"
PAGE_SIZE = 100  # max allowed by Up
# Month boundaries are computed in this timezone (Up is an Australian bank).
DEFAULT_TZ = os.environ.get("UP_TIMEZONE", "Australia/Sydney")


class UpApiError(RuntimeError):
    pass


# --------------------------------------------------------------------------
# Low-level HTTP
# --------------------------------------------------------------------------

def _token(token: Optional[str]) -> str:
    tok = token or os.environ.get("UP_API_TOKEN")
    if not tok:
        raise UpApiError("No Up API token. Set UP_API_TOKEN or pass token=...")
    return tok


def _get(url: str, token: str) -> dict[str, Any]:
    req = urllib.request.Request(
        url,
        headers={"Authorization": f"Bearer {token}", "Accept": "application/json"},
    )
    try:
        with urllib.request.urlopen(req, timeout=30) as resp:
            return json.load(resp)
    except urllib.error.HTTPError as e:
        body = e.read().decode("utf-8", "replace")
        raise UpApiError(f"Up API {e.code} for {url}: {body}") from e


def _get_all_pages(url: str, token: str) -> list[dict[str, Any]]:
    """Follow `links.next` until every page has been collected."""
    items: list[dict[str, Any]] = []
    while url:
        payload = _get(url, token)
        items.extend(payload.get("data", []))
        url = (payload.get("links") or {}).get("next")
    return items


# --------------------------------------------------------------------------
# Transactions
# --------------------------------------------------------------------------

def _month_bounds(year: int, month: int, tz: str) -> tuple[datetime, datetime]:
    if not 1 <= month <= 12:
        raise ValueError(f"month must be 1-12, got {month}")
    zone = ZoneInfo(tz)
    start = datetime(year, month, 1, tzinfo=zone)
    end = datetime(year + (month == 12), month % 12 + 1, 1, tzinfo=zone)
    return start, end


def _simplify(tx: dict[str, Any]) -> dict[str, Any]:
    """Flatten an Up JSON:API transaction into an agent-friendly dict."""
    a = tx["attributes"]
    rel = tx.get("relationships", {})

    def rel_id(name: str) -> Optional[str]:
        data = (rel.get(name) or {}).get("data")
        return data["id"] if isinstance(data, dict) else None

    return {
        "id": tx["id"],
        "created_at": a.get("createdAt"),
        "settled_at": a.get("settledAt"),
        "status": a.get("status"),  # HELD or SETTLED
        "description": a.get("description"),
        "raw_text": a.get("rawText"),
        "message": a.get("message"),
        # Negative = money out, positive = money in.
        "amount": float(a["amount"]["value"]),
        "amount_cents": a["amount"]["valueInBaseUnits"],
        "currency": a["amount"]["currencyCode"],
        "foreign_amount": a.get("foreignAmount"),
        "transaction_type": a.get("transactionType"),
        "card_method": (a.get("cardPurchaseMethod") or {}).get("method"),
        "category": rel_id("category"),
        "parent_category": rel_id("parentCategory"),
        "tags": [t["id"] for t in (rel.get("tags") or {}).get("data", [])],
        "account_id": rel_id("account"),
        # Set when money moved between your own Up accounts (e.g. to a Saver).
        "transfer_account_id": rel_id("transferAccount"),
        "is_internal_transfer": rel_id("transferAccount") is not None,
    }


def get_transactions_for_month(
    year: int,
    month: int,
    *,
    token: Optional[str] = None,
    account_id: Optional[str] = None,
    status: Optional[str] = None,
    tz: str = DEFAULT_TZ,
    raw: bool = False,
) -> list[dict[str, Any]]:
    """
    Return every transaction created in the given calendar month.

    Args:
        year, month: e.g. 2026, 3 for March 2026.
        token: Up personal access token (defaults to $UP_API_TOKEN).
        account_id: limit to one account; default is all accounts.
        status: "HELD" or "SETTLED" to filter; default returns both.
        tz: timezone used for month boundaries.
        raw: return Up's original JSON:API objects instead of flattened dicts.
    """
    tok = _token(token)
    start, end = _month_bounds(year, month, tz)
    params = {
        "page[size]": PAGE_SIZE,
        "filter[since]": start.isoformat(),
        "filter[until]": end.isoformat(),
    }
    if status:
        params["filter[status]"] = status.upper()

    path = f"/accounts/{account_id}/transactions" if account_id else "/transactions"
    url = f"{API_BASE}{path}?{urllib.parse.urlencode(params)}"
    items = _get_all_pages(url, tok)

    # filter[until] is exclusive in practice, but guard the boundary anyway.
    items = [
        t for t in items
        if start <= datetime.fromisoformat(t["attributes"]["createdAt"]) < end
    ]
    return items if raw else [_simplify(t) for t in items]


def get_current_month_transactions(
    *,
    token: Optional[str] = None,
    account_id: Optional[str] = None,
    status: Optional[str] = None,
    tz: str = DEFAULT_TZ,
    raw: bool = False,
) -> list[dict[str, Any]]:
    """Return every transaction from the 1st of the current month to now."""
    now = datetime.now(ZoneInfo(tz))
    return get_transactions_for_month(
        now.year, now.month,
        token=token, account_id=account_id, status=status, tz=tz, raw=raw,
    )


# --------------------------------------------------------------------------
# Extras useful for a cost-cutting agent
# --------------------------------------------------------------------------

def get_accounts(token: Optional[str] = None) -> list[dict[str, Any]]:
    """List accounts with current balances (to see savings progress)."""
    tok = _token(token)
    items = _get_all_pages(f"{API_BASE}/accounts?page[size]={PAGE_SIZE}", tok)
    return [
        {
            "id": a["id"],
            "name": a["attributes"]["displayName"],
            "type": a["attributes"]["accountType"],  # SAVER / TRANSACTIONAL / HOME_LOAN
            "ownership": a["attributes"].get("ownershipType"),
            "balance": float(a["attributes"]["balance"]["value"]),
            "currency": a["attributes"]["balance"]["currencyCode"],
        }
        for a in items
    ]


def summarize_spending(transactions: list[dict[str, Any]], top_n: int = 15) -> dict[str, Any]:
    """
    Summarise flattened transactions: income, spending, net, spend by
    category and top merchants. Internal transfers between your own
    accounts are excluded so they don't look like spending.
    """
    real = [t for t in transactions if not t["is_internal_transfer"]]
    spend = [t for t in real if t["amount"] < 0]
    income = [t for t in real if t["amount"] > 0]

    by_cat: dict[str, float] = defaultdict(float)
    by_parent: dict[str, float] = defaultdict(float)
    by_merchant: dict[str, list[float]] = defaultdict(list)
    for t in spend:
        amt = -t["amount"]
        by_cat[t["category"] or "uncategorised"] += amt
        by_parent[t["parent_category"] or "uncategorised"] += amt
        by_merchant[t["description"] or "unknown"].append(amt)

    def ranked(d: dict[str, float]) -> list[dict[str, Any]]:
        return [{"name": k, "total": round(v, 2)}
                for k, v in sorted(d.items(), key=lambda kv: -kv[1])]

    merchants = sorted(by_merchant.items(), key=lambda kv: -sum(kv[1]))[:top_n]
    total_spend = sum(-t["amount"] for t in spend)
    total_income = sum(t["amount"] for t in income)
    saved = sum(-t["amount"] for t in transactions
                if t["is_internal_transfer"] and t["amount"] < 0)

    return {
        "transaction_count": len(transactions),
        "total_income": round(total_income, 2),
        "total_spending": round(total_spend, 2),
        "net": round(total_income - total_spend, 2),
        "moved_to_own_accounts": round(saved, 2),
        "spending_by_parent_category": ranked(by_parent),
        "spending_by_category": ranked(by_cat),
        "top_merchants": [
            {"name": name, "total": round(sum(amts), 2), "count": len(amts),
             "average": round(sum(amts) / len(amts), 2)}
            for name, amts in merchants
        ],
    }


# --------------------------------------------------------------------------
# CLI
# --------------------------------------------------------------------------

def _main(argv: Optional[list[str]] = None) -> int:
    p = argparse.ArgumentParser(description="Fetch Up Bank transactions as JSON.")
    sub = p.add_subparsers(dest="cmd", required=True)

    cur = sub.add_parser("current", help="current month's transactions")
    mon = sub.add_parser("month", help="transactions for a given month")
    mon.add_argument("year", type=int)
    mon.add_argument("month", type=int)
    for sp in (cur, mon):
        sp.add_argument("--account", help="limit to one account id")
        sp.add_argument("--status", choices=["HELD", "SETTLED"])
        sp.add_argument("--summary", action="store_true",
                        help="print a spending summary instead of raw rows")
    sub.add_parser("accounts", help="list accounts and balances")

    args = p.parse_args(argv)
    try:
        if args.cmd == "accounts":
            out: Any = get_accounts()
        else:
            if args.cmd == "current":
                txs = get_current_month_transactions(account_id=args.account,
                                                     status=args.status)
            else:
                txs = get_transactions_for_month(args.year, args.month,
                                                 account_id=args.account,
                                                 status=args.status)
            out = summarize_spending(txs) if args.summary else txs
    except (UpApiError, ValueError) as e:
        print(f"error: {e}", file=sys.stderr)
        return 1

    json.dump(out, sys.stdout, indent=2)
    print()
    return 0


if __name__ == "__main__":
    sys.exit(_main())
